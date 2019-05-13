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

package com.marosseleng.distancemeasurements.ui.measurementdetail

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_SHOW_DATE
import android.text.format.DateUtils.FORMAT_SHOW_TIME
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import com.marosseleng.distancemeasurements.R
import com.marosseleng.distancemeasurements.application
import com.marosseleng.distancemeasurements.data.Measurement
import com.marosseleng.distancemeasurements.delegates.PermissionsDelegate
import com.marosseleng.distancemeasurements.delegates.ShouldShowRationaleDelegate
import com.marosseleng.distancemeasurements.requestcodes.EXPORT_CHART_STORAGE_PERMISSION
import com.marosseleng.distancemeasurements.requestcodes.EXPORT_VALUES_STORAGE_PERMISSION
import com.marosseleng.distancemeasurements.tags.STORAGE_PERMISSION_RATIONALE
import com.marosseleng.distancemeasurements.ui.common.BottomSheetDialogListener
import kotlinx.android.synthetic.main.fragment_measurement_detail.*
import timber.log.Timber

class MeasurementDetailFragment : Fragment(), BottomSheetDialogListener {

    private lateinit var viewModel: MeasurementDetailViewModel
    private lateinit var valuesAdapter: MeasuredValueAdapter
    private val args: MeasurementDetailFragmentArgs by navArgs()
    private val measurementId: Long by lazy { args.measurementId }

