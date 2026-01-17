package com.app.magkraft.ui.adapters

import android.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.magkraft.ui.model.GroupListModel
import com.app.magkraft.ui.model.LocationListModel

class LocationPopupAdapter(
    private val locations: MutableList<LocationListModel>,
    private val onClick: (LocationListModel) -> Unit
) : RecyclerView.Adapter<LocationPopupAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tv = view.findViewById<TextView>(R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.simple_list_item_1, parent, false)
        return VH(view)
    }

    override fun getItemCount() = locations.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = locations[position].Name
        holder.itemView.setOnClickListener {
            onClick(locations[position])
        }
    }

    fun update(newList: List<LocationListModel>) {
        locations.clear()
        locations.addAll(newList)
        notifyDataSetChanged()
    }
}
