package com.ehansih.whatsapprules

import android.os.Bundle
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

        if (editRuleId != -1) {
            supportActionBar?.title = "Edit Rule"
            binding.etContact.setText(intent.getStringExtra(EXTRA_CONTACT))
            binding.etKeyword.setText(intent.getStringExtra(EXTRA_KEYWORD))
            binding.etReply.setText(intent.getStringExtra(EXTRA_REPLY))
        } else {
            supportActionBar?.title = "Add Rule"
        }

        binding.btnSave.setOnClickListener { saveRule() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun saveRule() {
        val contact = binding.etContact.text.toString().trim().ifEmpty { "*" }
        val keyword = binding.etKeyword.text.toString().trim().ifEmpty { "*" }
        val reply = binding.etReply.text.toString().trim()

        if (reply.isEmpty()) {
            binding.etReply.error = "Reply message is required"
            return
        }

        lifecycleScope.launch {
            if (editRuleId != -1) {
                db.ruleDao().update(
                    Rule(id = editRuleId, contactName = contact, keyword = keyword, replyMessage = reply)
                )
            } else {
                db.ruleDao().insert(
                    Rule(contactName = contact, keyword = keyword, replyMessage = reply)
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
    }
}
