package com.marosseleng.distancemeasurements.ui.measurements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.marosseleng.distancemeasurements.R
import com.marosseleng.distancemeasurements.data.Measurement
import kotlinx.android.synthetic.main.item_measurement.view.*

/**
 * @author Maroš Šeleng
 */
class MeasurementAdapter(val callback: (Measurement) -> Unit) : RecyclerView.Adapter<MeasurementAdapter.ViewHolder>() {

    var items: List<Measurement> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_measurement, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder) {
            container.setOnClickListener {
                callback(item)
            }
            title.text = item.timestamp.toString()
            subtitle.text = item.deviceName
            measurementType.text = item.measurementType.name
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.container
        val title: TextView = itemView.titleLine
        val subtitle: TextView = itemView.subtitleLine
        val measurementType: TextView = itemView.measurementType
    }
}
