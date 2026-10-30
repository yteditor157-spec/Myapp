package com.ishan.sms

data class Payment(val trx: String, val amount: Double, val from: String)

/**
 * Reads "money received" SMS from bKash / Nagad / Rocket.
 * SMS formats change sometimes - test with your own real SMS and adjust the regexes if needed.
 */
object SmsParser {

    private val trxRegex = Regex(
        "(?:TrxID|TxnID|Trx\\s*ID|Txn\\s*ID|TransactionID)\\s*[:#]?\\s*([A-Za-z0-9]{6,20})",
        RegexOption.IGNORE_CASE
    )
    private val amountRegex = Regex(
        "(?:Tk|BDT|৳)\\s*:?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)",
        RegexOption.IGNORE_CASE
    )
    private val fromRegex = Regex("(?:from|Sender:?)\\s*(01[0-9]{9})", RegexOption.IGNORE_CASE)

    /** Which provider sent this SMS? null = not a supported sender (SMS is ignored completely). */
    fun provider(address: String): String? {
        val a = address.lowercase()
        return when {
            a.contains("bkash") -> "bkash"
            a.contains("nagad") -> "nagad"
            a.contains("rocket") || a.contains("16216") || a.contains("dbbl") -> "rocket"
            else -> null
        }
    }

    /** Returns the payment if this is an incoming-money SMS, otherwise null. */
    fun parse(body: String): Payment? {
        if (!body.contains("received", ignoreCase = true)) return null
        val trx = trxRegex.find(body)?.groupValues?.get(1)?.uppercase() ?: return null
        val amount = amountRegex.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null
        if (amount <= 0) return null
        val from = fromRegex.find(body)?.groupValues?.get(1) ?: ""
        return Payment(trx, amount, from)
    }
}
