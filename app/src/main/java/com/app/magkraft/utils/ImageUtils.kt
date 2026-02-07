package com.app.magkraft.utils
import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.annotation.OptIn
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

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

//    fun yuvToBitmap(imageProxy: ImageProxy): Bitmap? {
//        val nv21 = yuv420ToNv21(imageProxy)
//        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
//        val out = ByteArrayOutputStream()
//        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 90, out)
//        val imageBytes = out.toByteArray()
//        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//    }
//
//    private fun yuv420ToNv21(image: ImageProxy): ByteArray {
//        val yBuffer = image.planes[0].buffer
//        val uBuffer = image.planes[1].buffer
//        val vBuffer = image.planes[2].buffer
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
//        return nv21
//    }

    fun yuvToBitmap(imageProxy: ImageProxy): Bitmap? {
        val nv21 = yuvToNv21(imageProxy) // Using the stride-aware version
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        // Use 100 for max quality to help the AI score
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun yuvToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yStride = yPlane.rowStride
        val uStride = uPlane.rowStride
        val vStride = vPlane.rowStride
        val pixelStride = uPlane.pixelStride

        val nv21 = ByteArray(width * height * 3 / 2)

        // 1. Copy Y Plane
        var idY = 0
        for (row in 0 until height) {
            yBuffer.position(row * yStride)
            // 🔥 SAFETY CHECK: Only read what's left in the buffer
            val remaining = yBuffer.remaining()
            val bytesToRead = if (remaining < yStride) remaining else yStride

            val rowData = ByteArray(bytesToRead)
            yBuffer.get(rowData)

            // Only copy 'width' amount to skip padding
            System.arraycopy(rowData, 0, nv21, idY, width.coerceAtMost(bytesToRead))
            idY += width
        }

        // 2. Copy U/V Planes
        var idUV = width * height
        val uvHeight = height / 2
        val uvWidth = width / 2

        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                val vPos = row * vStride + col * pixelStride
                val uPos = row * uStride + col * pixelStride

                // 🔥 SAFETY CHECK: Ensure positions are within buffer limits
                if (vPos < vBuffer.capacity() && uPos < uBuffer.capacity()) {
                    nv21[idUV++] = vBuffer.get(vPos)
                    nv21[idUV++] = uBuffer.get(uPos)
                }
            }
        }

        return nv21
    }    fun getCorrectedBitmap(bitmap: Bitmap, rotationDegrees: Int, isFrontCamera: Boolean): Bitmap {
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
        // 1. Calculate how much bigger the camera image is than the screen
        val scaleX = bitmap.width.toFloat() / overlayWidth.coerceAtLeast(1)
        val scaleY = bitmap.height.toFloat() / overlayHeight.coerceAtLeast(1)

        // 2. Map the UI Oval coordinates to the Bitmap pixels
        val left = (viewOval.left * scaleX).toInt().coerceIn(0, bitmap.width - 1)
        val top = (viewOval.top * scaleY).toInt().coerceIn(0, bitmap.height - 1)
        val width = (viewOval.width() * scaleX).toInt().coerceIn(1, bitmap.width - left)
        val height = (viewOval.height() * scaleY).toInt().coerceIn(1, bitmap.height - top)

        return try {
            val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)

            // 3. 🔥 THE CRITICAL STEP:
            // Force the face inside the oval to be exactly 112x112.
            // This makes the "distance" look identical to the AI every time.
            val standardized = Bitmap.createScaledBitmap(cropped, 112, 112, true)

            if (cropped != standardized) cropped.recycle()
            standardized
        } catch (e: Exception) {
            null
        }
    }

    fun cropToFace(bitmap: Bitmap, faceRect: Rect, previewWidth: Int, previewHeight: Int): Bitmap {
        // 1. Calculate the ratio between the bitmap and the preview stream
        val scaleX = bitmap.width.toFloat() / previewWidth
        val scaleY = bitmap.height.toFloat() / previewHeight

        // 2. Map coordinates and add a small "padding" so we don't crop too tightly
        val padding = (faceRect.width() * 0.1f).toInt()
        val left = (faceRect.left * scaleX).toInt().coerceIn(0, bitmap.width - 1)
        val top = (faceRect.top * scaleY).toInt().coerceIn(0, bitmap.height - 1)
        val width = (faceRect.width() * scaleX).toInt().coerceAtMost(bitmap.width - left)
        val height = (faceRect.height() * scaleY).toInt().coerceAtMost(bitmap.height - top)

        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)

        // 3. Always resize to 112x112 for the model
        val standardized = Bitmap.createScaledBitmap(cropped, 112, 112, true)
        if (cropped != standardized) cropped.recycle()

        return standardized
    }

    fun cropToFaceRaw(bitmap: Bitmap, faceRect: Rect, sensorWidth: Int, sensorHeight: Int): Bitmap {
        val scaleX = bitmap.width.toFloat() / sensorWidth
        val scaleY = bitmap.height.toFloat() / sensorHeight

        // Standard mapping for raw sensor data
        val left = (faceRect.left * scaleX).toInt().coerceIn(0, bitmap.width - 1)
        val top = (faceRect.top * scaleY).toInt().coerceIn(0, bitmap.height - 1)
        val width = (faceRect.width() * scaleX).toInt().coerceAtMost(bitmap.width - left)
        val height = (faceRect.height() * scaleY).toInt().coerceAtMost(bitmap.height - top)

        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        val final = Bitmap.createScaledBitmap(cropped, 112, 112, true)
        if (cropped != final) cropped.recycle()
        return final
    }

    fun cropToFaceMirrored(bitmap: Bitmap, rect: Rect, viewWidth: Int, viewHeight: Int): Bitmap {
        val scaleX = bitmap.width.toFloat() / viewWidth
        val scaleY = bitmap.height.toFloat() / viewHeight

        // This is the direct mapping that worked for your oval
        val left = (viewWidth - rect.right) * scaleX
        val top = rect.top * scaleY
        val width = rect.width() * scaleX
        val height = rect.height() * scaleY

        // Create the square crop
        val size = (width * 1.1f).toInt()
        val centerX = left + (width / 2)
        val centerY = top + (height / 2)

        val cropLeft =
            (centerX - size / 2).toInt().coerceIn(0, (bitmap.width - size).coerceAtLeast(0))
        val cropTop =
            (centerY - size / 2).toInt().coerceIn(0, (bitmap.height - size).coerceAtLeast(0))

        val cropped = Bitmap.createBitmap(
            bitmap, cropLeft, cropTop,
            size.coerceAtMost(bitmap.width - cropLeft),
            size.coerceAtMost(bitmap.height - cropTop)
        )

        return Bitmap.createScaledBitmap(cropped, 112, 112, true)
    }

    fun saveBitmapToDisk(context: Context, bitmap: Bitmap, fileName: String) {
        try {
            val file = File(context.filesDir, "$fileName.png")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            Log.d("DEBUG_IMAGE", "Saved $fileName to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("DEBUG_IMAGE", "Failed to save $fileName", e)
        }
    }
}