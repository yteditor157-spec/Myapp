package com.ishan.sms

import android.content.Context
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SyncResult(val ok: Boolean, val message: String)

object Sync {

    /** Called for every incoming SMS (on a background thread). */
    fun handle(ctx: Context, key: String, address: String, body: String) {
        val provider = SmsParser.provider(address) ?: return   // other senders are never stored
        val pay = SmsParser.parse(body)
        val sms = Sms(
            id = "${System.currentTimeMillis()}_${(Math.random() * 100000).toInt()}",
            address = address,
            body = body,
            time = System.currentTimeMillis(),
            method = provider,
            trx = pay?.trx ?: "",
            amount = pay?.amount ?: 0.0,
            from = pay?.from ?: "",
            uploaded = pay == null,   // nothing to upload for non-payment SMS
            used = false
        )
        Store.add(ctx, sms)
        flush(ctx, key)
    }

    /** Upload waiting payments, refresh "used" status, send heartbeat. Safe to call anytime. */
    @Synchronized
    fun flush(ctx: Context, key: String): SyncResult {
        if (!Brand.valid(ctx)) return SyncResult(false, "অ্যাপ বৈধ নয় — অফিসিয়াল সংস্করণ ইনস্টল করুন")
        try {
            if (!Api.isReaderActive(key)) {
                val m = "Key বন্ধ বা ডিলিট করা হয়েছে"
                Prefs.setError(ctx, m)
                return SyncResult(false, m)
            }

            var sent = 0
            for (s in Store.all(ctx)) {
                if (!s.isPayment || s.uploaded) continue
                val json = JSONObject()
                    .put("method", s.method)
                    .put("amount", s.amount)
                    .put("sender", s.from)
                    .put("time", fmt(s.time))
                    .put("receivedAt", s.time)
                    .put("used", false)
                    .put("trx", s.trx)
                Api.putIfAbsent("sms_inbox/${s.trx}", json.toString())
                Store.update(ctx, s.id) { it.uploaded = true }
                sent++
            }

            // Which uploaded SMS have been used by players? (used is final, so checked only once it is true)
            var checked = 0
            for (s in Store.all(ctx)) {
                if (checked >= 40) break
                if (!s.isPayment || !s.uploaded || s.used) continue
                checked++
                if (Api.get("sms_inbox/${s.trx}/used") == "true") Store.update(ctx, s.id) { it.used = true }
            }

            Api.put("readers/$key/lastSeen", "\"${fmt(System.currentTimeMillis())}\"")
            Prefs.setOk(ctx)
            return SyncResult(true, "সিঙ্ক সফল • আপলোড $sent")
        } catch (e: IOException) {
            val m = errorText(e)
            Prefs.setError(ctx, m)
            return SyncResult(false, m)
        }
    }

    fun errorText(e: Exception): String =
        if (e is HttpError) "সার্ভার এরর (${e.code})" else "ইন্টারনেট সংযোগ নেই"

    private fun fmt(ms: Long) =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(Date(ms))
}
