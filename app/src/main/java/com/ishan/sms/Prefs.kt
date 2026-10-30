package com.ishan.sms

import android.content.Context

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("ishan_sms", Context.MODE_PRIVATE)

    fun key(c: Context): String? = p(c).getString("key", null)
    fun setKey(c: Context, k: String?) { p(c).edit().putString("key", k).apply() }

    fun keepAlive(c: Context): Boolean = p(c).getBoolean("keepAlive", true)
    fun setKeepAlive(c: Context, v: Boolean) { p(c).edit().putBoolean("keepAlive", v).apply() }

    fun lastOk(c: Context): Long = p(c).getLong("lastOk", 0L)
    fun lastError(c: Context): String = p(c).getString("lastError", "") ?: ""

    fun setOk(c: Context) {
        p(c).edit().putLong("lastOk", System.currentTimeMillis()).putString("lastError", "").apply()
    }
    fun setError(c: Context, msg: String) { p(c).edit().putString("lastError", msg).apply() }
}
