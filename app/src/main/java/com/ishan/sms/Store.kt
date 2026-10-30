package com.ishan.sms

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Sms(
    val id: String,
    val address: String,
    val body: String,
    val time: Long,
    val method: String,     // bkash | nagad | rocket
    val trx: String,        // "" if this SMS is not an incoming payment
    val amount: Double,
    val from: String,
    var uploaded: Boolean,
    var used: Boolean
) {
    val isPayment: Boolean get() = trx.isNotEmpty()

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("address", address).put("body", body).put("time", time)
        .put("method", method).put("trx", trx).put("amount", amount).put("from", from)
        .put("uploaded", uploaded).put("used", used)

    companion object {
        fun fromJson(o: JSONObject) = Sms(
            o.getString("id"), o.optString("address"), o.optString("body"), o.optLong("time"),
            o.optString("method"), o.optString("trx"), o.optDouble("amount", 0.0), o.optString("from"),
            o.optBoolean("uploaded"), o.optBoolean("used")
        )
    }
}

/** Local history of captured SMS (newest first), kept in a private file. */
object Store {
    private const val FILE = "sms_store.json"
    private const val MAX = 500
    private var list: MutableList<Sms>? = null

    @Synchronized
    private fun load(c: Context): MutableList<Sms> {
        list?.let { return it }
        val out = mutableListOf<Sms>()
        val f = File(c.filesDir, FILE)
        if (f.exists()) {
            try {
                val a = JSONArray(f.readText())
                for (i in 0 until a.length()) out.add(Sms.fromJson(a.getJSONObject(i)))
            } catch (_: Exception) { }
        }
        list = out
        return out
    }

    private fun save(c: Context) {
        val a = JSONArray()
        list?.forEach { a.put(it.toJson()) }
        File(c.filesDir, FILE).writeText(a.toString())
    }

    @Synchronized fun all(c: Context): List<Sms> = load(c).toList()

    /** false if a payment with the same TrxID is already stored. */
    @Synchronized
    fun add(c: Context, s: Sms): Boolean {
        val l = load(c)
        if (s.trx.isNotEmpty() && l.any { it.trx == s.trx }) return false
        l.add(0, s)
        while (l.size > MAX) l.removeAt(l.size - 1)
        save(c)
        return true
    }

    @Synchronized
    fun update(c: Context, id: String, mutate: (Sms) -> Unit) {
        load(c).find { it.id == id }?.let { mutate(it); save(c) }
    }

    @Synchronized
    fun clear(c: Context) { load(c).clear(); save(c) }
}
