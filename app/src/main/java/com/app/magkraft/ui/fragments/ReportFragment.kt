package com.app.magkraft.ui.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class ReportFragment : Fragment(R.layout.fragment_report) {

    lateinit var etGroup: EditText
    lateinit var etLocation: EditText
    lateinit var etEmployee: EditText
    lateinit var etMonth: EditText
    lateinit var btnViewReport: Button
    lateinit var layoutEmpty: LinearLayout
    lateinit var progressBar: ProgressBar
    lateinit var rvReport: RecyclerView

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
        setState(ReportState.EMPTY)

        etMonth.setOnClickListener {
            showMonthYearPicker(requireContext()) {
                etMonth.setText(it)
            }
        }

        btnViewReport.setOnClickListener {

            if (etGroup.text.isNullOrEmpty() ||
                etLocation.text.isNullOrEmpty() ||
                etEmployee.text.isNullOrEmpty() ||
                etMonth.text.isNullOrEmpty()) {

                Toast.makeText(requireContext(),
                    "Please select all filters",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setState(ReportState.LOADING)

            lifecycleScope.launch {
                delay(1200) // simulate DB/API

//                if (reportList.isEmpty()) {
//                    setState(ReportState.NO_DATA)
//                } else {
//                    setState(ReportState.DATA)
//                }
            }
        }
    }

    fun showMonthYearPicker(context: Context, onSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        val dialog = DatePickerDialog(
            context,
            { _, year, month, _ ->
                val selected = "${month + 1}/$year"
                onSelected(selected)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.datePicker.findViewById<View>(
            Resources.getSystem().getIdentifier("day", "id", "android")
        )?.visibility = View.GONE

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


    enum class ReportState {
        EMPTY,
        LOADING,
        DATA,
        NO_DATA
    }
}