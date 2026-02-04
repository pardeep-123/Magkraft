package com.app.magkraft.ui.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.MainActivity
import com.app.magkraft.R
import com.app.magkraft.network.ApiClient
import com.app.magkraft.ui.adapters.EmployeePopupAdapter
import com.app.magkraft.ui.adapters.GroupPopupAdapter
import com.app.magkraft.ui.adapters.ViewReportsAdapter
import com.app.magkraft.ui.model.EmployeeListModel
import com.app.magkraft.ui.model.GroupListModel
import com.app.magkraft.ui.model.ViewReportsModelItem
import com.app.magkraft.utils.AuthPref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.OutputStreamWriter
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

class ReportFragment : Fragment(R.layout.fragment_report) {

    lateinit var etGroup: EditText
    lateinit var etLocation: EditText
    lateinit var etEmployee: EditText
    lateinit var etMonth: EditText
    lateinit var placeholder: TextView
    lateinit var btnViewReport: Button
    lateinit var btnDownloadReport: Button
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

    var authPref: AuthPref? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etGroup = view.findViewById(R.id.etGroup)
        etLocation = view.findViewById(R.id.etLocation)
        etEmployee = view.findViewById(R.id.etEmployee)
        etMonth = view.findViewById(R.id.etMonth)
        btnViewReport = view.findViewById(R.id.btnViewReport)
        layoutEmpty = view.findViewById(R.id.nameLayout)
        progressBar = view.findViewById(R.id.progressBar)
        rvReport = view.findViewById(R.id.rvReport)
        placeholder = view.findViewById(R.id.placeholder)
        btnDownloadReport = view.findViewById(R.id.downloadReport)
        authPref = AuthPref(ctx!!)
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
            } else {
                getEmployeesReports()
            }

        }

        btnDownloadReport.setOnClickListener {
            downloadCsv(employeeReportsList)
        }
    }

    fun showMonthYearPicker(
        context: Context,
        onSelected: (String) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_month_year_picker)

        val monthPicker = dialog.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = dialog.findViewById<NumberPicker>(R.id.yearPicker)
        val btnOk = dialog.findViewById<Button>(R.id.btnOk)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        val months = DateFormatSymbols().months.take(12).toTypedArray()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months
        monthPicker.value = Calendar.getInstance().get(Calendar.MONTH)

        yearPicker.minValue = currentYear - 50
        yearPicker.maxValue = currentYear + 10
        yearPicker.value = currentYear

        btnOk.setOnClickListener {
            month = (months.indexOf(monthPicker.displayedValues[monthPicker.value]) + 1).toString()
                year = yearPicker.value.toString()
            onSelected("${months[monthPicker.value]} ${yearPicker.value}")
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

//    fun showMonthYearPicker(
//        context: Context,
//        onSelected: (String) -> Unit
//    ) {
//        val calendar = Calendar.getInstance()
//
//        val dialog = DatePickerDialog(
//            context,
//            { _, year, month, _ ->
//                this.month = (month + 1).toString()
//                this.year = year.toString()
//
//                val cal = Calendar.getInstance().apply {
//                    set(Calendar.YEAR, year)
//                    set(Calendar.MONTH, month)
//                    set(Calendar.DAY_OF_MONTH, 1) // important
//                }
//
//                val monthName = SimpleDateFormat(
//                    "MMMM",
//                    Locale.getDefault()
//                ).format(
//                    cal.time
//                )
//
//                onSelected("$monthName $year")
//            },
//            calendar.get(Calendar.YEAR),
//            calendar.get(Calendar.MONTH),
//            calendar.get(Calendar.DAY_OF_MONTH)
//        )
//
//        // ✅ Hide day picker safely
//        try {
//            val daySpinnerId =
//                Resources.getSystem().getIdentifier("day", "id", "android")
//            dialog.datePicker.findViewById<View>(daySpinnerId)?.visibility =
//                View.GONE
//        } catch (_: Exception) {
//            // ignore (some devices)
//        }
//
//        dialog.show()
//    }



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
                    if (authPref?.get("userType") == "2") {
                        if (authPref?.get("groupId") != "0") {
                            groupId = authPref?.get("groupId").toString()
                            etGroup.setText(
                                groupList.firstOrNull { it.Id.toString() == groupId }?.Name ?: ""
                            )
                            etGroup.isEnabled = false

                            getEmployeeList()
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

    private fun getEmployeesReports() {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.viewReports(employeeId, month, year)

        call.enqueue(object : Callback<List<ViewReportsModelItem>> {

            override fun onResponse(
                call: Call<List<ViewReportsModelItem>>,
                response: Response<List<ViewReportsModelItem>>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    employeeReportsList.clear()
                    employeeReportsList.addAll(response.body()!!)
                    employeeReportsList.reverse()
                    adapter.submitList(employeeReportsList)
                    placeholder.visibility = View.GONE
                    if(employeeReportsList.isNotEmpty()){
                        btnDownloadReport.visibility = View.VISIBLE
                        layoutEmpty.visibility = View.VISIBLE
                        val params = btnViewReport.layoutParams as LinearLayout.LayoutParams
                        params.weight = 0.5f
                        btnViewReport.layoutParams = params

                    }else{
                        btnDownloadReport.visibility = View.GONE
                        layoutEmpty.visibility = View.GONE
                        val params = btnViewReport.layoutParams as LinearLayout.LayoutParams
                        params.weight = 1f
                        btnViewReport.layoutParams = params
                    }
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadCsv(list: List<ViewReportsModelItem>) {
        val fileName = "employees_${System.currentTimeMillis()}.csv"

        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?:return


            resolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.append("Name,Code,TimeStamp\n") // CSV header

                    list.forEach { emp ->
                        writer.append("${emp.Name},${emp.Code},${emp.TimeStamp}}\n")
                    }

//                    writer.flush()
                }
            }

        // ✅ Mark file as complete (VISIBLE IN FILE MANAGER)
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

            Toast.makeText(requireContext(), "Saved to Downloads: $fileName", Toast.LENGTH_SHORT).show()

        // ✅ Open file
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(openIntent, "Open CSV with"))

    }

}