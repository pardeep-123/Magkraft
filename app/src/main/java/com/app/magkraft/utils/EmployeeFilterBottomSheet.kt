package com.app.magkraft.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import com.app.magkraft.R
import com.app.magkraft.ui.model.GroupListModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EmployeeFilterBottomSheet(
    private val groups: List<GroupListModel>,
    private val selectedGroupId: Int?,
    private var selectedStatus: Boolean?,
    private val onApply: (groupId: Int?, status: Boolean?) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_filter, container, false)

        val etGroup = view.findViewById<AutoCompleteTextView>(R.id.etGroup)
        val etStatus = view.findViewById<AutoCompleteTextView>(R.id.etStatus)
        val btnApply = view.findViewById<Button>(R.id.btnApply)
        val btnClear = view.findViewById<Button>(R.id.btnClear)

        // --- Group Dropdown ---
        val groupNames = groups.map { it.Name }
        val groupAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, groupNames)
        etGroup.setAdapter(groupAdapter)

        var selectedGroup: GroupListModel? = groups.firstOrNull { it.Id == selectedGroupId?.toInt() }
        etGroup.setText(selectedGroup?.Name ?: "", false)

        etGroup.setOnItemClickListener { _, _, position, _ ->
            selectedGroup = groups[position]
        }

        // --- Status Dropdown ---
        val statuses = listOf("Active", "Inactive")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        etStatus.setAdapter(statusAdapter)

        // Hint only (no preselect)
        etStatus.hint = "Select Status"
        etStatus.setText("", false)

        // Preselect
        when (selectedStatus) {
            true -> etStatus.setText("Active", false)
            false -> etStatus.setText("Inactive", false)
            null -> etStatus.setText("", false)
        }

        etStatus.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = position == 0
        }
//        etStatus.setText(selectedStatus ?: "", false)

        btnApply.setOnClickListener {
            onApply(selectedGroup?.Id, selectedStatus)
            dismiss()
        }

        btnClear.setOnClickListener {
            onApply(null, null)
            dismiss()
        }

        return view
    }
}
