package com.app.magkraft.ml
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.app.magkraft.data.local.db.UserEntity
import com.app.magkraft.utils.ImageUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class UltraFastAnalyzer(
    private val faceOverlay: FaceOverlayView,
    private val users: List<UserEntity>,
    private val onMatch: (UserEntity) -> Unit,
    private val context : Context
) : ImageAnalysis.Analyzer {

    private var lastMatchTime = 0L
    private val COOLDOWN_MS = 3000L

    private var isProcessing = false

    /**
     * here is the gemini code
     */

    // Add this to your UltraFast class
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    faceOverlay.setDynamicRect(null, 0, 0)
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                val face = faces[0]
                val sensorWidth = imageProxy.width
                val sensorHeight = imageProxy.height

                // 1. Fix the UI Overlay: Pass raw sensor dimensions
                faceOverlay.setDynamicRect(face.boundingBox, sensorWidth, sensorHeight)

                if (!isProcessing && (System.currentTimeMillis() - lastMatchTime > 1000)) {
                    isProcessing = true

                    val bitmap = ImageUtils.yuvToBitmap(imageProxy)
                    bitmap?.let { full ->
                        // 2. Rotate the bitmap based on sensor metadata
                     //   val upright = ImageUtils.getCorrectedBitmap(full, rotationDegrees,true)

//                        val faceCrop = ImageUtils.cropToFaceRaw(
//                            corrected,
//                            face.boundingBox,
//                            sensorWidth,
//                            sensorHeight
//                        )
//
//                        val matrix = Matrix().apply { postScale(-1f, 1f, 56f, 56f) }
//                        val alignedFace = Bitmap.createBitmap(faceCrop, 0, 0, 112, 112, matrix, true)
//                        ImageUtils.saveBitmapToDisk(context, alignedFace, "scan_face")
                        // 2. STABLE CENTER-CROP (Fixes the "Half-Face" shift)
// 1. Get the face center as a PERCENTAGE of the sensor
                        // imageProxy.width/height are the dimensions ML Kit used for detection
                        // 1. Correct the orientation of the full bitmap
                        val upright = ImageUtils.getCorrectedBitmap(full, imageProxy.imageInfo.rotationDegrees,true)

                        // 2. 🔥 FIX THE STRETCH: Use sensor dimensions that match the rotation
                        // This ensures the "percentage" is calculated against the correct axis
                        val sensorWidth = if (imageProxy.imageInfo.rotationDegrees % 180 != 0) imageProxy.height else imageProxy.width
                        val sensorHeight = if (imageProxy.imageInfo.rotationDegrees % 180 != 0) imageProxy.width else imageProxy.height

                        // 3. Calculate Center Percent using the accurate sensor dimensions
                        val centerXPercent = face.boundingBox.centerX().toFloat() / sensorWidth
                        val centerYPercent = face.boundingBox.centerY().toFloat() / sensorHeight

                        // 4. Map to Bitmap pixels
                        val bitmapCenterX = centerXPercent * upright.width
                        val bitmapCenterY = centerYPercent * upright.height

                        // Calculate box size relative to the correct sensor width
                        val widthPercent = face.boundingBox.width().toFloat() / sensorWidth
                        val boxSize = (widthPercent * upright.width * 1.2f).toInt()

                        // 5. Calculate Bounds
                        val left = (bitmapCenterX - boxSize / 2).toInt().coerceIn(0, (upright.width - boxSize).coerceAtLeast(0))
                        val top = (bitmapCenterY - boxSize / 2).toInt().coerceIn(0, (upright.height - boxSize).coerceAtLeast(0))

                        // 6. Crop and Resize
                        val faceCrop = Bitmap.createBitmap(upright, left, top,
                            boxSize.coerceAtMost(upright.width - left),
                            boxSize.coerceAtMost(upright.height - top))
                        val resized = Bitmap.createScaledBitmap(faceCrop, 112, 112, true)

                        // 7. 🔥 THE MIRROR FLIP: Crucial to match 'register_face' ear-side
                        val matrix = Matrix().apply { postScale(-1f, 1f, 56f, 56f) }
                        val finalScanFace = Bitmap.createBitmap(resized, 0, 0, 112, 112, matrix, true)
                        // 7. Save to check the new center
                        ImageUtils.saveBitmapToDisk(context, finalScanFace, "scan_face_percent")
                        if (isImageDetailed(finalScanFace)) {
                            val embedding = FaceRecognizer.getInstance().getEmbedding(finalScanFace)
                            val match = FaceMatcher.findBestMatch(embedding, users)

                            if (match != null) {
                                lastMatchTime = System.currentTimeMillis()
                                Handler(Looper.getMainLooper()).post {
                                    faceOverlay.updateFaceStatus(true)
                                    onMatch(match)
                                }
                            }
                        }
                        // faceCrop.recycle() // Keep if passing to another function
                        upright.recycle()
                    }
                    isProcessing = false
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                imageProxy.close()
            }
    }

    /**
     * working code of oval shaped
     */
//    private fun getCroppedFace(bitmap: Bitmap): Bitmap? {
//        val size = (minOf(bitmap.width, bitmap.height) * 0.7f).toInt()
//        val left = (bitmap.width - size) / 2
//        val top = (bitmap.height - size) / 2
//
//        // 1. Initial Square Crop
//        val square = Bitmap.createBitmap(bitmap, left, top, size, size)
//
//        // 2. Scale to model input size (112x112)
//        val scaled = Bitmap.createScaledBitmap(square, 112, 112, true)
//
//        // 3. Only recycle square if scaled created a NEW instance
//        if (square != scaled) {
//            square.recycle()
//        }
//
//        return scaled
//
//    }

    private fun getCroppedFace(bitmap: Bitmap): Bitmap? {
        // 1. Get the same virtual oval coordinates used in Registration
        val viewOval = faceOverlay.getOvalRect()

        // 2. Map Screen Pixels to Bitmap Pixels
        val scaleX = bitmap.width.toFloat() / faceOverlay.width
        val scaleY = bitmap.height.toFloat() / faceOverlay.height

        val left = (viewOval.left * scaleX).toInt().coerceIn(0, bitmap.width - 10)
        val top = (viewOval.top * scaleY).toInt().coerceIn(0, bitmap.height - 10)
        val width = (viewOval.width() * scaleX).toInt().coerceAtMost(bitmap.width - left)
        val height = (viewOval.height() * scaleY).toInt().coerceAtMost(bitmap.height - top)

        // 3. Create the precise crop
        val square = Bitmap.createBitmap(bitmap, left, top, width, height)
        val scaled = Bitmap.createScaledBitmap(square, 112, 112, true)

        if (square != scaled) square.recycle()
        return scaled
    }
    private fun isImageDetailed(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var sum = 0.0
        var sumSq = 0.0
        val n = (width * height).toDouble()

        for (pixel in pixels) {
            // Fast grayscale conversion: (R+G+B)/3
            val gray = (((pixel shr 16) and 0xFF) + ((pixel shr 8) and 0xFF) + (pixel and 0xFF)) / 3.0
            sum += gray
            sumSq += gray * gray
        }

        // Variance formula: (SumSq / N) - (Mean^2)
        val mean = sum / n
        val variance = (sumSq / n) - (mean * mean)

        // 🔥 ADJUSTMENT: 100f is very safe.
        // If it's too hard to detect your face, try 50f or 70f.
        Log.d("VarianceCheck", "Variance: $variance")
        return variance > 70.0
    }
}



