package com.app.magkraft.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.R
import com.app.magkraft.ui.model.EmployeeListModel
import com.app.magkraft.ui.model.ViewReportsModelItem

class ViewReportsAdapter() : RecyclerView.Adapter<ViewReportsAdapter.GroupVH>() {

    private val items = mutableListOf<ViewReportsModelItem>()
    lateinit var ctx: Context
    fun submitList(list: List<ViewReportsModelItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_view_reports, parent, false)
        ctx = parent.context
        return GroupVH(view)
    }

    override fun onBindViewHolder(holder: GroupVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class GroupVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ViewReportsModelItem) {
            itemView.findViewById<TextView>(R.id.txtEmployeeName).text = item.Name
            itemView.findViewById<TextView>(R.id.txtEmployeeId).text = "Code: " + item.Code
//            itemView.findViewById<TextView>(R.id.groupId).text = "Location Id: " + item.LocationId
            itemView.findViewById<TextView>(R.id.timeStamp).text = "Time: " + item.TimeStamp


        }
    }
}
