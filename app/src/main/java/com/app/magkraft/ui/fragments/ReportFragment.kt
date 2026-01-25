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
import com.app.magkraft.network.ApiClient
import com.app.magkraft.ui.RegisterActivity
import com.app.magkraft.ui.adapters.EmployeeAdapter
import com.app.magkraft.ui.adapters.EmployeePopupAdapter
import com.app.magkraft.ui.adapters.GroupPopupAdapter
import com.app.magkraft.ui.adapters.ViewReportsAdapter
import com.app.magkraft.ui.model.EmployeeListModel
import com.app.magkraft.ui.model.GroupListModel
import com.app.magkraft.ui.model.LocationListModel
import com.app.magkraft.ui.model.ViewReportsModelItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

class ReportFragment : Fragment(R.layout.fragment_report) {

    lateinit var etGroup: EditText
    lateinit var etLocation: EditText
    lateinit var etEmployee: EditText
    lateinit var etMonth: EditText
    lateinit var placeholder: TextView
    lateinit var btnViewReport: Button
    lateinit var layoutEmpty: LinearLayout
    lateinit var progressBar: ProgressBar
    lateinit var rvReport: RecyclerView

    private var groupList = ArrayList<GroupListModel>()
    private var employeeList = ArrayList<EmployeeListModel>()
    private var employeeReportsList = ArrayList<ViewReportsModelItem>()
    private var ctx: Context? = null

    private var groupId = ""
    private var employeeId = ""
    private var month = ""
    private var year = ""
    private lateinit var adapter: ViewReportsAdapter


    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etGroup = view.findViewById(R.id.etGroup)
        etLocation = view.findViewById(R.id.etLocation)
        etEmployee = view.findViewById(R.id.etEmployee)
        etMonth = view.findViewById(R.id.etMonth)
        btnViewReport = view.findViewById(R.id.btnViewReport)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        progressBar = view.findViewById(R.id.progressBar)
        rvReport = view.findViewById(R.id.rvReport)
        placeholder = view.findViewById(R.id.placeholder)
//        setState(ReportState.EMPTY)

        getGroups()

        adapter = ViewReportsAdapter()

        rvReport.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ReportFragment.adapter
        }


        etMonth.setOnClickListener {
            showMonthYearPicker(requireContext()) {
                etMonth.setText(it)
            }
        }

        etGroup.setOnClickListener {
            showGroupPopup(etGroup, groupList) {

                etGroup.setText(it.Name)
                groupId = it.Id.toString()

                CoroutineScope(Dispatchers.Main).launch {
                    getEmployeeList()
                }
            }
        }

        etEmployee.setOnClickListener {
            showEmployeePopup(etEmployee, employeeList) {

                etEmployee.setText(it.Name)
                employeeId = it.Id.toString()

//                CoroutineScope(Dispatchers.Main).launch {
//                    getEmployeeList()
//                }
            }
        }

        btnViewReport.setOnClickListener {

            if (etGroup.text.isNullOrEmpty() ||
//                etLocation.text.isNullOrEmpty() ||
                etEmployee.text.isNullOrEmpty() ||
                etMonth.text.isNullOrEmpty()
            ) {

                Toast.makeText(
                    requireContext(),
                    "Please select all filters",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }else{
                getEmployeesReports()
            }



        }
    }

//    fun showMonthYearPicker(context: Context, onSelected: (String) -> Unit) {
//        val calendar = Calendar.getInstance()
//
//        val dialog = DatePickerDialog(
//            context,
//            { _, year, month, _ ->
//                val selected = "${month + 1}/$year"
//                onSelected(selected)
//            },
//            calendar.get(Calendar.YEAR),
//            calendar.get(Calendar.MONTH),
//            calendar.get(Calendar.DAY_OF_MONTH)
//        )
//
//        dialog.datePicker.findViewById<View>(
//            Resources.getSystem().getIdentifier("day", "id", "android")
//        )?.visibility = View.GONE
//
//        dialog.show()
//    }

    fun showMonthYearPicker(
        context: Context,
        onSelected: (String) -> Unit
    ) {
        val calendar = Calendar.getInstance()

        val dialog = DatePickerDialog(
            context,
            { _, year, month, _ ->
                this.month = (month + 1).toString()
                this.year = year.toString()
                val monthName = SimpleDateFormat(
                    "MMMM",
                    Locale.getDefault()
                ).format(
                    Calendar.getInstance().apply {
                        set(Calendar.MONTH, month)
                    }.time
                )

                onSelected("$monthName $year")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // ✅ Hide day picker safely
        try {
            val daySpinnerId =
                Resources.getSystem().getIdentifier("day", "id", "android")
            dialog.datePicker.findViewById<View>(daySpinnerId)?.visibility =
                View.GONE
        } catch (e: Exception) {
            // ignore (some devices)
        }

        dialog.show()
    }

    private fun setState(state: ReportState) {
        layoutEmpty.visibility = View.GONE
        progressBar.visibility = View.GONE
        rvReport.visibility = View.GONE

        when (state) {
            ReportState.EMPTY -> layoutEmpty.visibility = View.VISIBLE
            ReportState.LOADING -> progressBar.visibility = View.VISIBLE
            ReportState.DATA -> rvReport.visibility = View.VISIBLE
            ReportState.NO_DATA -> layoutEmpty.visibility = View.VISIBLE
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
//                    adapter.submitList(groupList)

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

    private fun getEmployeesReports() {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.viewReports(employeeId,month,year)

        call.enqueue(object : Callback<List<ViewReportsModelItem>> {

            override fun onResponse(
                call: Call<List<ViewReportsModelItem>>,
                response: Response<List<ViewReportsModelItem>>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    employeeReportsList.clear()
                    employeeReportsList.addAll(response.body()!!)
                    adapter.submitList(employeeReportsList)
                    placeholder.visibility = View.GONE
                } else {
                    placeholder.visibility = View.VISIBLE
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ViewReportsModelItem>>, t: Throwable) {
                (ctx as MainActivity).hideLoader()
                placeholder.visibility = View.VISIBLE
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    enum class ReportState {
        EMPTY,
        LOADING,
        DATA,
        NO_DATA
    }
}