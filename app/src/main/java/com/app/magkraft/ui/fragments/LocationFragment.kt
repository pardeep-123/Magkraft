package com.app.magkraft.ui.fragments

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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.R
import com.app.magkraft.ui.adapters.GroupPopupAdapter
import com.app.magkraft.ui.adapters.LocationAdapter
import com.app.magkraft.ui.model.GroupListModel
import com.app.magkraft.ui.model.LocationListModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LocationFragment : Fragment(R.layout.fragment_location) {

    private lateinit var adapter: LocationAdapter
    private var locationList = mutableListOf<LocationListModel>()
    private var groupsList = mutableListOf<GroupListModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.findViewById<FloatingActionButton>(R.id.fabAddLocation).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 16
            }
            insets
        }
        adapter = LocationAdapter(
            onEdit = { showLocationBottomSheet(it) },
            onDelete = { }
        )

        view.findViewById<RecyclerView>(R.id.rvLocations).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LocationFragment.adapter
        }

        view.findViewById<FloatingActionButton>(R.id.fabAddLocation)
            .setOnClickListener {
                showLocationBottomSheet()
            }
        locationList.clear()
        locationList.add(0,LocationListModel(1,"one","mohali"))
        locationList.add(1,LocationListModel(2,"two","mohali"))
        locationList.add(2,LocationListModel(3,"three","mohali"))
        locationList.add(3,LocationListModel(4,"four","mohali"))
        locationList.add(4,LocationListModel(5,"five","mohali"))
        locationList.add(5,LocationListModel(6,"six","mohali"))
        locationList.add(6,LocationListModel(7,"seven","mohali"))
        adapter.submitList(locationList)


        groupsList.clear()
        groupsList.add(0,GroupListModel(1,"one",true))
        groupsList.add(1,GroupListModel(2,"two",false))
        groupsList.add(2,GroupListModel(3,"three",true))
        groupsList.add(3,GroupListModel(4,"four",false))
        groupsList.add(4,GroupListModel(5,"five",false))
        groupsList.add(5,GroupListModel(6,"six",true))
        groupsList.add(6,GroupListModel(7,"seven",false))


//        observeGroups()
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
            val filtered = groups.filter { g ->
                g.name.contains(it.toString(), true)
            }
            adapter.update(filtered)
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
            showGroupPopup(tvGroup, groupsList) {
                selectedGroup = it
                tvGroup.text = it.name
            }
        }

//        existing?.let {
//            etLocation.setText(it.locationName)
//            etAddress.setText(it.AddressName)
//            selectedGroup = it.group
//            tvGroup.text = it.group.name
//        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            if (selectedGroup == null) {

                Toast.makeText(requireContext(), "Select group", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

//            val location = LocationListModel(
//                id = existing?.id ?: 0,
//                locationName = etLocation.text.toString(),
//                address = etAddress.text.toString(),
//                group = selectedGroup!!
//            )

            // saveLocation(location)
            dialog.dismiss()
        }

        dialog.show()
    }


}