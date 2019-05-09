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

package com.marosseleng.distancemeasurements.ui.measurements

import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_SHOW_DATE
import android.text.format.DateUtils.FORMAT_SHOW_TIME
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.marosseleng.distancemeasurements.R
import com.marosseleng.distancemeasurements.application
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
            title.text = DateUtils.formatDateTime(application, item.timestamp, FORMAT_SHOW_DATE or FORMAT_SHOW_TIME)
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
