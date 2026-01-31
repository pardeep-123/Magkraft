package com.app.magkraft.ui.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.MainActivity
import com.app.magkraft.R
import com.app.magkraft.data.local.db.UserEntity
import com.app.magkraft.model.CommonResponse
import com.app.magkraft.network.ApiClient
import com.app.magkraft.ui.RegisterActivity
import com.app.magkraft.ui.adapters.EmployeeAdapter
import com.app.magkraft.ui.adapters.EmployeePopupAdapter
import com.app.magkraft.ui.adapters.GroupPopupAdapter
import com.app.magkraft.ui.adapters.LocationPopupAdapter
import com.app.magkraft.ui.adapters.ViewReportsAdapter
import com.app.magkraft.ui.model.EmployeeListModel
import com.app.magkraft.ui.model.GroupListModel
import com.app.magkraft.ui.model.LocationListModel
import com.app.magkraft.ui.model.ViewReportsModelItem
import com.app.magkraft.utils.AuthPref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ManualAttendanceFragment : Fragment(R.layout.fragment_manual_attendance) {

    lateinit var etGroup: EditText
    lateinit var etEmployee: EditText
    lateinit var etLocation: EditText
//    lateinit var placeholder: TextView
    lateinit var btnSave: Button
//    lateinit var progressBar: ProgressBar

    private var groupList = ArrayList<GroupListModel>()
    private var employeeList = ArrayList<EmployeeListModel>()
    private var ctx: Context? = null

    private var groupId = ""
    private var employeeId = ""
    private var locationId = ""

    private var locationList = ArrayList<LocationListModel>()

    var authPref: AuthPref ?=null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etGroup = view.findViewById(R.id.etGroup)
        etEmployee = view.findViewById(R.id.etEmployee)
        etLocation = view.findViewById(R.id.etLocation)
        btnSave = view.findViewById(R.id.btnSave)
        authPref = AuthPref(ctx!!)
        getGroups()

        etGroup.setOnClickListener {
            showGroupPopup(etGroup, groupList) {

                etGroup.setText(it.Name)
                groupId = it.Id.toString()

                CoroutineScope(Dispatchers.Main).launch {
                    getLocationList(it.Id)
                }
            }
        }

        etLocation.setOnClickListener {
            if (groupId.isEmpty()) {
                Toast.makeText(ctx, "Select group First", Toast.LENGTH_SHORT).show()

            } else {
                showLocationPopup(etLocation, locationList) {

                    etLocation.setText(it.Name)
                    locationId = it.Id.toString()

                    CoroutineScope(Dispatchers.Main).launch {
                        getEmployeeList()
                    }
                }
            }
        }

        etEmployee.setOnClickListener {
            showEmployeePopup(etEmployee, employeeList) {

                etEmployee.setText(it.Name)
                employeeId = it.Id.toString()

            }
        }

        btnSave.setOnClickListener {

            if (etGroup.text.isNullOrEmpty() ||
                etEmployee.text.isNullOrEmpty() ||
                etLocation.text.isNullOrEmpty()
            ) {

                Toast.makeText(
                    requireContext(),
                    "Please select all fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }else{
                markAttendance()
            }
        }
    }

    private fun showGroupPopup(
        anchor: View, groups: List<GroupListModel>, onSelect: (GroupListModel) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.popup_group_list, null)
        val popup = PopupWindow(
            view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rv = view.findViewById<RecyclerView>(R.id.rvGroups)

        val adapter = GroupPopupAdapter(groups.toMutableList()) {
            onSelect(it)
            popup.dismiss()
        }

        rv.layoutManager = LinearLayoutManager(ctx)
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


    private fun showEmployeePopup(
        anchor: View, groups: List<EmployeeListModel>, onSelect: (EmployeeListModel) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.popup_group_list, null)
        val popup = PopupWindow(
            view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val rv = view.findViewById<RecyclerView>(R.id.rvGroups)

        val adapter = EmployeePopupAdapter(groups.toMutableList()) {
            onSelect(it)
            popup.dismiss()
        }

        rv.layoutManager = LinearLayoutManager(ctx)
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

    private fun getGroups() {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.getGroups()

        call.enqueue(object : Callback<List<GroupListModel>> {

            override fun onResponse(
                call: Call<List<GroupListModel>>, response: Response<List<GroupListModel>>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    groupList.clear()
                    groupList.addAll(response.body()!!)

                    /**
                     * Here we need to check , if group id is not 0 with user type 2, then
                     * set group id to that
                     */
                    if(authPref?.get("userType")=="2"){
                        if(authPref?.get("groupId")!="0"){
                            groupId = authPref?.get("groupId").toString()
                            etGroup.setText(groupList.firstOrNull{it.Id.toString()==groupId}?.Name?:"")
                            etGroup.isEnabled = false

                            getLocationList(groupId.toInt())

                        }
                    }
                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<GroupListModel>>, t: Throwable) {
//                (ctx as MainActivity).hideLoader()
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun getEmployeeList() {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.getEmployeesByGroupId1(groupId)

        call.enqueue(object : Callback<List<EmployeeListModel>> {

            override fun onResponse(
                call: Call<List<EmployeeListModel>>,
                response: Response<List<EmployeeListModel>>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    employeeList.clear()
                    employeeList.addAll(response.body()!!)


                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<EmployeeListModel>>, t: Throwable) {
                (ctx as MainActivity).hideLoader()
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getLocationList(groupId: Int) {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.getLocations()

        call.enqueue(object : Callback<List<LocationListModel>> {

            override fun onResponse(
                call: Call<List<LocationListModel>>,
                response: Response<List<LocationListModel>>
            ) {
                (ctx as MainActivity). hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    locationList.clear()

                    locationList.addAll(response.body()!!.filter { it.GroupId == groupId })


                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<LocationListModel>>, t: Throwable) {
                (ctx as MainActivity). hideLoader()
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
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

        rvLocations.layoutManager = LinearLayoutManager(ctx)
        rvLocations.adapter = adapter

        popup.elevation = 12f
        popup.showAsDropDown(anchor)
    }


    private fun markAttendance() {

        (ctx as MainActivity). showLoader()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val call = ApiClient.apiService.markAttendance(
            employeeId,
            locationId,
            now
        )

        call.enqueue(object : Callback<CommonResponse> {

            override fun onResponse(
                call: Call<CommonResponse>,
                response: Response<CommonResponse>
            ) {
                (ctx as MainActivity). hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        ctx,
                        "Attendance Marked Successfully",
                        Toast.LENGTH_SHORT).show()

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, HomeFragment())
                        .addToBackStack(null)
                        .commit()

                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
                    Toast.makeText(
                        ctx,
                        response.body()?.message.toString(),
                        Toast.LENGTH_SHORT
                    )
                        .show()

                }
            }

            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                (ctx as MainActivity).hideLoader()

                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }
}