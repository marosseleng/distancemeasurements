package com.marosseleng.distancemeasurements.ui.newmeasurement

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.marosseleng.distancemeasurements.R
import kotlinx.android.synthetic.main.item_beacon.view.*

/**
 * @author Maroš Šeleng
 */
class BeaconsAdapter(val onClickListener: (ScanResult) -> Unit) : RecyclerView.Adapter<BeaconsAdapter.BeaconViewHolder>() {

    var beacons: List<ScanResult> = emptyList()
        set(value) {
            field = value.sortedBy { it.device?.name }
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconViewHolder {
        return BeaconViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_beacon, parent, false)
        )
    }

    override fun getItemCount() = beacons.size

    override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
        val beacon = beacons[position]
        with(holder) {
            container.setOnClickListener { onClickListener(beacon) }
            title.text = beacon.device?.name ?: "???"
        }
    }

    class BeaconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.container
        val title: TextView = itemView.anchorDescription
    }
}