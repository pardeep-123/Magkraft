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
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    private var isFaceDetected = false
    private val ovalRect = RectF()

    // Call this from the Activity/Fragment via the Analyzer callback
    fun updateFaceStatus(detected: Boolean) {
        this.isFaceDetected = detected
        postInvalidate() // Refresh the UI
    }
    // In FaceOverlayView.kt
    private var currentFaceRect: RectF? = null

    fun setDynamicRect(rect: Rect?, sensorWidth: Int, sensorHeight: Int) {
        if (rect == null) {
            currentFaceRect = null
            postInvalidate()
            return
        }

        // 🔥 SWAP for Portrait Tablet:
        // SensorWidth (Long side) maps to View Height
        // SensorHeight (Short side) maps to View Width
        val scaleX = width.toFloat() / sensorHeight.toFloat()
        val scaleY = height.toFloat() / sensorWidth.toFloat()

        // Mirroring for front camera
        val left = (sensorHeight - rect.right) * scaleX
        val right = (sensorHeight - rect.left) * scaleX
        val top = rect.top * scaleY
        val bottom = rect.bottom * scaleY

        // 🔥 FORCE OVAL RATIO: Even if the view is wide, make the oval look like a face
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2
        val portraitWidth = (right - left) * 0.9f
        val portraitHeight = portraitWidth * 1.4f // Standard head ratio

        currentFaceRect = RectF(
            centerX - portraitWidth / 2,
            centerY - portraitHeight / 2,
            centerX + portraitWidth / 2,
            centerY + portraitHeight / 2
        )
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        currentFaceRect?.let { rect ->
            val centerX = rect.centerX()
            val centerY = rect.centerY()

            // Force the oval to be 1.4 times taller than it is wide
            // This prevents the "Horizontal/Flattened" look on Tablets
            val headWidth = rect.width() * 0.8f
            val headHeight = headWidth * 1.4f

            val ovalRect = RectF(
                centerX - headWidth / 2,
                centerY - headHeight / 2,
                centerX + headWidth / 2,
                centerY + headHeight / 2
            )
            canvas.drawOval(ovalRect, paint)
        }
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