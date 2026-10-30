package com.ishan.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Brand.valid(context)) return
        val key = Prefs.key(context) ?: return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (msgs.isNullOrEmpty()) return
        val address = msgs[0].originatingAddress ?: return
        val body = msgs.joinToString("") { it.messageBody ?: "" }

        val pending = goAsync()
        Thread {
            try { Sync.handle(context.applicationContext, key, address, body) }
            catch (_: Exception) { }
            finally { pending.finish() }
        }.start()
    }
}
