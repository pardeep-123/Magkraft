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