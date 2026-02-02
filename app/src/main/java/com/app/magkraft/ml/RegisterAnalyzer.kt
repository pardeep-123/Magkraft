package com.app.magkraft.ml

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.app.magkraft.utils.ImageUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * working code of oval shaped
 */
//class RegisterAnalyzer(
//    private val onFaceReady: (Bitmap) -> Unit  // Renamed for clarity
//) : ImageAnalysis.Analyzer {
//
//    private var lastProcessTime = 0L
//    private val PROCESS_INTERVAL_MS = 200L  // Faster for registration
//
//    override fun analyze(imageProxy: ImageProxy) {
//        val now = System.currentTimeMillis()
//        if (now - lastProcessTime < PROCESS_INTERVAL_MS) {
//            imageProxy.close()
//            return
//        }
//        lastProcessTime = now
//        try {
//            val rotation = imageProxy.imageInfo.rotationDegrees
//            val bitmap = ImageUtils.yuvToBitmap(imageProxy)
//            bitmap?.let { fullBitmap ->
//                val analyzedFace = ImageUtils.getCorrectedBitmap(fullBitmap, rotation, isFrontCamera = true)
//                fullBitmap.recycle()
//                // 🔥 Direct oval crop from analysis frame
//                val faceCrop = cropOvalFromBitmap(analyzedFace)
//
//                // 4. Clean up the corrected full-size frame
//                analyzedFace.recycle()
//
//
//            if (faceCrop.width > 100 && faceCrop.height > 100) {
//                onFaceReady(faceCrop)
//            } else {
//                faceCrop.recycle()
//            }
//            }// Send aligned crop
//        }
//        catch (e: Exception) {
//            Log.e("RegisterAnalyzer", "Analysis error: ${e.message}")
//        }
//        finally {
//            imageProxy.close()
//        }
//
//    }
//
//
//    private fun cropOvalFromBitmap(bitmap: Bitmap): Bitmap {
//        // 1. Determine the square size (70% of the shortest side is usually perfect for a face)
//        val minEdge = minOf(bitmap.width, bitmap.height)
//        val size = (minEdge * 0.75f).toInt()
//
//        // 2. Calculate coordinates to pull from the exact center
//        val left = (bitmap.width - size) / 2
//        val top = (bitmap.height - size) / 2
//
//        // 3. Strict bounds checking to prevent "y + height must be <= bitmap.height" crashes
//        val safeLeft = left.coerceIn(0, bitmap.width - size)
//        val safeTop = top.coerceIn(0, bitmap.height - size)
//
//        return Bitmap.createBitmap(bitmap, safeLeft, safeTop, size, size)
//    }
//}

class RegisterAnalyzer(
    private val faceOverlay: FaceOverlayView, // 🔥 Pass this to sync crop coordinates
    private val onFaceReady: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastProcessTime = 0L
    private val PROCESS_INTERVAL_MS = 200L

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
                // 1. Correct Orientation and Mirroring
                val correctedBitmap = ImageUtils.getCorrectedBitmap(fullBitmap, rotation, isFrontCamera = true)
                fullBitmap.recycle()

                // 2. 🔥 Sync the Crop with the UI Overlay coordinates
//                val faceCrop = cropToMatchOverlay(correctedBitmap)
                // 🔥 CALL IT HERE: Match the live frame to the UI Oval
                val faceCrop = ImageUtils.safeCrop(correctedBitmap, faceOverlay.getOvalRect(),
                    overlayWidth = faceOverlay.width,
                    overlayHeight = faceOverlay.height
                    )
                correctedBitmap.recycle()

                // 3. Size check and delivery
                if (faceCrop != null && faceCrop.width > 50 && faceCrop.height > 50) {
                    onFaceReady(faceCrop)

                } else {
                    faceCrop?.recycle()

                }
            }
        } catch (e: Exception) {
            Log.e("RegisterAnalyzer", "Analysis error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun cropToMatchOverlay(bitmap: Bitmap): Bitmap? {
        // Get the coordinates from the same View used in capturePhoto
        val viewOval = faceOverlay.getOvalRect()

        // Calculate Scale (Screen UI vs. Camera Bitmap)
        val scaleX = bitmap.width.toFloat() / faceOverlay.width
        val scaleY = bitmap.height.toFloat() / faceOverlay.height

        val left = (viewOval.left * scaleX).toInt().coerceAtLeast(0)
        val top = (viewOval.top * scaleY).toInt().coerceAtLeast(0)
        val width = (viewOval.width() * scaleX).toInt().coerceAtMost(bitmap.width - left)
        val height = (viewOval.height() * scaleY).toInt().coerceAtMost(bitmap.height - top)

        return try {
            Bitmap.createBitmap(bitmap, left, top, width, height)
        } catch (e: Exception) {
            null
        }
    }
}