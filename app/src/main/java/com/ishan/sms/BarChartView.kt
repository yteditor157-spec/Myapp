package com.ishan.sms

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    var values: IntArray = IntArray(7)
        set(v) { field = v; invalidate() }
    var labels: Array<String> = arrayOf("", "", "", "", "", "", "")
        set(v) { field = v; invalidate() }

    private val d = resources.displayMetrics.density
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lab = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = 14 * d; color = Color.parseColor("#6B6785")
    }
    private val num = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = 12 * d; isFakeBoldText = true
        color = Color.parseColor("#4A25A8")
    }

    override fun onDraw(c: Canvas) {
        val n = values.size
        val w = width.toFloat(); val h = height.toFloat()
        val labelH = 30 * d
        val topSpace = 20 * d
        val minBar = 9 * d
        val maxBar = h - labelH - topSpace
        val slot = w / n
        val bw = slot * 0.42f
        val maxV = (values.maxOrNull() ?: 0).coerceAtLeast(1)
        for (i in 0 until n) {
            val cx = slot * i + slot / 2
            val bh = minBar + (maxBar - minBar) * values[i] / maxV
            val bottom = h - labelH
            val top = bottom - bh
            bar.color = Color.parseColor(if (i == n - 1) "#7C4DDB" else "#DDD6FA")
            val r = minOf(bw / 2, bh / 2)
            c.drawRoundRect(cx - bw / 2, top, cx + bw / 2, bottom, r, r, bar)
            if (values[i] > 0) c.drawText(values[i].toString(), cx, top - 6 * d, num)
            c.drawText(labels.getOrElse(i) { "" }, cx, h - 6 * d, lab)
        }
    }
}
