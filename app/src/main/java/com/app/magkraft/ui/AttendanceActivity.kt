package com.app.magkraft.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.app.magkraft.R
import com.app.magkraft.data.local.db.AppDatabase
import com.app.magkraft.data.local.db.AttendanceEntity
import com.app.magkraft.ml.AttendanceAnalyzer
import com.app.magkraft.ml.FaceMatcher
import com.app.magkraft.ml.FaceRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class AttendanceActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var txtStatus: TextView
    private lateinit var btnRegister: Button
    private var isProcessing = false

    private var recognitionLocked = false
    private var lastResultTime = 0L

    private val RECOGNITION_COOLDOWN = 3000L // 3 seconds

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required for attendance",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)
        FaceRecognizer.initialize(this)
        previewView = findViewById(R.id.previewView)
        txtStatus = findViewById(R.id.txtStatus)
        btnRegister = findViewById(R.id.btnRegister)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnRegister.setOnClickListener {
            startActivity(
                Intent(this@AttendanceActivity, RegisterActivity::class.java)
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(
                Executors.newSingleThreadExecutor(),
                AttendanceAnalyzer(
                    onFaceDetected = { bitmap ->
                        if (!isProcessing && !recognitionLocked) {
                            isProcessing = true
                            processAttendance(bitmap)
                        }
                    }
                )
            )
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(this))
    }


    private fun processAttendance(faceBitmap: Bitmap) {
        recognitionLocked = true
        txtStatus.text = "Recognizing..."

        CoroutineScope(Dispatchers.IO).launch {

            val embedding = FaceRecognizer.getEmbedding(faceBitmap)
            val users = AppDatabase.getDatabase(this@AttendanceActivity)
                .userDao()
                .getAllUsers()

            val result = FaceMatcher.findBestMatch(embedding, users)

            withContext(Dispatchers.Main) {

                lastResultTime = System.currentTimeMillis()

                if (result != null) {
                    txtStatus.text = "Attendance marked for ${result.name}"
                    saveAttendance(result.empId)
                } else {
                    txtStatus.text = "Face not recognized"

                }
                isProcessing = false

                // Release lock after cooldown
                Handler(Looper.getMainLooper()).postDelayed({
                    recognitionLocked = false
                }, RECOGNITION_COOLDOWN)

            }
        }
    }

    private fun saveAttendance(empId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val attendance = AttendanceEntity(
                empId = empId,
                timestamp = System.currentTimeMillis()
            )

            AppDatabase.getDatabase(this@AttendanceActivity)
                .attendanceDao()
                .insertAttendance(attendance)
        }
    }

}