    private val hasExternalStoragePermission: Boolean by PermissionsDelegate(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val shouldShowRationale: Boolean by ShouldShowRationaleDelegate(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun getTitleFormatted(measurement: Measurement): String {
        val dateFormatted =
            DateUtils.formatDateTime(application, measurement.timestamp, FORMAT_SHOW_DATE or FORMAT_SHOW_TIME)
        return "${measurement.measurementType}-$dateFormatted"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_measurement_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this, MeasurementDetailViewModel.Factory(measurementId))
            .get(MeasurementDetailViewModel::class.java)

        valuesAdapter = MeasuredValueAdapter()

        bindViewModel()
        setupUI()
    }

    private fun bindViewModel() {
        viewModel.measurement.observe(this, Observer { measurement: Measurement? ->
            Timber.e("==>observing measurement: %s", measurement)
            if (measurement == null) {
                return@Observer
            }
            title.text = getTitleFormatted(measurement)
            if (measurement.realDistances.parsedValues != realDistances.editText?.text?.toString()) {
                realDistances.editText?.setText(measurement.realDistances.parsedValues)
            }
            if (measurement.note != note.editText?.text?.toString()) {
                note.editText?.setText(measurement.note)
            }
        })

        viewModel.values.observe(this, Observer {
            val isListEmpty = it.isEmpty()
            noValues.isVisible = isListEmpty
            list.isVisible = !isListEmpty
            graphWrapper.isVisible = !isListEmpty

            if (!isListEmpty) {
                valuesAdapter.items = it
            }
        })

        viewModel.dataSets.observe(this, Observer {
            val (graphValues, realDistances) = it
            val array = graphValues.map { (fst, snd) ->
                Entry(fst, snd)
            }
            val measured = LineDataSet(array, getString(R.string.measurement_detail_graph_label_measured)).apply {
                setDrawValues(false)
                color = Color.BLUE
                setDrawCircles(false)
                setDrawCircleHole(false)
                mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                isHighlightEnabled = false
            }
            val realArray = realDistances.map { (fst, snd) ->
                Entry(fst, snd)
            }
            val real = LineDataSet(realArray, getString(R.string.measurement_detail_graph_label_real)).apply {
                setDrawValues(false)
                color = Color.RED
                setDrawCircles(false)
                setDrawCircleHole(false)
                mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                isHighlightEnabled = false
            }
            graph.run {
                data = LineData(measured, real)
                invalidate()
            }
        })

        viewModel.exportProgress.observe(this, Observer {
            when (it) {
                is ExportProgress.NotStarted -> {
                    saveGraph.isEnabled = true
                    saveValues.isEnabled = true
                }
                is ExportProgress.Running -> {
                    saveGraph.isEnabled = false
                    saveValues.isEnabled = false
                }
                is ExportProgress.Failure -> {
                    val cause = it.cause
                    Timber.e(cause)
                    Snackbar
                        .make(
                            measurementDetailContent,
                            R.string.measurement_detail_export_failure,
                            Snackbar.LENGTH_SHORT
                        )
                        .setAction(R.string.measurement_detail_export_failure_retry) {
                            viewModel.exportBitmap(graph.chartBitmap)
                        }
                        .show()
                    viewModel.resetExportProgress()
                }
                is ExportProgress.Success -> {
                    val uri = it.fileUri
                    val intent = Intent(ACTION_VIEW).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val snackbar = Snackbar.make(
                        measurementDetailContent,
                        R.string.measurement_detail_export_success,
                        Snackbar.LENGTH_SHORT
                    )
                    if (intent.resolveActivity(application.packageManager) != null) {
                        snackbar.setAction(R.string.measurement_detail_export_success_view) {
                            startActivity(intent)
                        }
                    }
                    snackbar.show()
                    viewModel.resetExportProgress()
                }
            }
        })
    }

    private fun setupUI() {
        with(list) {
            adapter = valuesAdapter
        }

        saveEdits.setOnClickListener {
            Timber.d("==>saveEdits clicked, measurement: %s", viewModel.measurement.value)
            viewModel.saveEdits(note.editText?.text?.toString(), realDistances.editText?.text?.toString())
        }

        with(graph) {
            xAxis.setDrawLabels(false)
            xAxis.setDrawGridLines(false)
            legend.form = Legend.LegendForm.LINE
            description = null
        }

        saveValues.setOnClickListener {
            saveValues.isEnabled = false
            if (hasExternalStoragePermission) {
                viewModel.exportValues()
            } else {
                if (shouldShowRationale) {
                    val fm = fragmentManager ?: return@setOnClickListener
                    StoragePermissionRationaleDialogFragment()
                        .apply { setTargetFragment(this@MeasurementDetailFragment, EXPORT_VALUES_STORAGE_PERMISSION) }
                        .show(fm, STORAGE_PERMISSION_RATIONALE)
                } else {
                    requestStoragePermission(EXPORT_VALUES_STORAGE_PERMISSION)
                }
            }
        }

        saveGraph.setOnClickListener {
            saveGraph.isEnabled = false
            if (hasExternalStoragePermission) {
                viewModel.exportBitmap(graph.chartBitmap)
            } else {
                if (shouldShowRationale) {
                    val fm = fragmentManager ?: return@setOnClickListener
                    StoragePermissionRationaleDialogFragment()
                        .apply { setTargetFragment(this@MeasurementDetailFragment, EXPORT_CHART_STORAGE_PERMISSION) }
                        .show(fm, STORAGE_PERMISSION_RATIONALE)
                } else {
                    requestStoragePermission(EXPORT_CHART_STORAGE_PERMISSION)
                }
            }
        }
    }

    private fun requestStoragePermission(requestCode: Int) {
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size != 1) {
            return
        }
        saveGraph.isEnabled = true
        saveValues.isEnabled = true
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                EXPORT_CHART_STORAGE_PERMISSION -> viewModel.exportBitmap(graph.chartBitmap)
                EXPORT_VALUES_STORAGE_PERMISSION -> viewModel.exportValues()
            }
        } else if (!shouldShowRationale) {
            Snackbar
                .make(
                    measurementDetailContent,
                    R.string.measurement_detail_export_access_denied,
                    Snackbar.LENGTH_SHORT
                )
                .show()
        }

    }

    override fun onPositiveButtonClicked(requestCode: Int) {
        requestStoragePermission(requestCode)
    }

    override fun onNegativeButtonClicked(requestCode: Int) {
        saveGraph.isEnabled = true
        saveValues.isEnabled = true
    }
}
