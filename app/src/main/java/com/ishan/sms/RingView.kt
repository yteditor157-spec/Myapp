package com.ishan.sms

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class RingView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    var percent: Int = 0
        set(value) { field = value.coerceIn(0, 100); invalidate() }

    private val d = resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 11 * d; color = Color.parseColor("#F3F0FF")
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 11 * d; strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#7C4DDB")
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = 20 * d; isFakeBoldText = true
        color = Color.parseColor("#1E1B3A")
    }
    private val rect = RectF()

    override fun onDraw(c: Canvas) {
        val pad = track.strokeWidth / 2
        rect.set(pad, pad, width - pad, height - pad)
        c.drawArc(rect, 0f, 360f, false, track)
        if (percent > 0) c.drawArc(rect, -90f, 360f * percent / 100f, false, arc)
        c.drawText("$percent%", width / 2f, height / 2f + txt.textSize / 3f, txt)
    }
}
