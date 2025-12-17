package com.app.magkraft.ml

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF

object FaceCropper {

    fun crop(
        bitmap: Bitmap,
        overlayRect: RectF,
        overlayWidth: Int,
        overlayHeight: Int
    ): Bitmap {

        // 1️⃣ Calculate scale ratios
        val scaleX = bitmap.width.toFloat() / overlayWidth
        val scaleY = bitmap.height.toFloat() / overlayHeight

        // 2️⃣ Convert overlay rect → bitmap rect
        val left = (overlayRect.left * scaleX).toInt()
        val top = (overlayRect.top * scaleY).toInt()
        val right = (overlayRect.right * scaleX).toInt()
        val bottom = (overlayRect.bottom * scaleY).toInt()

        // 3️⃣ Clamp values (VERY IMPORTANT)
        val safeLeft = left.coerceIn(0, bitmap.width - 1)
        val safeTop = top.coerceIn(0, bitmap.height - 1)
        val safeRight = right.coerceIn(safeLeft + 1, bitmap.width)
        val safeBottom = bottom.coerceIn(safeTop + 1, bitmap.height)

        val width = safeRight - safeLeft
        val height = safeBottom - safeTop

        // 4️⃣ Final crop
        return Bitmap.createBitmap(
            bitmap,
            safeLeft,
            safeTop,
            width,
            height
        )
    }

}
