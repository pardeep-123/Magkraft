package com.app.magkraft.utils


import android.graphics.*
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.*

object FaceAligner {

    fun align(bitmap: Bitmap, face: Face): Bitmap? {

        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        if (leftEye == null || rightEye == null) return null

        // 1️⃣ Calculate rotation angle
        val dy = rightEye.y - leftEye.y
        val dx = rightEye.x - leftEye.x
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        // 2️⃣ Rotate image
        val matrix = Matrix()
        matrix.postRotate(angle)

        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )

        // 3️⃣ Adjust bounding box after rotation
        val box = face.boundingBox

        val cx = box.centerX()
        val cy = box.centerY()

        val newX = max(0, cx - box.width() / 2)
        val newY = max(0, cy - box.height() / 2)

        val width = min(box.width(), rotated.width - newX)
        val height = min(box.height(), rotated.height - newY)

        if (width <= 0 || height <= 0) return null

        return Bitmap.createBitmap(
            rotated,
            newX,
            newY,
            width,
            height
        )
    }
}
