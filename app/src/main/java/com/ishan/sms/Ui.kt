package com.ishan.sms

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Ui {
    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    fun pill(tv: TextView, fg: String, bg: String) {
        val c = tv.context
        tv.setTextColor(Color.parseColor(fg))
        tv.background = GradientDrawable().apply {
            cornerRadius = dp(c, 50).toFloat()
            setColor(Color.parseColor(bg))
        }
        tv.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4))
    }

    fun money(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.2f", v)

    fun time(ms: Long): String = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(Date(ms))

    fun methodName(m: String) = when (m) { "bkash" -> "bKash"; "nagad" -> "Nagad"; "rocket" -> "Rocket"; else -> m }
    fun methodColor(m: String) = when (m) { "bkash" -> "#E2136E"; "nagad" -> "#F6921E"; "rocket" -> "#8C3494"; else -> "#6B6785" }
}
