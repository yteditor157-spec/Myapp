package com.ishan.sms

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Draws the (encrypted) credit line itself. There is no text in the XML to edit. */
class BrandFooterView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    private val d = resources.displayMetrics.density
    private val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12 * d; color = Color.parseColor("#8B86A8") }
    private val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13 * d; isFakeBoldText = true; color = Color.parseColor("#7C4DDB")
    }

    @Volatile var drawnOk = false
        private set

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (34 * d).toInt())
    }

    /** true only if the real credit line was drawn and the view is fully visible. */
    fun isIntact(): Boolean =
        drawnOk && visibility == VISIBLE && alpha >= 0.99f && width > 0 && height > 0

    override fun onDraw(c: Canvas) {
        val t = Brand.text(context)
        if (t == null) { drawnOk = false; return }
        val i = t.lastIndexOf(' ') + 1
        val left = t.substring(0, i)
        val right = t.substring(i)
        val total = soft.measureText(left) + bold.measureText(right)
        val x = (width - total) / 2f
        val y = height / 2f + 5 * d
        c.drawText(left, x, y, soft)
        c.drawText(right, x + soft.measureText(left), y, bold)
        drawnOk = true
    }
}
