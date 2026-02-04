package com.app.magkraft.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.app.magkraft.ui.RegisterActivity
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
    private val onFaceReady: (Bitmap) -> Unit,
    private val activity: RegisterActivity
) : ImageAnalysis.Analyzer {

    private var lastProcessTime = 0L
    private val PROCESS_INTERVAL_MS = 200L
    private var isProcessing = false
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    faceOverlay.setDynamicRect(null, 0, 0) // Clear oval if no face
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                val face = faces[0]
                activity.latestDetectedFaceRect = face.boundingBox // Update activity
                // 🔥 Pass imageProxy dimensions so the Overlay can map them to the screen
                faceOverlay.setDynamicRect(face.boundingBox,
                    imageProxy.width,
                    imageProxy.height)

                val now = System.currentTimeMillis()
                if (!isProcessing && (now - lastProcessTime > 500)) {
                    isProcessing = true
                    lastProcessTime = now

                    val bitmap = ImageUtils.yuvToBitmap(imageProxy)
                    bitmap?.let { full ->
                        // isFrontCamera = true is critical here for mirroring
                        val corrected = ImageUtils.getCorrectedBitmap(full, imageProxy.imageInfo.rotationDegrees, true)

                        // This crop MUST be identical to your ScanActivity crop
                        val faceCrop = ImageUtils.cropToFaceMirrored(corrected, face.boundingBox, imageProxy.width, imageProxy.height)

                        if (isImageDetailed(faceCrop)) {
                            onFaceReady(faceCrop) // Send to UI for preview/storage

                            Handler(Looper.getMainLooper()).post {
                                faceOverlay.updateFaceStatus(true) // Turn Green
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                faceOverlay.updateFaceStatus(false) // Turn Red/White
                            }
                        }
                        corrected.recycle()
                        // Note: Don't recycle faceCrop here if onFaceReady needs to display it
                    }
                    isProcessing = false
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                Log.e("RegisterAnalyzer", "Detection failed", it)
                imageProxy.close()
            }
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