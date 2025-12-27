package com.app.magkraft.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.R
import com.app.magkraft.ui.model.LocationListModel

class LocationAdapter(
    private val onEdit: (LocationListModel) -> Unit,
    private val onDelete: (LocationListModel) -> Unit
) : RecyclerView.Adapter<LocationAdapter.GroupVH>() {

    private val items = mutableListOf<LocationListModel>()

    fun submitList(list: List<LocationListModel>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location, parent, false)
        return GroupVH(view)
    }

    override fun onBindViewHolder(holder: GroupVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class GroupVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: LocationListModel) {
            itemView.findViewById<TextView>(R.id.txtLocationName).text = item.locationName
            itemView.findViewById<TextView>(R.id.txtAddressName).text = item.AddressName


            itemView.findViewById<ImageView>(R.id.btnEdit).setOnClickListener {
                onEdit(item)
            }

            itemView.findViewById<ImageView>(R.id.btnDelete).setOnClickListener {
                onDelete(item)
            }
        }
    }
}
