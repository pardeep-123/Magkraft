package com.app.magkraft.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.ui.model.GroupListModel

class GroupPopupAdapter(
    private val groups: MutableList<GroupListModel>,
    private val onClick: (GroupListModel) -> Unit
) : RecyclerView.Adapter<GroupPopupAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tv = view.findViewById<TextView>(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return VH(view)
    }

    override fun getItemCount() = groups.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = groups[position].name
        holder.itemView.setOnClickListener {
            onClick(groups[position])
        }
    }

    fun update(newList: List<GroupListModel>) {
        groups.clear()
        groups.addAll(newList)
        notifyDataSetChanged()
    }
}
