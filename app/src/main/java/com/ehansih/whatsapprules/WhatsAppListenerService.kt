package com.ehansih.whatsapprules

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WhatsAppListenerService : NotificationListenerService() {

    private val whatsappPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in whatsappPackages) return

        val extras = sbn.notification.extras
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
        val message = extras.getString(Notification.EXTRA_TEXT) ?: return

        // Skip group notifications (they show "X messages from Y contacts")
        if (message.contains("messages from") && message.contains("contacts")) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = RulesDatabase.getDatabase(applicationContext)
            val rules = db.ruleDao().getEnabledRules()

            for (rule in rules) {
                val contactMatch = rule.contactName == "*" ||
                        sender.contains(rule.contactName, ignoreCase = true)
                val keywordMatch = rule.keyword == "*" ||
                        message.contains(rule.keyword, ignoreCase = true)

                if (contactMatch && keywordMatch) {
                    sendReply(sbn.notification, rule.replyMessage)
                    break
                }
            }
        }
    }

    private fun sendReply(notification: Notification, replyText: String) {
        val actions = notification.actions ?: return

        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            if (remoteInputs.isEmpty()) continue

            val intent = Intent()
            val bundle = Bundle()
            for (remoteInput in remoteInputs) {
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)

            try {
                action.actionIntent.send(applicationContext, 0, intent)
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }
            break
        }
    }
}
