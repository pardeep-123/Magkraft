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
//    @OptIn(androidx.camera.core.ExperimentalGetImage::class)
//    private fun yuvToBitmap(imageProxy: ImageProxy): Bitmap? {
//
//        val image = imageProxy.image ?: return null
//        val planes = image.planes
//
//        // 🔒 SAFETY CHECK (prevents ArrayIndexOutOfBounds)
//        if (planes.size < 3) return null
//
//        val yBuffer = planes[0].buffer
//        val uBuffer = planes[1].buffer
//        val vBuffer = planes[2].buffer
//
//        val ySize = yBuffer.remaining()
//        val uSize = uBuffer.remaining()
//        val vSize = vBuffer.remaining()
//
//        val nv21 = ByteArray(ySize + uSize + vSize)
//
//        yBuffer.get(nv21, 0, ySize)
//        vBuffer.get(nv21, ySize, vSize)
//        uBuffer.get(nv21, ySize + vSize, uSize)
//
//        val yuvImage = YuvImage(
//            nv21,
//            ImageFormat.NV21,
//            image.width,
//            image.height,
//            null
//        )
//
//        val out = ByteArrayOutputStream()
//        yuvImage.compressToJpeg(
//            Rect(0, 0, image.width, image.height),
//            100,
//            out
//        )
//
//        var bitmap = BitmapFactory.decodeByteArray(
//            out.toByteArray(),
//            0,
//            out.size()
//        )
//
//        // 🔥 FIX ROTATION + MIRROR
//        val matrix = Matrix().apply {
//            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
//            postScale(-1f, 1f) // FRONT CAMERA UN-MIRROR
//        }
//
//        bitmap = Bitmap.createBitmap(
//            bitmap,
//            0, 0,
//            bitmap.width,
//            bitmap.height,
//            matrix,
//            true
//        )
//
//        return bitmap
//    }

    fun cropFaceFromYuv(
        imageProxy: ImageProxy,
        boundingBox: Rect
    ): Bitmap {

        // 1️⃣ Convert YUV → Bitmap (only ONCE)
        val bitmap = imageProxy.toBitmap()

        // 2️⃣ Clamp bounding box safely
        val left = boundingBox.left.coerceAtLeast(0)
        val top = boundingBox.top.coerceAtLeast(0)
        val right = boundingBox.right.coerceAtMost(bitmap.width)
        val bottom = boundingBox.bottom.coerceAtMost(bitmap.height)

        val width = right - left
        val height = bottom - top

        // 3️⃣ Crop face
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    // 🔥 CameraX YUV → Bitmap (fast + stable)
    private fun ImageProxy.toBitmap(): Bitmap {
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
            width,
            height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)

        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun yuvToBitmap(imageProxy: ImageProxy): Bitmap? {
        val nv21 = yuv420ToNv21(imageProxy)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 90, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun yuv420ToNv21(image: ImageProxy): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }

    fun getCorrectedBitmap(bitmap: Bitmap, rotationDegrees: Int, isFrontCamera: Boolean): Bitmap {
        val matrix = Matrix()

        // 1. Handle Rotation
        matrix.postRotate(rotationDegrees.toFloat())

        // 2. Handle Mirroring
        // If it's the front camera, we flip it horizontally so it
        // matches a "standard" orientation (like the back camera would see)
        if (isFrontCamera) {
            matrix.postScale(-1f, 1f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Inside ImageUtils
    fun safeCrop(bitmap: Bitmap, viewOval: RectF, overlayWidth: Int, overlayHeight: Int): Bitmap? {
        val scaleX = bitmap.width.toFloat() / overlayWidth.coerceAtLeast(1)
        val scaleY = bitmap.height.toFloat() / overlayHeight.coerceAtLeast(1)

        // 1. Center of the screen
        val centerX = viewOval.centerX() * scaleX
        val centerY = viewOval.centerY() * scaleY

        // 2. 🔥 THE ZOOM FIX:
        // We take a smaller square (35% of bitmap width instead of 50%).
        // This forces the crop to be JUST the face, cutting out shoulders.
        val faceZoneSize = (bitmap.width * 0.40f)

        val left = (centerX - faceZoneSize / 2).toInt().coerceIn(0, bitmap.width - 1)
        val top = (centerY - faceZoneSize / 2).toInt().coerceIn(0, bitmap.height - 1)

        val width = faceZoneSize.toInt().coerceIn(1, bitmap.width - left)
        val height = faceZoneSize.toInt().coerceIn(1, bitmap.height - top)

        return try {
            val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
            // Scale to 112x112 so the AI sees a "Full Face" every time
            val standardized = Bitmap.createScaledBitmap(cropped, 112, 112, true)
            if (cropped != standardized) cropped.recycle()
            standardized
        } catch (e: Exception) {
            null
        }
    }
}

