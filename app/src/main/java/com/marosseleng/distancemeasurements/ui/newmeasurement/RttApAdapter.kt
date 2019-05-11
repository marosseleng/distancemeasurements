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

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.marosseleng.distancemeasurements.R
import com.marosseleng.distancemeasurements.application
import kotlinx.android.synthetic.main.item_beacon.view.*

/**
 * @author Maroš Šeleng
 */
class RttApAdapter(val onClickListener: (ScanResult) -> Unit) : RecyclerView.Adapter<RttApAdapter.ViewHolder>() {

    var aps: List<ScanResult> = emptyList()
        set(value) {
            field = value.sortedBy { it.SSID }
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_beacon, parent, false)
        )
    }

    override fun getItemCount() = aps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ap = aps[position]
        with(holder) {
            container.setOnClickListener { onClickListener(ap) }
            title.text = ap.SSID
            methodIcon.setImageDrawable(ContextCompat.getDrawable(application, R.drawable.ic_wifi_black_24dp))
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.container
        val title: TextView = itemView.anchorDescription
        val methodIcon: ImageView = itemView.methodIcon
    }
}