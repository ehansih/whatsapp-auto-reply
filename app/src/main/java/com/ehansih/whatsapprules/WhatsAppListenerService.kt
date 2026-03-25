package com.ehansih.whatsapprules

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WhatsAppListenerService : NotificationListenerService() {

    private val whatsappPackages = setOf("com.whatsapp", "com.whatsapp.w4b")
    private val TAG = "WARules"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in whatsappPackages) return

        val notification = sbn.notification
        val extras = notification.extras

        val sender = extras.getString(Notification.EXTRA_TITLE) ?: run {
            Log.d(TAG, "No sender found"); return
        }
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: run {
            Log.d(TAG, "No message found"); return
        }

        Log.d(TAG, "WhatsApp notification — sender: $sender | message: $message")

        // Skip summary notifications
        if (extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)) {
            Log.d(TAG, "Skipping group conversation summary")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = RulesDatabase.getDatabase(applicationContext)
            val rules = db.ruleDao().getEnabledRules()
            Log.d(TAG, "Checking ${rules.size} rules")

            for (rule in rules) {
                val contactMatch = rule.contactName == "*" ||
                        sender.contains(rule.contactName, ignoreCase = true)
                val keywordMatch = rule.keyword == "*" ||
                        message.contains(rule.keyword, ignoreCase = true)

                Log.d(TAG, "Rule [${rule.contactName}/${rule.keyword}] contactMatch=$contactMatch keywordMatch=$keywordMatch")

                if (contactMatch && keywordMatch) {
                    // Small delay to ensure notification is fully posted
                    delay(500)
                    sendReply(notification, sbn, rule.replyMessage)
                    break
                }
            }
        }
    }

    private fun sendReply(notification: Notification, sbn: StatusBarNotification, replyText: String) {
        val actions = notification.actions

        if (actions == null || actions.isEmpty()) {
            Log.e(TAG, "No actions found on notification — cannot reply")
            return
        }

        Log.d(TAG, "Found ${actions.size} actions")

        for (action in actions) {
            val remoteInputs = action.remoteInputs

            if (remoteInputs == null || remoteInputs.isEmpty()) {
                Log.d(TAG, "Action '${action.title}' has no remoteInputs, skipping")
                continue
            }

            Log.d(TAG, "Sending reply via action '${action.title}'")

            val intent = Intent()
            val bundle = Bundle()
            for (remoteInput in remoteInputs) {
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)

            try {
                action.actionIntent.send(applicationContext, 0, intent)
                Log.d(TAG, "Reply sent successfully: $replyText")
                // Dismiss the notification after replying
                cancelNotification(sbn.key)
            } catch (e: PendingIntent.CanceledException) {
                Log.e(TAG, "PendingIntent cancelled: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending reply: ${e.message}")
            }
            return
        }

        Log.e(TAG, "No action with remoteInputs found — reply not sent")
    }
}
