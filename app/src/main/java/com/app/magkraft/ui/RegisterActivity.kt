package com.app.magkraft.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.MainActivity
import com.app.magkraft.R
import com.app.magkraft.data.local.db.AppDatabase
import com.app.magkraft.data.local.db.UserDao
import com.app.magkraft.data.local.db.UserEntity
import com.app.magkraft.floatArrayToBase64
import com.app.magkraft.ml.FaceOverlayView
import com.app.magkraft.ml.FaceRecognizer
import com.app.magkraft.ml.RegisterAnalyzer
import com.app.magkraft.model.AddGroupModel
import com.app.magkraft.model.CommonResponse
import com.app.magkraft.network.ApiClient
import com.app.magkraft.ui.adapters.GroupPopupAdapter
import com.app.magkraft.ui.adapters.LocationPopupAdapter
import com.app.magkraft.ui.model.EmployeeListModel
import com.app.magkraft.ui.model.GroupListModel
import com.app.magkraft.ui.model.LocationListModel
import com.app.magkraft.utils.AuthPref
import com.app.magkraft.utils.EmployeeViewModel
import com.app.magkraft.utils.ImageUtils
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.getValue

class RegisterActivity : BaseActivity() {

    private var capturedFace: Bitmap? = null
    private lateinit var userDao: UserDao
    private lateinit var btn: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnTakePhoto: Button
    private lateinit var etName: EditText
    private lateinit var etEmpId: EditText
    private lateinit var etGroup: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDesignation: EditText
    private lateinit var ivFace: ImageView
    private var isImageCaptured = false

    private lateinit var previewContainer: View
    private lateinit var previewView: PreviewView

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var faceOverlay: FaceOverlayView

    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var cameraExecutor: ExecutorService
    private var groupId = ""
    private var locationId = ""

    private var groupList = ArrayList<GroupListModel>()
    private var locationList = ArrayList<LocationListModel>()

    private var employeeData: EmployeeListModel? = null

    var embeddingBase64 = ""
    var employeeId = ""

