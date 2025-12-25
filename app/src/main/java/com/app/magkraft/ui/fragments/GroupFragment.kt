package com.app.magkraft.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.R
import com.app.magkraft.ui.adapters.GroupAdapter
import com.app.magkraft.ui.model.GroupListModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial

class GroupFragment : Fragment(R.layout.fragment_group) {
    private lateinit var adapter: GroupAdapter
   private var groupList = mutableListOf<GroupListModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adapter = GroupAdapter(
            onEdit = { showGroupBottomSheet(it) },
            onDelete = { }
        )

        view.findViewById<RecyclerView>(R.id.rvGroups).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GroupFragment.adapter
        }

        view.findViewById<FloatingActionButton>(R.id.fabAddGroup)
            .setOnClickListener {
                showGroupBottomSheet()
            }
      groupList.clear()
        groupList.add(0,GroupListModel(1,"one",true))
        groupList.add(1,GroupListModel(2,"two",false))
        groupList.add(2,GroupListModel(3,"three",true))
        groupList.add(3,GroupListModel(4,"four",false))
        groupList.add(4,GroupListModel(5,"five",false))
        groupList.add(5,GroupListModel(6,"six",true))
        groupList.add(6,GroupListModel(7,"seven",false))
        adapter.submitList(groupList)
//        observeGroups()
    }

    private fun showGroupBottomSheet(existing: GroupListModel? = null) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottomsheet_add_group, null)
        dialog.setContentView(view)

        val etName = view.findViewById<EditText>(R.id.etGroupName)
        val switchActive = view.findViewById<SwitchMaterial>(R.id.switchActive)

        existing?.let {
            etName.setText(it.name)
            switchActive.isChecked = it.isActive
        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val group = GroupListModel(
                id = existing?.id ?: 0,
                name = etName.text.toString(),
                isActive = switchActive.isChecked
            )
           // saveGroup(group)
            dialog.dismiss()
        }

        dialog.show()
    }


}