package com.app.magkraft.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.MainActivity
import com.app.magkraft.R
import com.app.magkraft.model.AddGroupModel
import com.app.magkraft.model.AddLocationModel
import com.app.magkraft.model.CommonResponse
import com.app.magkraft.network.ApiClient
import com.app.magkraft.ui.adapters.GroupPopupAdapter
import com.app.magkraft.ui.adapters.LocationAdapter
import com.app.magkraft.ui.model.GroupListModel
import com.app.magkraft.ui.model.LocationListModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationFragment : Fragment(R.layout.fragment_location) {

    private lateinit var adapter: LocationAdapter
    private var groupList = ArrayList<GroupListModel>()
    private var locationList = ArrayList<LocationListModel>()

    private var ctx: Context? = null
    private var groupId = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.findViewById<FloatingActionButton>(R.id.fabAddLocation)
                .updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = systemBars.bottom + 16
                }
            insets
        }
        adapter = LocationAdapter(
            onEdit = { showLocationBottomSheet(it) },
            onDelete = { showDeleteLocationDialog(it.Id.toString()) }
        )

        view.findViewById<RecyclerView>(R.id.rvLocations).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LocationFragment.adapter
        }

        view.findViewById<FloatingActionButton>(R.id.fabAddLocation)
            .setOnClickListener {
                showLocationBottomSheet()
            }
        CoroutineScope(Dispatchers.Main).launch {
            getLocationList()
        }
        CoroutineScope(Dispatchers.Main).launch {
            getGroups()
        }

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

        rv.layoutManager = LinearLayoutManager(requireContext())
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

    private fun showLocationBottomSheet(existing: LocationListModel? = null) {

        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottomsheet_add_location, null)
        dialog.setContentView(view)

        val tvGroup = view.findViewById<
                TextView>(R.id.tvSelectGroup)
        val etLocation = view.findViewById<EditText>(R.id.etLocationName)
        val etAddress = view.findViewById<EditText>(R.id.etAddress)

        var selectedGroup: GroupListModel? = null

        tvGroup.setOnClickListener {
            showGroupPopup(tvGroup, groupList) {
                selectedGroup = it
                tvGroup.text = it.Name
                groupId = it.Id.toString()
            }
        }

        existing?.let {
            etLocation.setText(it.Name)
//            etAddress.setText(it.AddressName)
            groupId = it.Id.toString()
//            selectedGroup = it.GroupName
            tvGroup.text = it.GroupName
        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            if (selectedGroup == null) {

                Toast.makeText(requireContext(), "Select group", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (etLocation.text.toString().isEmpty()) {
                Toast.makeText(requireContext(), "Enter Location Name", Toast.LENGTH_SHORT).show()

            } else {
                if (existing != null) {
                    updateLocation(
                        existing.Id.toString(),
                        groupId,
                        etLocation.text.toString()
                    )
                } else {
                    addLocation(etLocation.text.toString())

                }
            }

            dialog.dismiss()
        }

        dialog.show()
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

    private fun getLocationList() {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.getLocations()

        call.enqueue(object : Callback<List<LocationListModel>> {

            override fun onResponse(
                call: Call<List<LocationListModel>>,
                response: Response<List<LocationListModel>>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {
                    locationList.clear()
                    locationList.addAll(response.body()!!)
                    adapter.submitList(locationList)

                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<LocationListModel>>, t: Throwable) {
                (ctx as MainActivity).hideLoader()
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addLocation(locationName: String) {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.addLocation(locationName, groupId, "1", "0")

        call.enqueue(object : Callback<AddLocationModel> {

            override fun onResponse(
                call: Call<AddLocationModel>,
                response: Response<AddLocationModel>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {

                    Toast.makeText(
                        ctx,
                        "Location Added Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    getLocationList()

                } else {
//                    val errorMessage = (ctx as MainActivity).getErrorMessage(response)
//                    Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AddLocationModel>, t: Throwable) {
                (ctx as MainActivity).hideLoader()
                Toast.makeText(ctx, t.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateLocation(locationId: String, groupId: String, locationName: String) {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.updateLocation(locationId, groupId, locationName, "1", "0")

        call.enqueue(object : Callback<CommonResponse> {

            override fun onResponse(
                call: Call<CommonResponse>,
                response: Response<CommonResponse>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {

                    Toast.makeText(
                        ctx,
                        "Location Updated Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    getLocationList()

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

    private fun deleteLocation(id: String) {

        (ctx as MainActivity).showLoader()

        val call = ApiClient.apiService.deleteLocation(id)

        call.enqueue(object : Callback<CommonResponse> {

            override fun onResponse(
                call: Call<CommonResponse>,
                response: Response<CommonResponse>
            ) {
                (ctx as MainActivity).hideLoader()

                if (response.isSuccessful && response.body() != null) {

                    Toast.makeText(
                        ctx,
                        "Location Deleted Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    getLocationList()

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

    private fun showDeleteLocationDialog(id: String) {
        AlertDialog.Builder(ctx!!)
            .setTitle("Magkraft")
            .setMessage("Are you sure you want to delete Location?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                deleteLocation(id)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


}