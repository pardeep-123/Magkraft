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

        val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)
        bitmap?.let {
            // 🔥 Direct oval crop from analysis frame
            val faceCrop = cropOvalFromBitmap(it)
            it.recycle()

            if (faceCrop.width > 100 && faceCrop.height > 100) {
                onFaceReady(faceCrop)
            } else {
                faceCrop.recycle()
            }
        }// Send aligned crop
        imageProxy.close()
    }

    private fun cropOvalFromBitmap(bitmap: Bitmap): Bitmap {
        // ✅ Use ONLY bitmap dimensions - ignore imageProxy!
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        if (bitmapWidth < 100 || bitmapHeight < 100) {
            // Return tiny bitmap to avoid processing
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val centerX = bitmapWidth / 2f
        val centerY = bitmapHeight / 2f
        val ovalWidth = (bitmapWidth * 0.65f).coerceAtMost(bitmapWidth.toFloat())
        val ovalHeight = (bitmapHeight * 0.75f).coerceAtMost(bitmapHeight.toFloat())

        // ✅ Perfect bounds - IMPOSSIBLE to exceed
        val left = ((centerX - ovalWidth / 2f).coerceIn(0f, centerX)).toInt()
        val top = ((centerY - ovalHeight / 2f).coerceIn(0f, centerY)).toInt()
        val width = (ovalWidth.coerceIn(80f, bitmapWidth.toFloat())).toInt()
        val height = (ovalHeight.coerceIn(80f, bitmapHeight.toFloat())).toInt()

        // ✅ FINAL SAFETY CHECK
        require(left >= 0 && top >= 0 && left + width <= bitmapWidth && top + height <= bitmapHeight) {
            "Invalid crop: bitmap=${bitmapWidth}x$bitmapHeight, crop=($left,$top,$width,$height)"
        }

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }


}

/// this is from old perplexcity
//class RegisterAnalyzer(
//    private val onFaceAligned: (Bitmap) -> Unit  // Same callback signature
//) : ImageAnalysis.Analyzer {
//
//    private var lastProcessTime = 0L
//    private val PROCESS_INTERVAL_MS = 500L
//
//    private val detector = FaceDetection.getClient(
//        FaceDetectorOptions.Builder()
//            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
//            .build()
//    )
//
//    @androidx.annotation.OptIn(ExperimentalGetImage::class)
//    override fun analyze(imageProxy: ImageProxy) {
//        val now = System.currentTimeMillis()
//        if (now - lastProcessTime < PROCESS_INTERVAL_MS) {
//            imageProxy.close()
//            return
//        }
//        lastProcessTime = now
//
//        val mediaImage = imageProxy.image ?: run {
//            imageProxy.close()
//            return
//        }
//
//        val inputImage = InputImage.fromMediaImage(
//            mediaImage,
//            imageProxy.imageInfo.rotationDegrees
//        )
//
//        detector.process(inputImage)
//            .addOnSuccessListener { faces ->
//                if (faces.isNotEmpty()) {
//                    val face = faces[0]
//                    val bitmap = com.app.magkraft.utils.ImageUtils
//                        .imageProxyToBitmap(imageProxy)
//
//                    bitmap?.let { nonNullBitmap ->
//                        val alignedFace = FaceAligner.align(nonNullBitmap, face)
//                        alignedFace?.let { onFaceAligned(it) }
//                    }
//                }
//            }
//            .addOnCompleteListener {
//                imageProxy.close()
//            }
//    }
//}
