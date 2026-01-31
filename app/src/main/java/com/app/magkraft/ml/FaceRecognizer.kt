package com.app.magkraft.ml

//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.Color
//import org.tensorflow.lite.Interpreter
//import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
//import java.nio.MappedByteBuffer
//import java.nio.channels.FileChannel
import androidx.core.graphics.scale
import androidx.core.graphics.get
import com.app.magkraft.utils.MyApp

/// New code from perplexity

import org.tensorflow.lite.Interpreter
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt


class FaceRecognizer private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: FaceRecognizer? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = FaceRecognizer()
                        INSTANCE!!.initInterpreter(context)
                    }
                }
            }
        }

        fun getInstance(): FaceRecognizer {
            return INSTANCE ?: throw IllegalStateException("FaceRecognizer not initialized. Call initialize() first.")
        }
    }

    private var interpreter: Interpreter? = null
    private val MODEL_PATH = "mobile_facenet.tflite"  // Your model filename
    private val INPUT_SIZE = 112  // MobileFaceNet standard input
    private val EMBEDDING_SIZE = 192  // Your embedding dimension

    // 🔥 Reuse this buffer to stop Major Faults / GC pressure
    private val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
        order(ByteOrder.nativeOrder())
    }

    private fun initInterpreter(context: Context) {
        try {
            val modelBuffer = loadModelFile(context.assets, MODEL_PATH)
            val options = Interpreter.Options().apply {
                // 1. Try GPU first (Best performance)
                try {
                    val compatibilityList = CompatibilityList()
                    if (compatibilityList.isDelegateSupportedOnThisDevice) {
                        val delegateOptions = compatibilityList.bestOptionsForThisDevice
                        addDelegate(GpuDelegate(delegateOptions))
                        Log.d("FaceRecognizer", "GPU Delegate added")
                    } else {
                        // 2. Fallback to optimized CPU
                        setNumThreads(4)
                        Log.d("FaceRecognizer", "GPU not supported, using 4 CPU threads")
                    }
                } catch (e: Exception) {
                    setNumThreads(4)
                }

                // ❌ REMOVE setUseNNAPI(true) - It's often unstable/slow for face models
            }
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            Log.e("FaceRecognizer", "Failed to init interpreter", e)
        }
    }

    /**
     * from gemini
     */

    // 1. Pre-allocate this in your class properties, NOT inside the function
    private val outputBuffer = Array(1) { FloatArray(EMBEDDING_SIZE) }
    fun getEmbedding(inputBitmap: Bitmap): FloatArray {

        // 2. Ensure the bitmap is the exact size the model expects
        val resizedBitmap = if (inputBitmap.width == INPUT_SIZE && inputBitmap.height == INPUT_SIZE) {
            inputBitmap
        } else {
            Bitmap.createScaledBitmap(inputBitmap, INPUT_SIZE, INPUT_SIZE, true)
        }

        // 3. Preprocess image into inputBuffer
        preprocessImage(resizedBitmap)

        // 4. Recycle only if we created a temporary scaled bitmap
        if (resizedBitmap != inputBitmap) resizedBitmap.recycle()

        // 5. TFLite inference using the pre-allocated outputBuffer
        interpreter?.run(inputBuffer, outputBuffer)

        // 6. Return a normalized COPY of the results
        // We use .clone() so the returned data doesn't change when the next frame runs
        return l2Normalize(outputBuffer[0].clone())
    }

    private fun preprocessImage(bitmap: Bitmap) {
        inputBuffer.rewind() // Start at the beginning
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixelValue in intValues) {
            // This is the fastest way to move data to TFLite
            inputBuffer.putFloat(((pixelValue shr 16 and 0xFF) - 127.5f) / 128f)
            inputBuffer.putFloat(((pixelValue shr 8 and 0xFF) - 127.5f) / 128f)
            inputBuffer.putFloat(((pixelValue and 0xFF) - 127.5f) / 128f)
        }
    }

    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sum = 0.0
        for (it in embedding) {
            sum += (it * it).toDouble()
        }
        val invL2 = 1.0 / Math.sqrt(sum)
        for (i in embedding.indices) {
            embedding[i] = (embedding[i] * invL2).toFloat()
        }
        return embedding

    }


    private fun loadModelFile(assets: android.content.res.AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
//
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}


