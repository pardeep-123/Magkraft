package com.app.magkraft.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.magkraft.R
import com.app.magkraft.utils.AuthPref

class SplashActivity : AppCompatActivity() {

    private lateinit var authPref: AuthPref

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        authPref = AuthPref(this)

        Handler(Looper.getMainLooper()).postDelayed({


            if (authPref.getRole("role") == "" || authPref.getRole("role") == "1") {

                // User already logged in
                startActivity(
                    Intent(this, AttendanceActivity::class.java)
                )

            } else {

                // User not logged in
                startActivity(
                    Intent(this, LoginActivity::class.java)
                )
            }

            finish()

        }, 1500) // splash delay
    }
}