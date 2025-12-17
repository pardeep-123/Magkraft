package com.app.magkraft.utils
import android.graphics.*
import androidx.camera.core.ImageProxy
import androidx.annotation.OptIn
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return when (imageProxy.format) {
            ImageFormat.JPEG -> jpegToBitmap(imageProxy)
            ImageFormat.YUV_420_888 -> yuvToBitmap(imageProxy)
            else -> null
        }
    }



// ================= JPEG =================

    private fun jpegToBitmap(imageProxy: ImageProxy): Bitmap? {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // ================= YUV =================
    @OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun yuvToBitmap(imageProxy: ImageProxy): Bitmap? {

        val image = imageProxy.image ?: return null
        val planes = image.planes

        // 🔒 SAFETY CHECK (prevents ArrayIndexOutOfBounds)
        if (planes.size < 3) return null

        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(0, 0, image.width, image.height),
            100,
            out
        )

        var bitmap = BitmapFactory.decodeByteArray(
            out.toByteArray(),
            0,
            out.size()
        )

        // 🔥 FIX ROTATION + MIRROR
        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f) // FRONT CAMERA UN-MIRROR
        }

        bitmap = Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        return bitmap
    }


}
