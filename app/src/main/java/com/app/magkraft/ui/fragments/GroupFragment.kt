package com.app.magkraft.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
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
import com.app.magkraft.model.AddGroupModel
import com.app.magkraft.model.CommonResponse

import com.app.magkraft.network.ApiClient
import com.app.magkraft.ui.BaseActivity
import com.app.magkraft.ui.adapters.GroupAdapter
import com.app.magkraft.ui.model.GroupListModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupFragment : Fragment(R.layout.fragment_group) {
    private lateinit var adapter: GroupAdapter
    private var groupList = ArrayList<GroupListModel>()
    private var ctx: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.findViewById<FloatingActionButton>(R.id.fabAddGroup)
                .updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = systemBars.bottom + 16
                }
            insets
        }
        getGroups()
        adapter = GroupAdapter(
            onEdit = { showGroupBottomSheet(it) },
            onDelete = {showDeleteGroupDialog(it.Id.toString()) }
        )

        view.findViewById<RecyclerView>(R.id.rvGroups).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GroupFragment.adapter
        }

        view.findViewById<FloatingActionButton>(R.id.fabAddGroup)
            .setOnClickListener {
                showGroupBottomSheet()
            }

    }

    private fun showGroupBottomSheet(existing: GroupListModel? = null) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottomsheet_add_group, null)
        dialog.setContentView(view)

        val etName = view.findViewById<EditText>(R.id.etGroupName)
        val switchActive = view.findViewById<SwitchMaterial>(R.id.switchActive)

        existing?.let {
            etName.setText(it.Name)
            switchActive.isChecked = it.IsActive
        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
//            val group = GroupListModel(
//                id = existing?.id ?: 0,
//                name = etName.text.toString(),
//                isActive = switchActive.isChecked
//            )
//           // saveGroup(group)
            val status = if (switchActive.isChecked) {
                "1"
            } else {
                "0"
            }
            if (etName.text.toString().isEmpty()) {
                (ctx as MainActivity).showToast(ctx!!, "Enter Group Name")
            } else {
                if (existing != null) {
                    updateGroup(existing.Id.toString(), etName.text.toString(), status)
                } else {
                    addGroup(etName.text.toString(), status)
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun addGroup(groupName: String, status: String) {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.addGroup(groupName, status)

        call.enqueue(object : Callback<AddGroupModel> {

            override fun onResponse(
                call: Call<AddGroupModel>,
                response: Response<AddGroupModel>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {

                    Toast.makeText(
                        ctx,
                        "Group Added Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    getGroups()

                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AddGroupModel>, t: Throwable) {
                (ctx as MainActivity).hideLoader()
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun getGroups() {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.getGroups()

        call.enqueue(object : Callback<List<GroupListModel>> {

            override fun onResponse(
                call: Call<List<GroupListModel>>,
                response: Response<List<GroupListModel>>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    groupList.clear()
                    groupList.addAll(response.body()!!)
                    adapter.submitList(groupList)

                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<GroupListModel>>, t: Throwable) {
                (ctx as MainActivity).hideLoader()
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateGroup(groupId: String, groupName: String, status: String) {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.updateGroup(groupId, groupName, status,"0")

        call.enqueue(object : Callback<CommonResponse> {

            override fun onResponse(
                call: Call<CommonResponse>,
                response: Response<CommonResponse>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {

                    Toast.makeText(
                        ctx,
                        "Group Updated Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    getGroups()

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

    private fun deleteGroup(id: String) {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.deleteGroup(id)

        call.enqueue(object : Callback<CommonResponse> {

            override fun onResponse(
                call: Call<CommonResponse>,
                response: Response<CommonResponse>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {

                    Toast.makeText(
                        ctx,
                        "Group Deleted Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    getGroups()

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

    private fun showDeleteGroupDialog(id: String) {
        AlertDialog.Builder(ctx!!)
            .setTitle("Logout")
            .setMessage("Are you sure you want to delete Group?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                deleteGroup(id)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}