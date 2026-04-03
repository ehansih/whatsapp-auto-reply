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

    // Track recently replied notifications to avoid duplicate replies
    private val recentlyReplied = mutableSetOf<String>()

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

        // Skip summary / stacked notifications (no remoteInput available on these)
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            Log.d(TAG, "Skipping group summary notification"); return
        }

        // Skip group chats — WhatsApp group messages contain ": " in sender or "@" in title
        if (isGroupChat(extras, sender)) {
            Log.d(TAG, "Skipping group chat from: $sender"); return
        }

        // Avoid replying twice to the same notification key
        if (sbn.key in recentlyReplied) {
            Log.d(TAG, "Already replied to ${sbn.key}, skipping"); return
        }

        Log.d(TAG, "WhatsApp message — sender: $sender | message: $message")

        CoroutineScope(Dispatchers.IO).launch {
            val db = RulesDatabase.getDatabase(applicationContext)
            val rules = db.ruleDao().getEnabledRules()
            Log.d(TAG, "Checking ${rules.size} rules")

            for (rule in rules) {
                val contactMatch = rule.contactName.trim() == "*" ||
                        rule.contactName.split(",").map { it.trim() }.any { entry ->
                            sender.contains(entry, ignoreCase = true)
                        }
                val keywordMatch = rule.keyword.trim() == "*" ||
                        message.contains(rule.keyword.trim(), ignoreCase = true)

                Log.d(TAG, "Rule [${rule.contactName}/${rule.keyword}] contactMatch=$contactMatch keywordMatch=$keywordMatch")

                if (contactMatch && keywordMatch) {
                    // Wait for WhatsApp to fully post the notification with actions
                    delay(1500)

                    // Build the final reply text (AI or template)
                    val finalReply = if (rule.useAI) {
                        val provider = AiProvider.values()
                            .firstOrNull { it.name == rule.aiProvider } ?: AiProvider.GROQ
                        AiReplyEngine.generateReply(
                            applicationContext, sender, message, rule.replyMessage, provider
                        )
                    } else {
                        AiReplyEngine.applyPlaceholders(rule.replyMessage, sender, message)
                    }

                    val sent = sendReply(notification, sbn, finalReply)
                    if (sent) {
                        recentlyReplied.add(sbn.key)
                        // Clear after 30s so same contact can get another reply later
                        launch {
                            delay(30_000)
                            recentlyReplied.remove(sbn.key)
                        }
                    }
                    break
                }
            }
        }
    }

    /**
     * Detects group chats. WhatsApp group messages have sender format "Name: " in the
     * EXTRA_TEXT, or EXTRA_IS_GROUP_CONVERSATION flag set, or the title contains "@".
     */
    private fun isGroupChat(extras: Bundle, sender: String): Boolean {
        if (extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)) return true
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        // Group messages have format "SenderName: message text"
        if (text.contains(Regex("^[^:]+: .+"))) {
            // Only treat as group if the sender doesn't match the sub-sender
            val subSender = text.substringBefore(":").trim()
            if (subSender != sender) return true
        }
        return false
    }

    /**
     * Tries to send a reply via:
     * 1. Standard notification actions with remoteInputs
     * 2. WearableExtender actions (used by modern WhatsApp for inline reply)
     * Returns true if reply was sent successfully.
     */
    private fun sendReply(
        notification: Notification,
        sbn: StatusBarNotification,
        replyText: String
    ): Boolean {
        // Collect all actions: standard + wearable extender
        val standardActions = notification.actions?.toList() ?: emptyList()
        val wearableActions = Notification.WearableExtender(notification).actions
        val allActions = standardActions + wearableActions

        Log.d(TAG, "Total actions found: ${allActions.size} (standard=${standardActions.size}, wearable=${wearableActions.size})")

        if (allActions.isEmpty()) {
            Log.e(TAG, "No actions found — cannot reply")
            return false
        }

        // Find the reply action — prefer ones with "reply" or "repl" in title
        val replyAction = allActions.firstOrNull { action ->
            val title = action.title?.toString()?.lowercase() ?: ""
            action.remoteInputs != null &&
                    action.remoteInputs!!.isNotEmpty() &&
                    (title.contains("reply") || title.contains("repl") || title.contains("respond"))
        } ?: allActions.firstOrNull { action ->
            // Fallback: any action that has remoteInputs
            action.remoteInputs != null && action.remoteInputs!!.isNotEmpty()
        }

        if (replyAction == null) {
            Log.e(TAG, "No action with remoteInputs found across ${allActions.size} actions")
            allActions.forEachIndexed { i, a ->
                Log.d(TAG, "  Action[$i] title='${a.title}' remoteInputs=${a.remoteInputs?.size ?: 0}")
            }
            return false
        }

        Log.d(TAG, "Using action '${replyAction.title}' with ${replyAction.remoteInputs!!.size} remoteInput(s)")

        val intent = Intent()
        val bundle = Bundle()
        for (remoteInput in replyAction.remoteInputs!!) {
            bundle.putCharSequence(remoteInput.resultKey, replyText)
            Log.d(TAG, "  RemoteInput key: ${remoteInput.resultKey}")
        }
        RemoteInput.addResultsToIntent(replyAction.remoteInputs!!, intent, bundle)

        return try {
            replyAction.actionIntent.send(applicationContext, 0, intent)
            Log.d(TAG, "Reply sent successfully: \"$replyText\"")
            cancelNotification(sbn.key)
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "PendingIntent cancelled — WhatsApp may have updated the notification: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error sending reply: ${e.message}")
            false
        }
    }
}
