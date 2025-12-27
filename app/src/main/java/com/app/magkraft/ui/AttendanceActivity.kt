package com.app.magkraft.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.app.magkraft.R
import com.app.magkraft.data.local.db.AppDatabase
import com.app.magkraft.data.local.db.AttendanceEntity
import com.app.magkraft.data.local.db.UserEntity
import com.app.magkraft.ml.AttendanceAnalyzer
import com.app.magkraft.ml.FaceMatcher
import com.app.magkraft.ml.FaceRecognizer
import com.app.magkraft.ml.UltraFastAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AttendanceActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var txtStatus: TextView
    private lateinit var txtReady: TextView
    private lateinit var txtName: TextView
    private lateinit var tickImage: ImageView
    private lateinit var btnRegister: Button
    private lateinit var imageAnalysis: ImageAnalysis


    private lateinit var users: List<UserEntity>

    private lateinit var cameraExecutor: ExecutorService

    private val mainScope = lifecycleScope

    private var cameraProvider: ProcessCameraProvider? = null
    private var matchShown = false
    private fun loadUsers() {
        lifecycleScope.launch {
            users = AppDatabase.getDatabase(this@AttendanceActivity)
                .userDao().getAllUsers()
        }
    }

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


    override fun onResume() {
        super.onResume()

        // ✅ Delay camera start - lets PreviewView fully recreate
        previewView.postDelayed({
            FaceRecognizer.initialize(this)
            loadUsers()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }, 100)  // 100ms delay fixes blank screen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_attendance)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        enableEdgeToEdge()
        setSupportActionBar(toolbar)
        // Set white overflow icon
        toolbar.overflowIcon =
            ContextCompat.getDrawable(this, R.drawable.more)?.apply {
                setTint(ContextCompat.getColor(this@AttendanceActivity, android.R.color.white))
            }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(
            this,
            R.color.mid_color
        )
        supportActionBar?.setDisplayShowTitleEnabled(false)
        previewView = findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        txtStatus = findViewById(R.id.txtStatus)
        txtReady = findViewById(R.id.txtReady)
        txtName = findViewById(R.id.txtName)
        tickImage = findViewById(R.id.tickImage)
        btnRegister = findViewById(R.id.btnRegister)
        resetUI()

        btnRegister.setOnClickListener {
            startActivity(
                Intent(this@AttendanceActivity, RegisterActivity::class.java)
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_switch_admin -> {
                startActivity(
                    Intent(this, LoginActivity::class.java)
                )
                true
            }

            else -> super.onOptionsItemSelected(item)
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


    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider  // Store reference

            // ✅ Prevent double binding
            provider.unbindAll()
            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetResolution(Size(640, 480))  // Smaller = faster
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(1)
                .build()
            //  CoroutineScope(Dispatchers.IO).launch {
//                val users = AppDatabase.getDatabase(this@AttendanceActivity)
//                    .userDao()
//                    .getAllUsers()

            // 🔥 ULTRAFAST - No ML Kit!
            imageAnalysis.setAnalyzer(
                cameraExecutor, UltraFastAnalyzer(
                    users = users,  // Load once
                    onMatch = { user ->
                        processFastMatch(user)  // Simplified
                    }
                ))
            //   }
//            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFastMatch(result: UserEntity) {
        mainScope.launch(Dispatchers.Main) {
            txtStatus.text = "Attendance marked for ${result.name}"
            txtReady.visibility = View.GONE
            txtName.text = "Designation: ${result.designation}"
            txtName.visibility = View.VISIBLE
            tickImage.visibility = View.VISIBLE
            saveAttendance(result.empId)

            // Auto-reset after 2s
            Handler(Looper.getMainLooper()).postDelayed({
                matchShown = false  // ✅ Reset - ready for new face
                resetUI()
            }, 3000)
        }
    }

    private fun resetUI() {
        txtStatus.text = "Align your Face In Oval"
        txtName.visibility = View.INVISIBLE
        txtReady.visibility = View.VISIBLE
        tickImage.visibility = View.INVISIBLE
        txtName.text = ""
    }

    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()  // ✅ Unbind BEFORE pause
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
    }
}

