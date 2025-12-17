package com.app.magkraft.ml

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        pathEffect = DashPathEffect(floatArrayOf(20f, 15f), 0f)
        isAntiAlias = true
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000") // semi-transparent black
        style = Paint.Style.FILL
    }

    private val ovalRect = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val centerX = w / 2f
        val centerY = h / 2f

        val ovalWidth = w * 0.65f
        val ovalHeight = h * 0.75f

        ovalRect.set(
            centerX - ovalWidth / 2,
            centerY - ovalHeight / 2,
            centerX + ovalWidth / 2,
            centerY + ovalHeight / 2
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 🔲 Darken outside
        val path = Path().apply {
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addOval(ovalRect, Path.Direction.CCW)
        }
        canvas.drawPath(path, overlayPaint)

        // 🔵 Draw dotted oval
        canvas.drawOval(ovalRect, borderPaint)
    }

    fun getOvalRect(): RectF {
        val centerX = width / 2f
        val centerY = height / 2f

        val ovalWidth = width * 0.65f
        val ovalHeight = height * 0.75f

        return RectF(
            centerX - ovalWidth / 2,
            centerY - ovalHeight / 2,
            centerX + ovalWidth / 2,
            centerY + ovalHeight / 2
        )
    }

}
