package com.app.magkraft

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun floatArrayToBase64(floatArray: FloatArray): String {
    val buffer = ByteBuffer.allocate(floatArray.size * 4)
    buffer.order(ByteOrder.nativeOrder())
    floatArray.forEach { buffer.putFloat(it) }
    return Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
}

fun base64ToFloatArray(base64: String): FloatArray {
    val bytes = Base64.decode(base64, Base64.NO_WRAP)
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
    val floatArray = FloatArray(bytes.size / 4)
    buffer.asFloatBuffer().get(floatArray)
    return floatArray
}
fun base64ToByteArray(base64: String): ByteArray {
    return Base64.decode(base64, Base64.NO_WRAP)
}