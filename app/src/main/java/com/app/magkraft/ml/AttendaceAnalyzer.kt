package com.app.magkraft.ml

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.app.magkraft.utils.FaceAligner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions


class AttendanceAnalyzer(
    private val onFaceDetected: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastProcessTime = 0L
    private val PROCESS_INTERVAL_MS = 500L  // Process every 500ms

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
    )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastProcessTime < PROCESS_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        lastProcessTime = now

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {

                    val face = faces[0]

                    val bitmap = com.app.magkraft.utils.ImageUtils
                        .imageProxyToBitmap(imageProxy)

                    bitmap?.let { nonNullBitmap ->

                        // ✅ ALIGN FACE (rotation + crop)
                        val alignedFace = FaceAligner.align(nonNullBitmap, face)

                        alignedFace?.let {
                            onFaceDetected(it)
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }

    }
}
