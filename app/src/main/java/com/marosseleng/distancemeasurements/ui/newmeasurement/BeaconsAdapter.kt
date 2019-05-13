/*
 * Copyright 2019 Maroš Šeleng
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.marosseleng.distancemeasurements.ui.newmeasurement

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.marosseleng.distancemeasurements.R
import kotlinx.android.synthetic.main.item_beacon.view.*

class BeaconsAdapter(val onClickListener: (ScanResult) -> Unit) :
    RecyclerView.Adapter<BeaconsAdapter.BeaconViewHolder>() {

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