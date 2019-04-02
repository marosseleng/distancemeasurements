package com.marosseleng.distancemeasurements.ui.newmeasurement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.marosseleng.distancemeasurements.data.MeasuredValue

/**
 * @author Maroš Šeleng
 */
class RawMeasuredValueAdapter : RecyclerView.Adapter<RawMeasuredValueAdapter.ViewHolder>() {

    private val items = mutableListOf<Int>()

    fun addItem(newItem: Int) {
        items.add(0, newItem)
        notifyItemInserted(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val value = items[position]
        holder.text.text = String.format("%d. RSS: %d", items.size - position, value)
    }

    fun clear() {
        items.clear()
        val itemCount = itemCount
        notifyItemRangeRemoved(0, itemCount)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(android.R.id.text1)
    }
}