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

    private var frameCount = 0
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
//            val fullBitmap = ImageUtils.imageProxyToBitmap(imageProxy)

//            if (fullBitmap != null) {
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
        /**
         * comment below at 25-01 at 22:16 to copy code from gemini
         */
//    override fun analyze(imageProxy: ImageProxy) {
//        frameCount++
//        if (frameCount % 20 != 0) {  // 🔥 Process ONLY 1/4 frames = 4x speed
//            imageProxy.close()
//            return
//        }
//
//        val now = System.currentTimeMillis()
//        if (now - lastMatchTime < COOLDOWN_MS) {
//            imageProxy.close()
//            return
//        }
//
//        // new code
//
//
//        val startTotal = System.currentTimeMillis()  // 🔥 TIMING
//        //
//        val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)
//        bitmap?.let {
//            val cropStart = System.currentTimeMillis()
//
//            val faceCrop = cropOvalFromBitmap(it, imageProxy.width, imageProxy.height)
//            it.recycle()
//            Log.d("TIME", "Crop: ${System.currentTimeMillis() - cropStart}ms")
//
//            // ✅ Additional size check
//            if (faceCrop.width >= 80 && faceCrop.height >= 80) {
//                try {
//                    val embedStart = System.currentTimeMillis()
//                    val embedding = FaceRecognizer.getInstance().getEmbedding(faceCrop)
//                    faceCrop.recycle()
//                    Log.d("TIME", "TFLite: ${System.currentTimeMillis() - embedStart}ms")
//                    val matchStart = System.currentTimeMillis()
//                    val match = FaceMatcher.findBestMatch(embedding, users)
//                    Log.d("TIME", "Match: ${System.currentTimeMillis() - matchStart}ms")
//                    Log.d("TIME", "TOTAL: ${System.currentTimeMillis() - startTotal}ms")
//
//
//                    if (match != null) {
//                        lastMatchTime = now
//                        onMatch(match)
//                    }
//                } catch (e: Exception) {
//                    Log.e("UltraFast", "Embedding failed", e)
////                    faceCrop.recycle()
//                }
//            } else {
//                faceCrop.recycle()
//            }
//        }
//
//        imageProxy.close()
//    }

        // Fixed cropOvalFromBitmap() from above 👆


       // private fun cropOvalFromBitmap(bitmap: Bitmap, frameWidth: Int, frameHeight: Int): Bitmap {
            // ✅ Use BITMAP dimensions, not frame dimensions
//        val centerX = bitmap.width / 2f
//        val centerY = bitmap.height / 2f
//        val ovalWidth = bitmap.width * 0.65f.coerceAtMost(bitmap.width.toFloat())
//        val ovalHeight = bitmap.height * 0.75f.coerceAtMost(bitmap.height.toFloat())
//
//        val left = (centerX - ovalWidth / 2).toInt().coerceIn(0, bitmap.width)
//        val top = (centerY - ovalHeight / 2).toInt().coerceIn(0, bitmap.height)
//
//        // ✅ Ensure bounds NEVER exceed bitmap
//        val maxWidth = bitmap.width - left
//        val maxHeight = bitmap.height - top
//        val width = ovalWidth.toInt().coerceIn(50, maxWidth)  // Min 50px
//        val height = ovalHeight.toInt().coerceIn(50, maxHeight)
//
//        Log.d("CropDebug", "Bitmap: ${bitmap.width}x${bitmap.height}, Crop: $left,$top $width x$height")
//
//        return Bitmap.createBitmap(bitmap, left, top, width, height)

            /**
             * gemini code
             */

            // Your logic is mostly fine, but let's ensure it's efficient
//            val centerX = bitmap.width / 2f
//            val centerY = bitmap.height / 2f
//            val ovalWidth = (bitmap.width * 0.60f).toInt()
//            val ovalHeight = (bitmap.height * 0.70f).toInt()
//
//            val left = (centerX - ovalWidth / 2).toInt().coerceIn(0, bitmap.width - ovalWidth)
//            val top = (centerY - ovalHeight / 2).toInt().coerceIn(0, bitmap.height - ovalHeight)
//
//            return Bitmap.createBitmap(bitmap, left, top, ovalWidth, ovalHeight)
//        }

    }
}



