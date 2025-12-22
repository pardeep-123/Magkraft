package com.app.magkraft.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.magkraft.R
import com.app.magkraft.data.local.db.AppDatabase
import com.app.magkraft.data.local.db.UserDao
import com.app.magkraft.data.local.db.UserEntity
import com.app.magkraft.ml.FaceOverlayView
import com.app.magkraft.ml.FaceRecognizer
import com.app.magkraft.ml.RegisterAnalyzer
import com.app.magkraft.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RegisterActivity : AppCompatActivity() {

    private var capturedFace: Bitmap? = null
    private lateinit var userDao: UserDao
    private lateinit var btn: Button
    private lateinit var btnSave: Button
    private lateinit var btnTakePhoto: Button
    private lateinit var etName: EditText
    private lateinit var etEmpId: EditText
    private lateinit var etGroup: EditText
    private lateinit var etDesignation: EditText
    private lateinit var ivFace: ImageView
    private var isImageCaptured = false

    private lateinit var previewContainer: View
    private lateinit var previewView: PreviewView

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var faceOverlay: FaceOverlayView

    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var cameraExecutor: ExecutorService


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetResolution(Size(640, 480))  // Faster
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(1)
                .build()

            // ✅ LIVE PREVIEW ANALYZER (shows face position)
            imageAnalysis.setAnalyzer(cameraExecutor, RegisterAnalyzer(
                onFaceReady = { faceBitmap ->
                    // Live preview only - NO saving
                    runOnUiThread {
                        ivFace.setImageBitmap(faceBitmap)
                    }
                }
            ))

            cameraProvider!!.unbindAll()
            cameraProvider!!.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(this))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        btn = findViewById(R.id.btnCapture)
        btnSave = findViewById(R.id.btnSave)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        etName = findViewById(R.id.etName)
        etEmpId = findViewById(R.id.etEmpId)
        etGroup = findViewById(R.id.etGroup)
        etDesignation = findViewById(R.id.etDesignation)
        ivFace = findViewById(R.id.ivFace)
        previewContainer = findViewById(R.id.previewContainer)
        previewView = findViewById(R.id.previewView)
        faceOverlay = findViewById(R.id.faceOverlay)
        userDao = AppDatabase.getDatabase(this).userDao()
// ✅ Initialize executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        btn.setOnClickListener {
            previewContainer.visibility = View.VISIBLE
            startCamera()
        }

        btnSave.setOnClickListener {
            saveUserAndStartScan()
        }

        btnTakePhoto.setOnClickListener {


            // ✅ Stop live preview
            imageAnalysis.clearAnalyzer()

            // ✅ Single shot capture using PreviewView
            capturePhoto()  // Your existing fast method
        }
    }

    private fun closeCamera() {
//        previewContainer.visibility = View.GONE
        cameraProvider?.unbindAll()
    }

    private fun saveUserAndStartScan() {

        val name = etName.text.toString()
        val empId = etEmpId.text.toString()
        val group = etGroup.text.toString()
        val etDesignation = etDesignation.text.toString()

        if (name.isEmpty() || empId.isEmpty() || group.isEmpty() || capturedFace == null) {
            Toast.makeText(this, "Fill all fields and capture face", Toast.LENGTH_LONG).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val embedding = FaceRecognizer.getEmbedding(capturedFace!!)
                val imageBytes = bitmapToByteArray(capturedFace!!)

                val user = UserEntity(
                    empId = empId,
                    name = name,
                    designation = etDesignation,
                    groupName = group,
                    embedding = embedding,
                    image = imageBytes
                )
                CoroutineScope(Dispatchers.IO).launch {
                    userDao.insertUser(user)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "User registered successfully",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }

    private fun capturePhoto() {
        val previewBitmap = previewView.bitmap ?: return
        val ovalRect = faceOverlay.getOvalRect()

        val faceBitmap = Bitmap.createBitmap(
            previewBitmap,
            ovalRect.left.toInt(),
            ovalRect.top.toInt(),
            ovalRect.width().toInt(),
            ovalRect.height().toInt()
        )

        // ✅ Safe recycle
        capturedFace?.recycle()
        capturedFace = faceBitmap
        ivFace.setImageBitmap(capturedFace)

        isImageCaptured = true
        btnSave.isEnabled = true  // Enable save button

        previewBitmap.recycle()
        previewContainer.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        capturedFace?.recycle()
        capturedFace = null

    }

}
