package com.ishan.sms

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpError(val code: Int) : IOException("HTTP $code")

/** Plain REST calls to Firebase Realtime Database. No Firebase SDK needed. */
object Api {
    const val DB_URL = "https://rh-esports-1f458-default-rtdb.firebaseio.com"

    enum class PutResult { CREATED, EXISTS }

    private fun open(path: String, method: String): HttpURLConnection {
        val c = URL("$DB_URL/$path.json").openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 8000
        c.readTimeout = 8000
        return c
    }

    /** GET a node. Returns raw JSON text ("null" if the node does not exist). */
    @Throws(IOException::class)
    fun get(path: String): String {
        val c = open(path, "GET")
        try {
            if (c.responseCode != 200) throw HttpError(c.responseCode)
            return c.inputStream.bufferedReader().readText().trim()
        } finally { c.disconnect() }
    }

    /** true = reader exists and is active. Throws on network / server error. */
    @Throws(IOException::class)
    fun isReaderActive(key: String): Boolean = get("readers/$key/active") == "true"

    /** Write only if the node does not exist yet (atomic, via Firebase ETag "null_etag"). */
    @Throws(IOException::class)
    fun putIfAbsent(path: String, json: String): PutResult {
        val c = open(path, "PUT")
        try {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("if-match", "null_etag")
            c.outputStream.use { it.write(json.toByteArray()) }
            return when (c.responseCode) {
                200 -> PutResult.CREATED
                412 -> PutResult.EXISTS
                else -> throw HttpError(c.responseCode)
            }
        } finally { c.disconnect() }
    }

    @Throws(IOException::class)
    fun put(path: String, json: String) {
        val c = open(path, "PUT")
        try {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(json.toByteArray()) }
            if (c.responseCode != 200) throw HttpError(c.responseCode)
        } finally { c.disconnect() }
    }
}
