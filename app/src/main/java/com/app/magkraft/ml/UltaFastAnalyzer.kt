package com.app.magkraft.ml
import android.graphics.Bitmap
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
    private val onMatch: (UserEntity) -> Unit
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
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // Inside UltraFastAnalyzer.analyze success listener
        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    // No face? Clear the UI
                    faceOverlay.setDynamicRect(null, 0, 0)
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                val face = faces[0]
                // 1. Tell the UI to draw a circle around THIS face
                faceOverlay.setDynamicRect(face.boundingBox, imageProxy.width, imageProxy.height)

                if (!isProcessing && (System.currentTimeMillis() - lastMatchTime > COOLDOWN_MS)) {
                    isProcessing = true

                    val bitmap = ImageUtils.yuvToBitmap(imageProxy)
                    bitmap?.let { full ->
                        val corrected = ImageUtils.getCorrectedBitmap(full, imageProxy.imageInfo.rotationDegrees, true)

                        // 2. Crop exactly where the detector found the face
                        val faceCrop = ImageUtils.cropToFaceRaw(corrected, face.boundingBox, imageProxy.width, imageProxy.height)

                        // 3. Check detail and Match
                        if (isImageDetailed(faceCrop)) {
                            val embedding = FaceRecognizer.getInstance().getEmbedding(faceCrop)
                            val match = FaceMatcher.findBestMatch(embedding, users)

                            if (match != null) {
                                lastMatchTime = System.currentTimeMillis()
                                Handler(Looper.getMainLooper()).post {
                                    faceOverlay.updateFaceStatus(true) // Turn Green
                                    onMatch(match)
                                }
                            }
                        }
                        faceCrop.recycle()
                        corrected.recycle()
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



