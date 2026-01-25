package com.app.magkraft.ml
import android.graphics.Bitmap
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
    private val COOLDOWN_MS = 150L

    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastMatchTime < COOLDOWN_MS) {
            imageProxy.close()
            return
        }

        val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)
        bitmap?.let {
            val faceCrop = cropOvalFromBitmap(it, imageProxy.width, imageProxy.height)
            it.recycle()

            // ✅ Additional size check
            if (faceCrop.width >= 80 && faceCrop.height >= 80) {
                try {
                    val embedding = FaceRecognizer.getEmbedding(faceCrop)
                    faceCrop.recycle()

                    val match = FaceMatcher.findBestMatch(embedding, users)
                    if (match != null) {
                        lastMatchTime = now
                        onMatch(match)
                    }
                } catch (e: Exception) {
                    Log.e("UltraFast", "Embedding failed", e)
                    faceCrop.recycle()
                }
            } else {
                faceCrop.recycle()
            }
        }

        imageProxy.close()
    }

    // Fixed cropOvalFromBitmap() from above 👆



    private fun cropOvalFromBitmap(bitmap: Bitmap, frameWidth: Int, frameHeight: Int): Bitmap {
        // ✅ Use BITMAP dimensions, not frame dimensions
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        val ovalWidth = bitmap.width * 0.65f.coerceAtMost(bitmap.width.toFloat())
        val ovalHeight = bitmap.height * 0.75f.coerceAtMost(bitmap.height.toFloat())

        val left = (centerX - ovalWidth / 2).toInt().coerceIn(0, bitmap.width)
        val top = (centerY - ovalHeight / 2).toInt().coerceIn(0, bitmap.height)

        // ✅ Ensure bounds NEVER exceed bitmap
        val maxWidth = bitmap.width - left
        val maxHeight = bitmap.height - top
        val width = ovalWidth.toInt().coerceIn(50, maxWidth)  // Min 50px
        val height = ovalHeight.toInt().coerceIn(50, maxHeight)

        Log.d("CropDebug", "Bitmap: ${bitmap.width}x${bitmap.height}, Crop: $left,$top $width x$height")

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

}



