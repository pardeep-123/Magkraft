package com.app.magkraft.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.activity.viewModels
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.magkraft.MainActivity
import com.app.magkraft.R
import com.app.magkraft.data.local.db.AppDatabase
import com.app.magkraft.data.local.db.AttendanceEntity
import com.app.magkraft.data.local.db.UserEntity

import com.app.magkraft.ml.FaceRecognizer
import com.app.magkraft.ml.UltraFastAnalyzer
import com.app.magkraft.model.CommonResponse
import com.app.magkraft.network.ApiClient
import com.app.magkraft.ui.model.EmployeeListModel
import com.app.magkraft.utils.AuthPref
import com.app.magkraft.utils.EmployeeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AttendanceActivity : BaseActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var txtStatus: TextView
    private lateinit var txtReady: TextView
    private lateinit var txtName: TextView
    private lateinit var groupName: TextView
    private lateinit var locationName: TextView
    private lateinit var tickImage: ImageView
    private lateinit var btnRegister: Button
    private lateinit var imageAnalysis: ImageAnalysis


    //    private lateinit var users: List<UserEntity>
    private var users: List<UserEntity> = emptyList()
    private var isCameraStarted = false
    private lateinit var cameraExecutor: ExecutorService

    private val mainScope = lifecycleScope

    private var cameraProvider: ProcessCameraProvider? = null
    private var matchShown = false

    var authPref: AuthPref? = null

    private var employeeList = mutableListOf<EmployeeListModel>()

    private val employeeViewModel: EmployeeViewModel by viewModels()


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

    override fun onStop() {
        super.onStop()
        // Reset the flag so that when the user returns, startCamera() can trigger
        isCameraStarted = false
    }

    override fun onResume() {
        super.onResume()

        // ✅ Delay camera start - lets PreviewView fully recreate
//        previewView.postDelayed({
////            FaceRecognizer.initialize(this)
//           // loadUsers()
//
//            if (ContextCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.CAMERA
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                startCamera()
//            } else {
//                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
//            }
//        }, 100)  // 100ms delay fixes blank screen

// 1. Just check permissions.
        // If granted, the Flow observer in onCreate will naturally trigger startCamera()
        // as soon as the database emits the user list.
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        if (authPref?.getLocation("groupId") != "") {
            groupName.text = "Group: " + authPref?.getLocation("groupName")
            locationName.text = "Location: " + authPref?.getLocation("locationName")
        } else {
            groupName.text = "Please Set Location to Scan"
        }
        if (authPref?.getLocation("groupId") != "") {
//            getEmployeesListByGroup()
//        }
//            lifecycleScope.launch(Dispatchers.IO) {
            employeeViewModel.syncEmployees()
            //  }
        }
        // 2️⃣ Load employees AFTER sync
//        lifecycleScope.launch(Dispatchers.IO) {
//            users = employeeViewModel.getEmployees()
//
//            withContext(Dispatchers.Main) {
//                if (users.isNotEmpty()) {
//                    startCamera()   // ✅ camera starts only when users are ready
//                }
//            }
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_attendance)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        enableEdgeToEdge()
        setSupportActionBar(toolbar)
        authPref = AuthPref(this)
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
        groupName = findViewById(R.id.groupName)
        locationName = findViewById(R.id.locationName)
        tickImage = findViewById(R.id.tickImage)
        btnRegister = findViewById(R.id.btnRegister)
        resetUI()

        btnRegister.setOnClickListener {
            startActivity(
                Intent(this@AttendanceActivity, RegisterActivity::class.java)
            )
        }

        // Observe the employees list reactively
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                employeeViewModel.allEmployees.collect { updatedList ->
                    users = updatedList
                    if (users.isNotEmpty() && !isCameraStarted) {

                        startCamera() // Only starts when data is actually there
                        txtStatus.text = "Align your Face In Oval"
                    } else if (users.isEmpty()) {
                        txtStatus.text = "Syncing Employees..."
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_switch_admin -> {
                if (authPref?.isLoggedIn() == true) {
                    startActivity(
                        Intent(this@AttendanceActivity, MainActivity::class.java)
                    )
                } else {
                    startActivity(
                        Intent(this, LoginActivity::class.java)
                    )
                }
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
        // Only proceed if the view is attached and we have permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        previewView.post { //
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(320, 240)) // Low res = High speed
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(
                    cameraExecutor, UltraFastAnalyzer(
                        users = users,
                        onMatch = { user -> processFastMatch(user) }
                    ))

                try {
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis
                    )
                    isCameraStarted = true
                } catch (e: Exception) {
                    Log.e("CameraX", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }

    //    private fun startCamera() {
//
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            val provider = cameraProviderFuture.get()
//            cameraProvider = provider  // Store reference
//
//            // ✅ Prevent double binding
//            provider.unbindAll()
//            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
//
//            val preview = Preview.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .build()
//                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
//
//            imageAnalysis = ImageAnalysis.Builder()
////                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetResolution(Size(320, 240))  // Smaller = faster
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
////                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
//
//                .setImageQueueDepth(1)
//                .build()
//            //  CoroutineScope(Dispatchers.IO).launch {
////                val users = AppDatabase.getDatabase(this@AttendanceActivity)
////                    .userDao()
////                    .getAllUsers()
//
//            // 🔥 ULTRAFAST - No ML Kit!
//            imageAnalysis.setAnalyzer(
//                cameraExecutor, UltraFastAnalyzer(
//                    users = users,  // Load once
//                    onMatch = { user ->
//                        processFastMatch(user)  // Simplified
//                    }
//                ))
//            //   }
////            cameraProvider?.unbindAll()
//            cameraProvider?.bindToLifecycle(
//                this,
//                CameraSelector.DEFAULT_FRONT_CAMERA,
//                preview,
//                imageAnalysis
//            )
//        }, ContextCompat.getMainExecutor(this))
//    }
    private val ATTENDANCE_COOLDOWN = 5 * 60 * 1000L // 5 minutes
    private fun processFastMatch(result: UserEntity) {
        // 3. Prevent multiple scans of the same person (Debounce)
        if (matchShown) return
        matchShown = true

        if (authPref?.getLocation("groupId") != "") {
//            mainScope.launch(Dispatchers.Main) {
//                markAttendance(result)
//
//            }
            lifecycleScope.launch(Dispatchers.IO) {

                val dao = AppDatabase
                    .getDatabase(this@AttendanceActivity)
                    .attendanceDao()

                val lastTime = dao.getLastAttendanceTime(result.empId)
                val now = System.currentTimeMillis()

                // 🚫 Block if within 5 minutes
                if (lastTime != null && now - lastTime < ATTENDANCE_COOLDOWN) {
                    withContext(Dispatchers.Main) {
                        txtStatus.text = "Already marked. Please wait 5 minutes."
                        tickImage.visibility = View.INVISIBLE
                        delayAndReset(2000)
                    }
                    // Auto-reset after 2s
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        matchShown = false  // ✅ Reset - ready for new face
//                        resetUI()
//                    }, 3000)
                    return@launch
                }

                // ✅ Allow attendance
                withContext(Dispatchers.Main) {
                    markAttendance(result)
                }
            }
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
        cameraExecutor.shutdown()
        FaceRecognizer.getInstance().close()
    }


    private fun markAttendance(empId: UserEntity) {

        showLoader()
        val now = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val call = ApiClient.apiService.markAttendance(
            empId.empId,
            authPref?.getLocation("locationId").toString(),
            now
        )

        call.enqueue(object : Callback<CommonResponse> {

            override fun onResponse(
                call: Call<CommonResponse>,
                response: Response<CommonResponse>
            ) {
                hideLoader()

                if (response.isSuccessful && response.body() != null) {

                    txtStatus.text = "Attendance marked for ${empId.name}"
                    txtReady.visibility = View.GONE
                    txtName.text = "Designation: ${empId.designation}"
                    txtName.visibility = View.VISIBLE
                    tickImage.visibility = View.VISIBLE
                    saveAttendance(empId.empId)
                    delayAndReset(3000)
                    // Auto-reset after 2s
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        matchShown = false  // ✅ Reset - ready for new face
//                        resetUI()
//                    }, 3000)
                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
                    Toast.makeText(
                        this@AttendanceActivity,
                        response.body()?.message.toString(),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    matchShown = false
                }
            }

            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                hideLoader()
                matchShown = false
                Toast.makeText(this@AttendanceActivity, t.localizedMessage, Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun delayAndReset(ms: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            matchShown = false
            resetUI()
        }, ms)
    }


}


//package com.app.magkraft.ui
//
//import android.Manifest
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Size
//import android.view.Menu
//import android.view.MenuItem
//import android.view.View
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.app.AppCompatDelegate
//import androidx.appcompat.widget.Toolbar
//import androidx.camera.core.AspectRatio
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.core.content.ContextCompat
//import androidx.core.view.WindowCompat
//import androidx.lifecycle.lifecycleScope
//import com.app.magkraft.MainActivity
//import com.app.magkraft.R
//import com.app.magkraft.data.local.db.AppDatabase
//import com.app.magkraft.data.local.db.AttendanceEntity
//import com.app.magkraft.data.local.db.UserEntity
//import com.app.magkraft.ml.AttendanceAnalyzer
//import com.app.magkraft.ml.FaceMatcher
//import com.app.magkraft.ml.FaceRecognizer
//import com.app.magkraft.ml.UltraFastAnalyzer
//import com.app.magkraft.utils.AuthPref
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//
//class AttendanceActivity : AppCompatActivity() {
//
//    private lateinit var previewView: PreviewView
//    private lateinit var txtStatus: TextView
//    private lateinit var txtReady: TextView
//    private lateinit var txtName: TextView
//    private lateinit var groupName: TextView
//    private lateinit var locationName: TextView
//    private lateinit var tickImage: ImageView
//    private lateinit var btnRegister: Button
//    private lateinit var imageAnalysis: ImageAnalysis
//
//
//    private lateinit var users: List<UserEntity>
//
//    private lateinit var cameraExecutor: ExecutorService
//
//    private val mainScope = lifecycleScope
//
//    private var cameraProvider: ProcessCameraProvider? = null
//    private var matchShown = false
//
//    var authPref: AuthPref ?=null
//
//    private fun loadUsers() {
//        lifecycleScope.launch {
//            users = AppDatabase.getDatabase(this@AttendanceActivity)
//                .userDao().getAllUsers()
//        }
//    }
//
//    private val cameraPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//            if (granted) {
//                startCamera()
//            } else {
//                Toast.makeText(
//                    this,
//                    "Camera permission is required for attendance",
//                    Toast.LENGTH_LONG
//                ).show()
//                finish()
//            }
//        }
//
//
//    override fun onResume() {
//        super.onResume()
//
//        // ✅ Delay camera start - lets PreviewView fully recreate
//        previewView.postDelayed({
//            FaceRecognizer.initialize(this)
//            loadUsers()
//
//            if (ContextCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.CAMERA
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                startCamera()
//            } else {
//                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
//            }
//        }, 100)  // 100ms delay fixes blank screen
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        setContentView(R.layout.activity_attendance)
//        val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        enableEdgeToEdge()
//        setSupportActionBar(toolbar)
//        authPref = AuthPref(this)
//        // Set white overflow icon
//        toolbar.overflowIcon =
//            ContextCompat.getDrawable(this, R.drawable.more)?.apply {
//                setTint(ContextCompat.getColor(this@AttendanceActivity, android.R.color.white))
//            }
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//        window.statusBarColor = ContextCompat.getColor(
//            this,
//            R.color.mid_color
//        )
//        supportActionBar?.setDisplayShowTitleEnabled(false)
//        previewView = findViewById(R.id.previewView)
//        cameraExecutor = Executors.newSingleThreadExecutor()
//        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
//
//        txtStatus = findViewById(R.id.txtStatus)
//        txtReady = findViewById(R.id.txtReady)
//        txtName = findViewById(R.id.txtName)
//        groupName = findViewById(R.id.groupName)
//        locationName = findViewById(R.id.locationName)
//        tickImage = findViewById(R.id.tickImage)
//        btnRegister = findViewById(R.id.btnRegister)
//        resetUI()
//
//        btnRegister.setOnClickListener {
//            startActivity(
//                Intent(this@AttendanceActivity, RegisterActivity::class.java)
//            )
//        }
//
//        if(authPref?.getLocation("locationId")!=null){
//            groupName.text = "Group: "+authPref?.getLocation("groupName")
//            locationName.text ="Location: "+ authPref?.getLocation("locationName")
//        }else{
//            groupName.text = " Please Set Location to Scan"
//        }
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_toolbar, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//
//            R.id.action_switch_admin -> {
//                if (authPref?.isLoggedIn() == true){
//                    startActivity(
//                        Intent(this@AttendanceActivity, MainActivity::class.java)
//                    )
//                }else {
//                    startActivity(
//                        Intent(this, LoginActivity::class.java)
//                    )
//                }
//                true
//            }
//
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    private fun saveAttendance(empId: String) {
//        CoroutineScope(Dispatchers.IO).launch {
//            val attendance = AttendanceEntity(
//                empId = empId,
//                timestamp = System.currentTimeMillis()
//            )
//
//            AppDatabase.getDatabase(this@AttendanceActivity)
//                .attendanceDao()
//                .insertAttendance(attendance)
//        }
//    }
//
//
//    private fun startCamera() {
//
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            val provider = cameraProviderFuture.get()
//            cameraProvider = provider  // Store reference
//
//            // ✅ Prevent double binding
//            provider.unbindAll()
//            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
//
//            val preview = Preview.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .build()
//                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
//
//            imageAnalysis = ImageAnalysis.Builder()
//                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
////                .setTargetResolution(Size(640, 480))  // Smaller = faster
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .setImageQueueDepth(1)
//                .build()
//            //  CoroutineScope(Dispatchers.IO).launch {
////                val users = AppDatabase.getDatabase(this@AttendanceActivity)
////                    .userDao()
////                    .getAllUsers()
//
//            // 🔥 ULTRAFAST - No ML Kit!
//            imageAnalysis.setAnalyzer(
//                cameraExecutor, UltraFastAnalyzer(
//                    users = users,  // Load once
//                    onMatch = { user ->
//                        processFastMatch(user)  // Simplified
//                    }
//                ))
//            //   }
////            cameraProvider?.unbindAll()
//            cameraProvider?.bindToLifecycle(
//                this,
//                CameraSelector.DEFAULT_FRONT_CAMERA,
//                preview,
//                imageAnalysis
//            )
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    private fun processFastMatch(result: UserEntity) {
//        mainScope.launch(Dispatchers.Main) {
//            txtStatus.text = "Attendance marked for ${result.name}"
//            txtReady.visibility = View.GONE
//            txtName.text = "Designation: ${result.designation}"
//            txtName.visibility = View.VISIBLE
//            tickImage.visibility = View.VISIBLE
//            saveAttendance(result.empId)
//
//            // Auto-reset after 2s
//            Handler(Looper.getMainLooper()).postDelayed({
//                matchShown = false  // ✅ Reset - ready for new face
//                resetUI()
//            }, 3000)
//        }
//    }
//
//    private fun resetUI() {
//        txtStatus.text = "Align your Face In Oval"
//        txtName.visibility = View.INVISIBLE
//        txtReady.visibility = View.VISIBLE
//        tickImage.visibility = View.INVISIBLE
//        txtName.text = ""
//    }
//
//    override fun onPause() {
//        super.onPause()
//        cameraProvider?.unbindAll()  // ✅ Unbind BEFORE pause
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        cameraProvider?.unbindAll()
//    }
//}

