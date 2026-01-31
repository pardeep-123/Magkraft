package com.app.magkraft.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.MainActivity
import com.app.magkraft.R
import com.app.magkraft.model.CommonResponse
import com.app.magkraft.network.ApiClient
import com.app.magkraft.ui.RegisterActivity
import com.app.magkraft.ui.adapters.EmployeeAdapter
import com.app.magkraft.ui.model.EmployeeListModel
import com.app.magkraft.ui.model.GroupListModel
import com.app.magkraft.ui.model.LocationListModel
import com.app.magkraft.utils.EmployeeFilterBottomSheet

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EmployeeFragment : Fragment(R.layout.fragment_employee) {
    private lateinit var adapter: EmployeeAdapter
    private var employeeList = mutableListOf<EmployeeListModel>()

    private var groupList = ArrayList<GroupListModel>()

    lateinit var ctx : Context
    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onResume() {
        super.onResume()
        getEmployeesList()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_employee, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                openFilterBottomSheet()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.findViewById<FloatingActionButton>(R.id.fabAddEmployee).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 16
            }
            insets
        }
        adapter = EmployeeAdapter(
            onEdit = {
                startActivity(
                    Intent(requireContext(), RegisterActivity::class.java)
                        .putExtra("data",it))
            },
            onDelete = {
                showDeleteEmployeeDialog(it.Id.toString())
            }
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

        CoroutineScope(Dispatchers.Main).launch{
            getGroups()
        }
    }


    private fun getEmployeesList() {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.getEmployees()

        call.enqueue(object : Callback<List<EmployeeListModel>> {

            override fun onResponse(
                call: Call<List<EmployeeListModel>>,
                response: Response<List<EmployeeListModel>>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    employeeList.clear()
                    employeeList.addAll(response.body()!!)
                    adapter.submitList(employeeList)

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


    private fun deleteEmployee(id: String) {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.deleteEmployee(id)

        call.enqueue(object : Callback<CommonResponse> {

            override fun onResponse(
                call: Call<CommonResponse>,
                response: Response<CommonResponse>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {

                    Toast.makeText(
                        ctx,
                        "User Deleted Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    getEmployeesList()

                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
                    Toast.makeText(ctx, response.body()?.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<CommonResponse>, t: Throwable) {
                (ctx as MainActivity).hideLoader()
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
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
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteEmployeeDialog(id: String) {
        AlertDialog.Builder(ctx)
            .setTitle("Logout")
            .setMessage("Are you sure you want to delete Employee?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                deleteEmployee(id)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private var selectedGroupId: String? = null
    private var selectedStatus: String? = null

    private fun openFilterBottomSheet() {
        EmployeeFilterBottomSheet(
            groups = groupList,
            selectedGroupId = selectedGroupId,
            selectedStatus = selectedStatus
        ) { groupId, status ->
            selectedGroupId = groupId
            selectedStatus = status
            applyFilters()
        }.show(parentFragmentManager, "EmployeeFilter")
    }

    private fun applyFilters() {
        val filtered = employeeList.filter { emp ->
            val groupMatch = selectedGroupId == null || emp.GroupId == selectedGroupId?.toInt()
//            val statusMatch = selectedStatus == null || emp.IsActive == selectedStatus
            groupMatch
        }

        adapter.submitList(filtered)
    }

}