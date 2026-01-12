package com.app.magkraft.network


import com.app.magkraft.model.LoginSignupModel
import com.app.magkraft.utils.Constants
import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.http.*


interface ApiCallInterface {
    /*
   Login API
   */
    @FormUrlEncoded
    @POST(Constants.login)
    fun loginUser(
        @Field("user") email: String,
        @Field("pwd") password: String,
    ): Call<LoginSignupModel>

}