    var authPref: AuthPref ?=null
    private val employeeViewModel: EmployeeViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    }
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
            imageAnalysis.setAnalyzer(
                cameraExecutor, RegisterAnalyzer(
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

    private fun setStatusBarColor(window: Window, statusBarBgView: View) {
        statusBarBgView.setBackgroundResource(R.drawable.maroon_black_gradient_bg)
        ViewCompat.setOnApplyWindowInsetsListener(statusBarBgView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams {
                height = systemBarsInsets.top
            }
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = false
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        btn = findViewById(R.id.btnEdit)
        btnSave = findViewById(R.id.btnSave)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        etName = findViewById(R.id.etName)
        etEmpId = findViewById(R.id.etEmployeeId)
        etGroup = findViewById(R.id.etGroup)
        etLocation = findViewById(R.id.etLocation)
        etDesignation = findViewById(R.id.etDesignation)
        ivFace = findViewById(R.id.ivFace)
        previewContainer = findViewById(R.id.previewContainer)
        previewView = findViewById(R.id.previewView)
        faceOverlay = findViewById(R.id.faceOverlay)
        authPref = AuthPref(this)
        /**
         * Set values in case of user edit
         */

        employeeData = intent.getParcelableExtra("data")
        employeeData?.let {
            etName.setText(it.Name)
            etDesignation.setText(it.Designation)
            etEmpId.setText(it.Code.toString())
            findViewById<SwitchMaterial>(R.id.switchActive).isChecked = it.IsActive
            etGroup.setText(it.GroupName)
            etLocation.setText(it.LocationName)
            groupId = it.GroupId.toString()
            locationId = it.LocationId.toString()
            embeddingBase64 = it.Photo
            employeeId = it.Id.toString()

        }

//        statusBarBackgroundView = findViewById(R.id.status_bar_background_view)
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

//            setStatusBarColor(window, statusBarBackgroundView)
        }
        etGroup.setOnClickListener {
            showGroupPopup(etGroup, groupList) {

                etGroup.setText(it.Name)
                groupId = it.Id.toString()

                CoroutineScope(Dispatchers.Main).launch {
                    getLocationList(groupId.toInt())
                }
            }
        }

        etLocation.setOnClickListener {
            if (groupId.isEmpty()) {
                Toast.makeText(this, "Select group First", Toast.LENGTH_SHORT).show()

            } else {
                showLocationPopup(etLocation, locationList) {

                    etLocation.setText(it.Name)
                    locationId = it.Id.toString()
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            getGroups()
        }


    }

    private fun closeCamera() {
//        previewContainer.visibility = View.GONE
        cameraProvider?.unbindAll()
    }

    private fun saveUserAndStartScan() {

        val name = etName.text.toString()
        val empId = etEmpId.text.toString()

        val etDesignation = etDesignation.text.toString()

        if (name.isEmpty() || empId.isEmpty() || etDesignation.isEmpty() || groupId == "" || locationId == "" || (employeeId.isEmpty() && capturedFace == null)) {
            Toast.makeText(this, "Fill all fields and capture face", Toast.LENGTH_LONG).show()
            return
        } else {

        //    lifecycleScope.launch {

                try {
                    if (employeeId.isEmpty()) {
//                        val embedding =
//                            withContext(Dispatchers.Default) {
//                                FaceRecognizer.getEmbedding(capturedFace!!)
//                            }
////                val imageBytes = bitmapToByteArray(capturedFace!!)
//                        embeddingBase64 =
//                            withContext(Dispatchers.Default) {
//                                floatArrayToBase64(embedding)
//                            }

                        addEmployee(embeddingBase64,1)
                    } else {
                        addEmployee(embeddingBase64,2)
                    }

//                val user = UserEntity (
//                    empId = empId,
//                    name = name,
//                    designation = etDesignation,
//                    groupName = group,
//                    embedding = embedding,
//                    image = imageBytes
//                )
//
//                Log.d("embeddings", embedding.toString())
//                CoroutineScope(Dispatchers.IO).launch {
//                    userDao.insertUser(user)
//                }
//
//
//
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@RegisterActivity,
//                        "User registered successfully",
//                        Toast.LENGTH_LONG
//                    ).show()
//
//                    finish()
//                }

                } catch (e: Exception) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                }
          //  }

        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }

    private fun capturePhoto() {
        val previewBitmap = previewView.bitmap ?: return
        val correctedBitmap = ImageUtils.getCorrectedBitmap(previewBitmap, 0, isFrontCamera = true)
        val ovalRect = faceOverlay.getOvalRect()

        val faceBitmap = Bitmap.createBitmap(
            correctedBitmap,
            ovalRect.left.toInt().coerceAtLeast(0),
            ovalRect.top.toInt().coerceAtLeast(0),
            ovalRect.width().toInt().coerceAtMost(correctedBitmap.width),
            ovalRect.height().toInt().coerceAtMost(correctedBitmap.height)
        )

        // ✅ Safe recycle
        capturedFace?.recycle()
        capturedFace = faceBitmap
        ivFace.setImageBitmap(capturedFace)

        isImageCaptured = true
        btnSave.isEnabled = true  // Enable save button

        lifecycleScope.launch {
            val embedding =
                withContext(Dispatchers.Default) {
                    FaceRecognizer.getInstance().getEmbedding(faceBitmap)
                }
//                val imageBytes = bitmapToByteArray(capturedFace!!)
            embeddingBase64 =
                withContext(Dispatchers.Default) {
                    floatArrayToBase64(embedding)
                }
        }
        previewBitmap.recycle()
        previewContainer.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        capturedFace?.recycle()
        capturedFace = null

    }


    private fun addEmployee(image: String, from: Int) {

        showLoader()
        val status = if (findViewById<SwitchMaterial>(R.id.switchActive).isChecked) {
            "1"
        } else {
            "0"
        }
        val call = if (from == 1) {
            ApiClient.apiService.register(
                etName.text.toString().trim(),
                etEmpId.text.toString().trim(),
                etDesignation.text.toString().trim(),
                groupId,
                locationId,
                status,
                "0",
                image

            )
        } else {
            ApiClient.apiService.updateEmployee(
                etName.text.toString().trim(),
                etEmpId.text.toString().trim(),
                etDesignation.text.toString().trim(),
                groupId,
                locationId,
                status,
                "0",
                image,
                employeeId

            )
        }


        call.enqueue(object : Callback<CommonResponse> {

            override fun onResponse(
                call: Call<CommonResponse>,
                response: Response<CommonResponse>
            ) {
                hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    Log.d("response is", response.body().toString())
                    Toast.makeText(
                        this@RegisterActivity,
                        "User Added Successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    if(authPref?.getLocation("groupId")!="") {
//            getEmployeesListByGroup()
//        }
//                        lifecycleScope.launch(Dispatchers.IO) {
                            employeeViewModel.syncEmployees()
                      //  }
                    }

                    finish()

                } else {
                    val errorMessage = getErrorMessage(response)
                    Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                hideLoader()
                Toast.makeText(this@RegisterActivity, t.localizedMessage, Toast.LENGTH_SHORT).show()
                Log.d("error is", t.localizedMessage.toString())
            }
        })
    }

    private fun getGroups() {

//        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.getGroups()

        call.enqueue(object : Callback<List<GroupListModel>> {

            override fun onResponse(
                call: Call<List<GroupListModel>>,
                response: Response<List<GroupListModel>>
            ) {
//                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    groupList.clear()
                    groupList.addAll(response.body()!!)
//                    adapter.submitList(groupList)

                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<GroupListModel>>, t: Throwable) {
//                (ctx as MainActivity).hideLoader()
                Toast.makeText(this@RegisterActivity, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getLocationList(groupId: Int) {

        showLoader()

        val call = ApiClient.apiService.getLocations()

        call.enqueue(object : Callback<List<LocationListModel>> {

            override fun onResponse(
                call: Call<List<LocationListModel>>,
                response: Response<List<LocationListModel>>
            ) {
                hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    locationList.clear()

                    locationList.addAll(response.body()!!.filter { it.GroupId == groupId })


                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<LocationListModel>>, t: Throwable) {
                hideLoader()
                Toast.makeText(this@RegisterActivity, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showGroupPopup(
        anchor: View,
        groups: List<GroupListModel>,
        onSelect: (GroupListModel) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.popup_group_list, null)
        val popup = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rv = view.findViewById<RecyclerView>(R.id.rvGroups)

        val adapter = GroupPopupAdapter(groups.toMutableList()) {
            onSelect(it)
            popup.dismiss()
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        etSearch.addTextChangedListener { it ->
//            val filtered = groups.filter { g ->
//                g.name.contains(it.toString(), true)
//            }
//            adapter.update(filtered)
        }

        popup.elevation = 12f
        popup.showAsDropDown(anchor)
    }

    private fun showLocationPopup(
        anchor: View,
        locations: List<LocationListModel>,
        onSelect: (LocationListModel) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.popup_location_list, null)
        val popup = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rvLocations = view.findViewById<RecyclerView>(R.id.rvLocations)

        val adapter = LocationPopupAdapter(locations.toMutableList()) {
            onSelect(it)
            popup.dismiss()
        }

        rvLocations.layoutManager = LinearLayoutManager(this)
        rvLocations.adapter = adapter

        etSearch.addTextChangedListener { it ->
//            val filtered = groups.filter { g ->
//                g.name.contains(it.toString(), true)
//            }
//            adapter.update(filtered)
        }

        popup.elevation = 12f
        popup.showAsDropDown(anchor)
    }





}
