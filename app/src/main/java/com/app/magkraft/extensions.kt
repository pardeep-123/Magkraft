package com.app.magkraft

import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun floatArrayToBase64(floatArray: FloatArray): String {
    val buffer = ByteBuffer.allocate(floatArray.size * 4)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    floatArray.forEach { buffer.putFloat(it) }
    return Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
}

fun base64ToFloatArray(base64: String): FloatArray {
    try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val floatArray = FloatArray(bytes.size / 4)
//    buffer.asFloatBuffer().get(floatArray)
        for (i in floatArray.indices) {
            floatArray[i] = buffer.getFloat()
        }
        return floatArray
    }catch (e: Exception) {
        Log.e("EncodingUtils", "Base64 conversion failed: ${e.message}")
        return floatArrayOf()
    }
}
fun base64ToByteArray(base64: String): ByteArray {
    return Base64.decode(base64, Base64.NO_WRAP)
}