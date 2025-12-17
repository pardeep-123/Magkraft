package com.app.magkraft.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import androidx.core.graphics.scale
import androidx.core.graphics.get

object FaceRecognizer {

    private const val MODEL_NAME = "mobile_facenet.tflite"
    private const val INPUT_SIZE = 112
    private const val EMBEDDING_SIZE = 192

    private lateinit var interpreter: Interpreter

    fun initialize(context: Context) {
        if (::interpreter.isInitialized) return
        val model = loadModelFile(context, MODEL_NAME)
        interpreter = Interpreter(model)
    }

    fun getEmbedding(faceBitmap: Bitmap): FloatArray {

        // 1️⃣ Resize face
        val resized = Bitmap.createScaledBitmap(
            faceBitmap,
            INPUT_SIZE,
            INPUT_SIZE,
            true
        )

        // 2️⃣ Prepare input buffer
        val inputBuffer =
            ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.rewind()

        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val px = resized.getPixel(x, y)

                inputBuffer.putFloat((Color.red(px) - 127.5f) / 128f)
                inputBuffer.putFloat((Color.green(px) - 127.5f) / 128f)
                inputBuffer.putFloat((Color.blue(px) - 127.5f) / 128f)
            }
        }

        // 3️⃣ Model output: [1, 192]
        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }

        interpreter.run(inputBuffer, output)

        // 4️⃣ L2 NORMALIZATION (🔥 REQUIRED)
        return l2Normalize(output[0])
    }

    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sum = 0f
        for (v in embedding) {
            sum += v * v
        }
        val norm = kotlin.math.sqrt(sum)

        return FloatArray(embedding.size) { i ->
            embedding[i] / norm
        }
    }

    private fun loadModelFile(
        context: Context,
        modelName: String
    ): MappedByteBuffer {

        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }
}

