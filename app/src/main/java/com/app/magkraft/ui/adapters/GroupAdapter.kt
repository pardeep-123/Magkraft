package com.app.magkraft.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.R
import com.app.magkraft.ui.model.GroupListModel

class GroupAdapter(
    private val onEdit: (GroupListModel) -> Unit,
    private val onDelete: (GroupListModel) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupVH>() {

    private val items = mutableListOf<GroupListModel>()

    fun submitList(list: List<GroupListModel>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupVH(view)
    }

    override fun onBindViewHolder(holder: GroupVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class GroupVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: GroupListModel) {
            itemView.findViewById<TextView>(R.id.txtGroupName).text = item.Name
            itemView.findViewById<TextView>(R.id.txtStatus).text =
                if (item.IsActive) "Active" else "Inactive"

            itemView.findViewById<ImageView>(R.id.btnEdit).setOnClickListener {
                onEdit(item)
            }

            itemView.findViewById<ImageView>(R.id.btnDelete).setOnClickListener {
                onDelete(item)
            }
        }
    }
}
