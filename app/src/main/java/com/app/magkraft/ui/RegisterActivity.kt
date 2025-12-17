package com.app.magkraft.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
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
import com.app.magkraft.ml.FaceCropper
import com.app.magkraft.ml.FaceOverlayView
import com.app.magkraft.ml.FaceRecognizer
import com.app.magkraft.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class RegisterActivity : AppCompatActivity() {

    private lateinit var capturedFace: Bitmap
    private lateinit var userDao: UserDao
    lateinit var btn: Button
    lateinit var btnSave: Button
    lateinit var btnTakePhoto: Button
    lateinit var etName: EditText
    lateinit var etEmpId: EditText
    lateinit var etGroup: EditText
    lateinit var etDesignation: EditText
    lateinit var ivFace: ImageView

    private var isImageCaptured = false
    private lateinit var imageCapture: ImageCapture

    private lateinit var previewContainer: View
    private lateinit var previewView: PreviewView

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var faceOverlay: FaceOverlayView


    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageCapture
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

        btn.setOnClickListener {
            previewContainer.visibility = View.VISIBLE
            startCamera()
        }

        btnSave.setOnClickListener {
            saveUserAndStartScan()
        }

        btnTakePhoto.setOnClickListener {
            capturePhoto()
        }


    }

    private fun closeCamera() {
        previewContainer.visibility = View.GONE
        cameraProvider?.unbindAll()
    }

    private fun saveUserAndStartScan() {

        val name = etName.text.toString()
        val empId = etEmpId.text.toString()
        val group = etGroup.text.toString()
        val etDesignation = etDesignation.text.toString()

        if (name.isEmpty() || empId.isEmpty() || group.isEmpty() || !::capturedFace.isInitialized) {
            Toast.makeText(this, "Fill all fields and capture face", Toast.LENGTH_LONG).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {

            val embedding = FaceRecognizer.getEmbedding(capturedFace)
            val imageBytes = bitmapToByteArray(capturedFace)

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
                Toast.makeText(this@RegisterActivity, "User registered successfully", Toast.LENGTH_LONG).show()

                finish()
            }
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }

//    private fun capturePhoto() {
//
//        imageCapture.takePicture(
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageCapturedCallback() {
//
//                override fun onCaptureSuccess(image: ImageProxy) {
//
//                    val bitmap = ImageUtils.imageProxyToBitmap(image)
//                    image.close()
//
//                    bitmap?.let { fullBitmap ->
//
//                        // ✅ USE EXISTING OVERLAY VIEW (VERY IMPORTANT)
//                        val ovalRect = faceOverlay.getOvalRect()
//
//                        val faceBitmap = FaceCropper.crop(
//                            fullBitmap,
//                            ovalRect,
//                            faceOverlay.width,
//                            faceOverlay.height
//                        )
//
//                        // ✅ SAVE & SHOW CROPPED FACE (NOT FULL IMAGE)
//                        capturedFace = faceBitmap
//                        ivFace.setImageBitmap(capturedFace)
//
//                        isImageCaptured = true
//                        closeCamera()
//                    }
//                }
//
//                override fun onError(exception: ImageCaptureException) {
//                    Toast.makeText(
//                        this@RegisterActivity,
//                        "Capture failed: ${exception.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        )
//    }


    private fun capturePhoto() {

        // 1️⃣ Get EXACT preview frame (same as overlay)
        val previewBitmap = previewView.bitmap ?: return

        // 2️⃣ Get overlay oval rect
        val ovalRect = faceOverlay.getOvalRect()

        // 3️⃣ Crop directly (NO scaling math needed)
        val faceBitmap = Bitmap.createBitmap(
            previewBitmap,
            ovalRect.left.toInt(),
            ovalRect.top.toInt(),
            ovalRect.width().toInt(),
            ovalRect.height().toInt()
        )

        // 4️⃣ Show cropped face ONLY
        capturedFace = faceBitmap
        ivFace.setImageBitmap(faceBitmap)

        // 5️⃣ Stop camera
        closeCamera()
    }


}
