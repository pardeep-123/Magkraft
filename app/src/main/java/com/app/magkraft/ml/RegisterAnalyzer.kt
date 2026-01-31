package com.app.magkraft.ml

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.app.magkraft.utils.FaceAligner
import com.app.magkraft.utils.ImageUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

// New Code from Perplexcity

class RegisterAnalyzer(
    private val onFaceReady: (Bitmap) -> Unit  // Renamed for clarity
) : ImageAnalysis.Analyzer {

    private var lastProcessTime = 0L
    private val PROCESS_INTERVAL_MS = 200L  // Faster for registration

    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastProcessTime < PROCESS_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        lastProcessTime = now
        try {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = ImageUtils.yuvToBitmap(imageProxy)
            bitmap?.let { fullBitmap ->
                val analyzedFace = ImageUtils.getCorrectedBitmap(fullBitmap, rotation, isFrontCamera = true)
                fullBitmap.recycle()
                // 🔥 Direct oval crop from analysis frame
                val faceCrop = cropOvalFromBitmap(analyzedFace)

                // 4. Clean up the corrected full-size frame
                analyzedFace.recycle()


            if (faceCrop.width > 100 && faceCrop.height > 100) {
                onFaceReady(faceCrop)
            } else {
                faceCrop.recycle()
            }
            }// Send aligned crop
        }
        catch (e: Exception) {
            Log.e("RegisterAnalyzer", "Analysis error: ${e.message}")
        }
        finally {
            imageProxy.close()
        }

    }


    private fun cropOvalFromBitmap(bitmap: Bitmap): Bitmap {
        // 1. Determine the square size (70% of the shortest side is usually perfect for a face)
        val minEdge = minOf(bitmap.width, bitmap.height)
        val size = (minEdge * 0.75f).toInt()

        // 2. Calculate coordinates to pull from the exact center
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2

        // 3. Strict bounds checking to prevent "y + height must be <= bitmap.height" crashes
        val safeLeft = left.coerceIn(0, bitmap.width - size)
        val safeTop = top.coerceIn(0, bitmap.height - size)

        return Bitmap.createBitmap(bitmap, safeLeft, safeTop, size, size)
    }
}