package com.app.magkraft.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import com.app.magkraft.R
import com.app.magkraft.ui.model.GroupListModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EmployeeFilterBottomSheet(
    private val groups: List<GroupListModel>,
    private val selectedGroupId: Int?,
    private var selectedStatus: Boolean,
    private var searchedName: String,
    private val onApply: (groupId: Int?, status: Boolean, name: String) -> Unit
) : BottomSheetDialogFragment() {
    var authPref: AuthPref? = null
    var selectedGroup: GroupListModel? = null
    private var showInactive = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_filter, container, false)

        val etGroup = view.findViewById<AutoCompleteTextView>(R.id.etGroup)
//        val etStatus = view.findViewById<AutoCompleteTextView>(R.id.etStatus)
        val btnApply = view.findViewById<Button>(R.id.btnApply)
        val btnClear = view.findViewById<Button>(R.id.btnClear)
        val searchName = view.findViewById<EditText>(R.id.etName)
        val checkBoxTerms = view.findViewById<CheckBox>(R.id.checkBoxTerms)

        authPref = AuthPref(requireContext())

        // --- Group Dropdown ---
        val groupNames = groups.map { it.Name }
        val groupAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, groupNames)
        etGroup.setAdapter(groupAdapter)

        /**
         * Here we need to check , if group id is not 0 with user type 2, then
         * set group id to that
         */
        if (authPref?.get("userType") == "2") {
            if (authPref?.get("groupId") != "0") {
                selectedGroup =
                    groups.firstOrNull { it.Id == authPref?.get("groupId")?.toInt() }
                etGroup.setText(groups.firstOrNull {
                    it.Id == authPref?.get("groupId")?.toInt()
                }?.Name ?: "")
                etGroup.isEnabled = false
                etGroup.isFocusable = false
                showInactive = selectedStatus
                checkBoxTerms.isChecked = showInactive
                searchName.setText(searchedName)
            }
        } else {

            selectedGroup =
                groups.firstOrNull { it.Id == selectedGroupId?.toInt() }
            etGroup.setText(selectedGroup?.Name ?: "", false)

            etGroup.setOnItemClickListener { _, _, position, _ ->
                selectedGroup = groups[position]
            }
            showInactive = selectedStatus
            checkBoxTerms.isChecked = showInactive

            searchName.setText(searchedName)

        }

        checkBoxTerms.setOnCheckedChangeListener { _, isChecked ->
            showInactive = isChecked

        }

        // --- Status Dropdown ---
//        val statuses = listOf("Active", "Inactive")
//        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
//        etStatus.setAdapter(statusAdapter)
//
//        // Hint only (no preselect)
//        etStatus.hint = "Select Status"
//        etStatus.setText("", false)
//
//        // Preselect
//        when (selectedStatus) {
//            true -> etStatus.setText("Active", false)
//            false -> etStatus.setText("Inactive", false)
//            null -> etStatus.setText("", false)
//        }
//
//        etStatus.setOnItemClickListener { _, _, position, _ ->
//            selectedStatus = position == 0
//        }
//        etStatus.setText(selectedStatus ?: "", false)

        btnApply.setOnClickListener {
            onApply(selectedGroup?.Id, showInactive, searchName.text.toString())
            dismiss()
        }

        btnClear.setOnClickListener {
            onApply(null, false, "")
            dismiss()
        }

        return view
    }
}
