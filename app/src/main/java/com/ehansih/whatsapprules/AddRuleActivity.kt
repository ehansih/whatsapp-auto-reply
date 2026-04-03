package com.ehansih.whatsapprules

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ehansih.whatsapprules.databinding.ActivityAddRuleBinding
import kotlinx.coroutines.launch

class AddRuleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRuleBinding
    private lateinit var db: RulesDatabase
    private var editRuleId: Int = -1

    private val providers = AiProvider.values()
    private val providerLabels get() = providers.map { it.displayName }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = RulesDatabase.getDatabase(this)
        editRuleId = intent.getIntExtra(EXTRA_RULE_ID, -1)

        // Set up provider spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, providerLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProvider.adapter = adapter

        // Default to Groq (best free option)
        binding.spinnerProvider.setSelection(AiProvider.GROQ.ordinal)

        // Show/hide AI fields on toggle
        binding.switchAI.setOnCheckedChangeListener { _, isChecked ->
            toggleAiFields(isChecked)
            binding.tvReplyLabel.text = if (isChecked)
                "Fallback Reply (used if AI fails) *"
            else
                "Auto-Reply Message *"
        }

        // When provider changes, update API key hint and load saved key
        binding.spinnerProvider.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val provider = providers[pos]
                updateApiKeyHint(provider)
                val savedKey = AiReplyEngine.getApiKey(this@AddRuleActivity, provider)
                binding.etApiKey.setText(if (savedKey.isNotBlank()) savedKey else "")
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Populate fields if editing
        if (editRuleId != -1) {
            supportActionBar?.title = "Edit Rule"
            binding.etContact.setText(intent.getStringExtra(EXTRA_CONTACT))
            binding.etKeyword.setText(intent.getStringExtra(EXTRA_KEYWORD))
            binding.etReply.setText(intent.getStringExtra(EXTRA_REPLY))
            val useAI = intent.getBooleanExtra(EXTRA_USE_AI, false)
            binding.switchAI.isChecked = useAI
            toggleAiFields(useAI)
            val providerName = intent.getStringExtra(EXTRA_PROVIDER) ?: AiProvider.GROQ.name
            val idx = providers.indexOfFirst { it.name == providerName }.coerceAtLeast(0)
            binding.spinnerProvider.setSelection(idx)
        } else {
            supportActionBar?.title = "Add Rule"
            // Pre-fill saved Groq key if exists
            val savedKey = AiReplyEngine.getApiKey(this, AiProvider.GROQ)
            if (savedKey.isNotBlank()) binding.etApiKey.setText(savedKey)
        }

        binding.btnSave.setOnClickListener { saveRule() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun toggleAiFields(show: Boolean) {
        val v = if (show) View.VISIBLE else View.GONE
        binding.tvProviderLabel.visibility = v
        binding.spinnerProvider.visibility = v
        binding.tvApiKeyLabel.visibility = v
        binding.etApiKey.visibility = v
        binding.tvApiKeyHint.visibility = v
        if (show) updateApiKeyHint(providers[binding.spinnerProvider.selectedItemPosition])
    }

    private fun updateApiKeyHint(provider: AiProvider) {
        val hint = when (provider) {
            AiProvider.GROQ     -> "Get FREE key at groq.com → sign up → API Keys"
            AiProvider.GEMINI   -> "Get FREE key at aistudio.google.com → Get API Key"
            AiProvider.DEEPSEEK -> "Get key at platform.deepseek.com (free credits on signup)"
            AiProvider.OPENAI   -> "Get key at platform.openai.com (paid)"
            AiProvider.CLAUDE   -> "Get key at console.anthropic.com (paid)"
        }
        binding.tvApiKeyHint.text = "Key saved on device only. $hint"
    }

    private fun selectedProvider(): AiProvider = providers[binding.spinnerProvider.selectedItemPosition]

    private fun saveRule() {
        val contact  = binding.etContact.text.toString().trim().ifEmpty { "*" }
        val keyword  = binding.etKeyword.text.toString().trim().ifEmpty { "*" }
        val reply    = binding.etReply.text.toString().trim()
        val useAI    = binding.switchAI.isChecked
        val provider = selectedProvider()

        if (reply.isEmpty()) { binding.etReply.error = "Reply message is required"; return }

        if (useAI) {
            val apiKey = binding.etApiKey.text.toString().trim()
            if (apiKey.isNotBlank()) {
                AiReplyEngine.saveApiKey(this, provider, apiKey)
            } else if (!AiReplyEngine.hasApiKey(this, provider)) {
                binding.etApiKey.error = "API key required for AI mode"
                return
            }
        }

        lifecycleScope.launch {
            if (editRuleId != -1) {
                db.ruleDao().update(Rule(
                    id = editRuleId, contactName = contact, keyword = keyword,
                    replyMessage = reply, useAI = useAI, aiProvider = provider.name
                ))
            } else {
                db.ruleDao().insert(Rule(
                    contactName = contact, keyword = keyword,
                    replyMessage = reply, useAI = useAI, aiProvider = provider.name
                ))
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_RULE_ID  = "rule_id"
        const val EXTRA_CONTACT  = "contact"
        const val EXTRA_KEYWORD  = "keyword"
        const val EXTRA_REPLY    = "reply"
        const val EXTRA_USE_AI   = "use_ai"
        const val EXTRA_PROVIDER = "ai_provider"
    }
}
