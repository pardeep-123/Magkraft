package com.app.magkraft.utils

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
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
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )
        // Remove FaceRecognizer.initialize() from here
    }
}
