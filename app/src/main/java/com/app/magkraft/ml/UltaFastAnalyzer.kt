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
//                val faceCrop = getCroppedFace(correctedBitmap)
                val faceCrop = ImageUtils.safeCrop(correctedBitmap, faceOverlay.getOvalRect(),
                    overlayWidth = faceOverlay.width,
                    overlayHeight = faceOverlay.height)
                // Immediately recycle the huge original bitmap to free memory
                correctedBitmap.recycle()

                if (faceCrop?.width!! >= 50 && faceCrop.height >= 50) {


                    // If the embedding values are too close to zero (flat), it's a wall, not a face.
                    if (isImageDetailed(faceCrop)) {
                        val embedding = FaceRecognizer.getInstance().getEmbedding(faceCrop)
                    val match = FaceMatcher.findBestMatch(embedding, users)

                    if (match != null) {
                        lastMatchTime = System.currentTimeMillis()
                        // Use a Handler to post to Main Thread safely
                        Handler(Looper.getMainLooper()).post {
                            // ✅ Show feedback to the user immediately
                            faceOverlay.updateFaceStatus(true)
                            onMatch(match)
                        }}
                    }else {
                        // Optional: Update overlay to show a "searching" state
                         Handler(Looper.getMainLooper()).post { faceOverlay.updateFaceStatus(false) }
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

    private fun isEmbeddingValid(embedding: FloatArray): Boolean {
        // A real face embedding should have a certain amount of "energy"
        var sumOfSquares = 0f
        for (value in embedding) {
            sumOfSquares += value * value
        }
        // Normalized embeddings should have a sum of squares near 1.0.
        // If it's near 0, the model didn't find any features.
        return sumOfSquares > 0.8f
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

        var sum = 0f
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            sum += (r + g + b) / 3f
        }
        val average = sum / (width * height)

        var variance = 0f
        for (pixel in pixels) {
            val gray = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3f
            variance += (gray - average) * (gray - average)
        }

        // If variance is very low, it's a flat surface (wall).
        // Usually, a face has a variance > 100. Adjust this number if needed.
        return (variance / (width * height)) > 100f
    }
}



