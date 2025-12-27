package com.app.magkraft.ui.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.R
import com.app.magkraft.ui.RegisterActivity
import com.app.magkraft.ui.adapters.EmployeeAdapter
import com.app.magkraft.ui.model.EmployeeListModel

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial

class EmployeeFragment : Fragment(R.layout.fragment_employee) {
    private lateinit var adapter: EmployeeAdapter
    private var employeeList = mutableListOf<EmployeeListModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.findViewById<FloatingActionButton>(R.id.fabAddEmployee).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 16
            }
            insets
        }
        adapter = EmployeeAdapter(
            onEdit = {  },
            onDelete = { }
        )

        view.findViewById<RecyclerView>(R.id.rvEmployees).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@EmployeeFragment.adapter
        }

        view.findViewById<FloatingActionButton>(R.id.fabAddEmployee)
            .setOnClickListener {
                startActivity(
                    Intent(requireContext(), RegisterActivity::class.java)
                )
            }
        employeeList.clear()
        employeeList.add(0,EmployeeListModel(1,"Sam","ABVP",true))
        employeeList.add(1,EmployeeListModel(2,"Will","NTPC",false))
        employeeList.add(2,EmployeeListModel(3,"Jamie","Aus",true))
        employeeList.add(3,EmployeeListModel(4,"Pardeep","BJP",false))
        employeeList.add(4,EmployeeListModel(5,"Jimmy","AAP",false))
        employeeList.add(5,EmployeeListModel(6,"Anmol","CONG",true))
        employeeList.add(6,EmployeeListModel(7,"Cummins","RJD", false))
        adapter.submitList(employeeList)
//        observeGroups()
    }



}