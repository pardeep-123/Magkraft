package com.app.magkraft.network

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.google.gson.JsonElement

class ApiResponse(status: Status, data: JsonElement?, error: Throwable?) {
    lateinit var status: Status
    lateinit var data: JsonElement
    lateinit var error: Throwable

    companion object {

        fun loading(): ApiResponse {
            return ApiResponse(Status.LOADING, null, null)
        }

        fun success(data: JsonElement): ApiResponse {
            return ApiResponse(Status.SUCCESS, data, null)
        }

        fun error(@NonNull error: Throwable): ApiResponse {
            return ApiResponse(Status.ERROR, null, error)
        }
    }
}