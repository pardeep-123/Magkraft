package com.app.magkraft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
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

fun Context.isInternetAvailable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}


fun bitmapToBase64(bitmap: Bitmap): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

fun base64ToBitmap(base64: String): Bitmap? {
    val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}