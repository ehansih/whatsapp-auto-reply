package com.ehansih.whatsapprules

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ehansih.whatsapprules.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: RulesDatabase
    private lateinit var adapter: RulesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = RulesDatabase.getDatabase(this)

        adapter = RulesAdapter(
            onEdit = { rule ->
                val intent = Intent(this, AddRuleActivity::class.java)
                intent.putExtra(AddRuleActivity.EXTRA_RULE_ID, rule.id)
                intent.putExtra(AddRuleActivity.EXTRA_CONTACT, rule.contactName)
                intent.putExtra(AddRuleActivity.EXTRA_KEYWORD, rule.keyword)
                intent.putExtra(AddRuleActivity.EXTRA_REPLY, rule.replyMessage)
                startActivity(intent)
            },
            onDelete = { rule ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Rule")
                    .setMessage("Delete this rule?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch { db.ruleDao().delete(rule) }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onToggle = { rule, enabled ->
                lifecycleScope.launch {
                    db.ruleDao().update(rule.copy(isEnabled = enabled))
                }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        db.ruleDao().getAllRules().observe(this) { rules ->
            adapter.submitList(rules)
            binding.tvEmpty.visibility =
                if (rules.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddRuleActivity::class.java))
        }

        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val isEnabled = enabledListeners?.contains(packageName) == true

        if (!isEnabled) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("WhatsApp Rules needs notification access to auto-reply to messages. Please enable it in the next screen.")
                .setPositiveButton("Grant Access") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setCancelable(false)
                .show()
        }
    }
}
