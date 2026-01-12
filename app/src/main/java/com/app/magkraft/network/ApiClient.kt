package com.app.magkraft.network

import com.app.magkraft.utils.Constants
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient {

    companion object {

        val apiService: ApiCallInterface
            get() {
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.BODY
                val client = OkHttpClient.Builder().addInterceptor(interceptor).connectTimeout(10, TimeUnit.MINUTES)
                    .writeTimeout(10, TimeUnit.MINUTES)
                    .readTimeout(10, TimeUnit.MINUTES).build()
                val gson = GsonBuilder()
                    .setLenient()
                    .create()

                val retrofit =
                    Retrofit.Builder().baseUrl(Constants.BASEURL)
                        .addConverterFactory(GsonConverterFactory.create(gson))

//                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .client(client)
                        .build()

                return retrofit.create(ApiCallInterface::class.java)
            }
    }
}