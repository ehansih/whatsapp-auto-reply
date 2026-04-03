package com.ehansih.whatsapprules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Restarts the WhatsAppListenerService on device boot.
 * NotificationListenerService is system-managed, so we just log here —
 * Android rebinds it automatically if notification access is granted.
 * This receiver ensures the app process starts so the service can bind.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("WARules", "Boot completed — WhatsAppListenerService will auto-rebind")
        }
    }
}
