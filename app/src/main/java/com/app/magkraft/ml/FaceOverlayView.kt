package com.app.magkraft.ml

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * comment for new AI code
 */
//class FaceOverlayView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null
//) : View(context, attrs) {
//
//    private val borderPaint = Paint().apply {
//        color = Color.WHITE
//        style = Paint.Style.STROKE
//        strokeWidth = 6f
//        pathEffect = DashPathEffect(floatArrayOf(20f, 15f), 0f)
//        isAntiAlias = true
//    }
//
//    private val overlayPaint = Paint().apply {
//        color = Color.parseColor("#80000000") // semi-transparent black
//        style = Paint.Style.FILL
//    }
//
//    private val ovalRect = RectF()
//
//    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
//        super.onSizeChanged(w, h, oldw, oldh)
//
//        val centerX = w / 2f
//        val centerY = h / 2f
//
//        val ovalWidth = w * 0.65f
//        val ovalHeight = h * 0.75f
//
//        ovalRect.set(
//            centerX - ovalWidth / 2,
//            centerY - ovalHeight / 2,
//            centerX + ovalWidth / 2,
//            centerY + ovalHeight / 2
//        )
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//
//        // 🔲 Darken outside
//        val path = Path().apply {
//            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
//            addOval(ovalRect, Path.Direction.CCW)
//        }
//        canvas.drawPath(path, overlayPaint)
//
//        // 🔵 Draw dotted oval
//        canvas.drawOval(ovalRect, borderPaint)
//    }
//
//    fun getOvalRect(): RectF {
//        val centerX = width / 2f
//        val centerY = height / 2f
//
//        val ovalWidth = width * 0.65f
//        val ovalHeight = height * 0.75f
//
//        return RectF(
//            centerX - ovalWidth / 2,
//            centerY - ovalHeight / 2,
//            centerX + ovalWidth / 2,
//            centerY + ovalHeight / 2
//        )
//    }
//
//}

class FaceOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val borderPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private var isFaceDetected = false
    private val ovalRect = RectF()

    // Call this from the Activity/Fragment via the Analyzer callback
    fun updateFaceStatus(detected: Boolean) {
        this.isFaceDetected = detected
        invalidate() // 🔄 This tells Android to call onDraw() immediately
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Only draw the border if the Analyzer confirms a face is there
//        if (isFaceDetected) {
//            canvas.drawOval(getOvalRect(), borderPaint)
//        }
    }

    fun getOvalRect(): RectF {
        // Fallback: If view isn't laid out yet, return a small default to avoid 0-width crash
        if (width == 0 || height == 0) return RectF(0f, 0f, 1f, 1f)

        val centerX = width / 2f
        val centerY = height / 2f
        val ovalWidth = width * 0.70f
        val ovalHeight = height * 0.80f

        return RectF(
            centerX - ovalWidth / 2,
            centerY - ovalHeight / 2,
            centerX + ovalWidth / 2,
            centerY + ovalHeight / 2
        )
    }
}