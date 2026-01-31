package com.app.magkraft.ml
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.app.magkraft.data.local.db.UserEntity
import com.app.magkraft.utils.ImageUtils

class UltraFastAnalyzer(
    private val users: List<UserEntity>,
    private val onMatch: (UserEntity) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastMatchTime = 0L
    private val COOLDOWN_MS = 3000L

    private var isProcessing = false

    /**
     * here is the gemini code
     */
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()

        // 1. Quick exits: Cooldown OR already processing a frame
        if (isProcessing || (now - lastMatchTime < COOLDOWN_MS)) {
            imageProxy.close()
            return
        }

        isProcessing = true // Lock

        try {
            // 1. Convert YUV ImageProxy to Bitmap only when needed
            // Most Face SDKs have a utility for this, e.g., ImageUtils or FaceNet
            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = ImageUtils.yuvToBitmap(imageProxy)
            // 2. Convert to Bitmap ONCE

            bitmap?.let { fullBitmap ->
                val correctedBitmap = ImageUtils.getCorrectedBitmap(fullBitmap, rotation, isFrontCamera = true)
                fullBitmap.recycle()
                // 3. Perform the crop
                val faceCrop = getCroppedFace(correctedBitmap)

                // Immediately recycle the huge original bitmap to free memory
                correctedBitmap.recycle()

                if (faceCrop?.width!! >= 80 && faceCrop.height >= 80) {
                    val embedding = FaceRecognizer.getInstance().getEmbedding(faceCrop)
                    val match = FaceMatcher.findBestMatch(embedding, users)

                    if (match != null) {
                        lastMatchTime = System.currentTimeMillis()
                        // Use a Handler to post to Main Thread safely
                        Handler(Looper.getMainLooper()).post {
                            onMatch(match)
                        }
                    }
                }
                faceCrop.recycle() // Clean up the crop
            }
        } catch (e: Exception) {
            Log.e("UltraFast", "Analysis error", e)
        } finally {
            // 4. ALWAYS close the proxy and unlock
            imageProxy.close()
            isProcessing = false
        }
    }

    private fun getCroppedFace(bitmap: Bitmap): Bitmap? {
        val size = (minOf(bitmap.width, bitmap.height) * 0.7f).toInt()
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2

        // 1. Initial Square Crop
        val square = Bitmap.createBitmap(bitmap, left, top, size, size)

        // 2. Scale to model input size (112x112)
        val scaled = Bitmap.createScaledBitmap(square, 112, 112, true)

        // 3. Only recycle square if scaled created a NEW instance
        if (square != scaled) {
            square.recycle()
        }

        return scaled

    }
}



