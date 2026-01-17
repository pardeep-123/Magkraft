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

class EmployeeAdapter(
    private val onEdit: (EmployeeListModel) -> Unit,
    private val onDelete: (EmployeeListModel) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.GroupVH>() {

    private val items = mutableListOf<EmployeeListModel>()
    lateinit var ctx: Context
    fun submitList(list: List<EmployeeListModel>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_employees, parent, false)
        ctx = parent.context
        return GroupVH(view)
    }

    override fun onBindViewHolder(holder: GroupVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class GroupVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: EmployeeListModel) {
            itemView.findViewById<TextView>(R.id.txtEmployeeName).text = item.Name
            itemView.findViewById<TextView>(R.id.txtEmployeeId).text =
                "Employee Id: " + item.Code.toString()
            itemView.findViewById<TextView>(R.id.txtStatus).text =
                if (item.IsActive) "Active" else "InActive"
            if (item.IsActive)
                itemView.findViewById<TextView>(R.id.txtStatus)
                    .setTextColor(ctx.getColor(R.color.green))
            else
                itemView.findViewById<TextView>(R.id.txtStatus)
                    .setTextColor(ctx.getColor(R.color.pink_color))
            itemView.findViewById<ImageView>(R.id.btnEdit).setOnClickListener {
                onEdit(item)
            }

            itemView.findViewById<ImageView>(R.id.btnDelete).setOnClickListener {
                onDelete(item)
            }
        }
    }
}
