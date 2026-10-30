package com.ishan.sms

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * SMS READER • Ishan
 *
 * The credit line is NOT stored as plain text anywhere (not in XML, not in strings).
 * It is AES-256 encrypted, and the key is derived from this app's package name, so
 * changing the package name also breaks decryption. The decrypted text is checked
 * against a SHA-256 hash, and the app refuses to work if the check fails.
 *
 * OPTIONAL (recommended for release): set CERT to the SHA-256 of your release signing
 * certificate (lowercase hex, no colons). A re-signed / repackaged APK will then be rejected.
 */
object Brand {
    private const val SALT = "ISHAN::SMS::READER::v1"
    private const val IV = "V75VV+RUHfgfbSNys23gXg=="
    private const val CT = "w45I6LzyRjMbmkKEB3z1FwGMJapVBzcGx+RHkK9BzpE="
    private const val H1 = "030a86112ae2a3eb"
    private const val H2 = "32a8fe31c0f25f22"
    private const val H3 = "6ef9c5b2fe406143"
    private const val H4 = "bed3fae67df124b1"

    /** Your release certificate SHA-256 (see README). Empty = certificate check disabled. */
    private const val CERT = ""

    /** The decrypted credit line, or null if anything was tampered with. */
    fun text(ctx: Context): String? {
        return try {
            val key = MessageDigest.getInstance("SHA-256").digest((ctx.packageName + SALT).toByteArray())
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
            c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"),
                IvParameterSpec(Base64.decode(IV, Base64.NO_WRAP)))
            val s = String(c.doFinal(Base64.decode(CT, Base64.NO_WRAP)), Charsets.UTF_8)
            if (hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())) == H1 + H2 + H3 + H4) s else null
        } catch (e: Exception) { null }
    }

    fun valid(ctx: Context): Boolean = text(ctx) != null && certOk(ctx)

    private fun certOk(ctx: Context): Boolean {
        if (CERT.isEmpty()) return true
        if (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) return true
        return try {
            val pm = ctx.packageManager
            @Suppress("DEPRECATION")
            val sig = if (Build.VERSION.SDK_INT >= 28) {
                pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo?.apkContentsSigners?.firstOrNull()
            } else {
                pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES).signatures?.firstOrNull()
            }
            sig != null && hex(MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())) == CERT
        } catch (e: Exception) { false }
    }

    private fun hex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }
}
