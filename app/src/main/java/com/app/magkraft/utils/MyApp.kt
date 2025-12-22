package com.app.magkraft.utils

import android.app.Application
import com.app.magkraft.ml.FaceRecognizer

// In Application class
class MyApp : Application() {

    companion object {
        lateinit var instance: MyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Remove FaceRecognizer.initialize() from here
    }
}
