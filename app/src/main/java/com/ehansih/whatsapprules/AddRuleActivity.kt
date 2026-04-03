package com.ehansih.whatsapprules

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ehansih.whatsapprules.databinding.ActivityAddRuleBinding
import kotlinx.coroutines.launch

class AddRuleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRuleBinding
    private lateinit var db: RulesDatabase
    private var editRuleId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = RulesDatabase.getDatabase(this)
        editRuleId = intent.getIntExtra(EXTRA_RULE_ID, -1)

        // Populate saved API key if present
        val savedKey = AiReplyEngine.getApiKey(this)
        if (savedKey.isNotBlank()) binding.etApiKey.setText(savedKey)

        if (editRuleId != -1) {
            supportActionBar?.title = "Edit Rule"
            binding.etContact.setText(intent.getStringExtra(EXTRA_CONTACT))
            binding.etKeyword.setText(intent.getStringExtra(EXTRA_KEYWORD))
            binding.etReply.setText(intent.getStringExtra(EXTRA_REPLY))
            val useAI = intent.getBooleanExtra(EXTRA_USE_AI, false)
            binding.switchAI.isChecked = useAI
            toggleAiFields(useAI)
        } else {
            supportActionBar?.title = "Add Rule"
        }

        binding.switchAI.setOnCheckedChangeListener { _, isChecked ->
            toggleAiFields(isChecked)
            // When AI is turned on, update reply hint
            if (isChecked) {
                binding.tvReplyLabel.text = "Fallback Reply (used if AI fails) *"
                binding.etReply.hint = "Hi {name}! I'll get back to you soon 🙏"
            } else {
                binding.tvReplyLabel.text = "Auto-Reply Message *"
                binding.etReply.hint = "Hi {name}! I'm busy right now, I'll reply soon 🙏"
            }
        }

        binding.btnSave.setOnClickListener { saveRule() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun toggleAiFields(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.tvApiKeyLabel.visibility = visibility
        binding.etApiKey.visibility = visibility
        binding.tvApiKeyHint.visibility = visibility
    }

    private fun saveRule() {
        val contact = binding.etContact.text.toString().trim().ifEmpty { "*" }
        val keyword = binding.etKeyword.text.toString().trim().ifEmpty { "*" }
        val reply = binding.etReply.text.toString().trim()
        val useAI = binding.switchAI.isChecked

        if (reply.isEmpty()) {
            binding.etReply.error = "Reply message is required"
            return
        }

        // Save API key if provided
        if (useAI) {
            val apiKey = binding.etApiKey.text.toString().trim()
            if (apiKey.isNotBlank()) {
                AiReplyEngine.saveApiKey(this, apiKey)
            } else if (!AiReplyEngine.hasApiKey(this)) {
                binding.etApiKey.error = "API key required for AI mode"
                return
            }
        }

        lifecycleScope.launch {
            if (editRuleId != -1) {
                db.ruleDao().update(
                    Rule(
                        id = editRuleId,
                        contactName = contact,
                        keyword = keyword,
                        replyMessage = reply,
                        useAI = useAI
                    )
                )
            } else {
                db.ruleDao().insert(
                    Rule(
                        contactName = contact,
                        keyword = keyword,
                        replyMessage = reply,
                        useAI = useAI
                    )
                )
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_RULE_ID = "rule_id"
        const val EXTRA_CONTACT = "contact"
        const val EXTRA_KEYWORD = "keyword"
        const val EXTRA_REPLY = "reply"
        const val EXTRA_USE_AI = "use_ai"
    }
}
