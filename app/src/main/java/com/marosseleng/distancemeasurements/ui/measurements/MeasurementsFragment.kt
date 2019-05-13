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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.marosseleng.distancemeasurements.R
import com.marosseleng.distancemeasurements.data.Measurement
import com.marosseleng.distancemeasurements.data.MeasurementType
import kotlinx.android.synthetic.main.fragment_measurements.*
import timber.log.Timber

class MeasurementsFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var viewModel: MeasurementsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_measurements, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val measurementAdapter = MeasurementAdapter {
            val action = MeasurementsFragmentDirections.openMeasurementDetailAction(it.id)
            findNavController().navigate(action)
        }

        with(list) {
            adapter = measurementAdapter
            addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
        }

        with(spinner) {
            adapter = ArrayAdapter<String>(
                context,
                android.R.layout.simple_spinner_item,
                MeasurementType.values().map { it.toString() } + getString(R.string.measurements_list_measurement_type_all))
                .apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            onItemSelectedListener = this@MeasurementsFragment
        }

        viewModel = ViewModelProviders.of(this).get(MeasurementsViewModel::class.java)

        viewModel.measurements.observe(this, Observer { measurements: List<Measurement> ->
            noMeasurements.isVisible = measurements.isEmpty()
            list.isVisible = !noMeasurements.isVisible
            measurementAdapter.items = measurements
        })
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Timber.d("onNothingSelected")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.setMeasurementTypeFilter(position)
    }
}
