package com.app.magkraft.ui.adapters

import android.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.ui.model.EmployeeListModel
import com.app.magkraft.ui.model.GroupListModel

class EmployeePopupAdapter(
    private val employeeList: MutableList<EmployeeListModel>,
    private val onClick: (EmployeeListModel) -> Unit
) : RecyclerView.Adapter<EmployeePopupAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tv = view.findViewById<TextView>(R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.simple_list_item_1, parent, false)
        return VH(view)
    }

    override fun getItemCount() = employeeList.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = employeeList[position].Name
        holder.itemView.setOnClickListener {
            onClick(employeeList[position])
        }
    }

    fun update(newList: List<EmployeeListModel>) {
        employeeList.clear()
        employeeList.addAll(newList)
        notifyDataSetChanged()
    }
}
