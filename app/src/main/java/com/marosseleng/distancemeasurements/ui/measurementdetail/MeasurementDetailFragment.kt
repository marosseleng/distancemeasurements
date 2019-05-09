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
import androidx.appcompat.app.AppCompatActivity
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
import com.marosseleng.distancemeasurements.tags.STORAGE_PERMISSION_RATIONALE
import kotlinx.android.synthetic.main.fragment_measurement_detail.*
import timber.log.Timber

/**
 * @author Maroš Šeleng
 */
class MeasurementDetailFragment : Fragment(), PositiveButtonClickedListener {

    private lateinit var viewModel: MeasurementDetailViewModel
    private lateinit var valuesAdapter: MeasuredValueAdapter
    private val args: MeasurementDetailFragmentArgs by navArgs()
    private val measurement: Measurement by lazy { args.measurement }

    private val hasExternalStoragePermission: Boolean by PermissionsDelegate(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val shouldShowRationale: Boolean by ShouldShowRationaleDelegate(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val exportFileName: String by lazy {
        val dateFormatted = DateUtils.formatDateTime(application, measurement.timestamp, FORMAT_SHOW_DATE or FORMAT_SHOW_TIME)
        "${measurement.measurementType}-$dateFormatted"
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

        viewModel = ViewModelProviders.of(this, MeasurementDetailViewModel.Factory(measurement))
            .get(MeasurementDetailViewModel::class.java)

        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title.text = measurement.timestamp.toString()

        valuesAdapter = MeasuredValueAdapter()

        bindViewModel()
        setupUI()
    }

    private fun bindViewModel() {
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
                }
                is ExportProgress.Running -> {
                    saveGraph.isEnabled = false
                }
                is ExportProgress.Failure -> {
                    val cause = it.cause
                    Timber.e(cause)
                    Snackbar.make(measurementDetailContent, R.string.measurement_detail_graph_export_failure, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.measurement_detail_graph_export_failure_retry) {
                            viewModel.exportBitmap(graph.chartBitmap, exportFileName)
                        }
                        .show()
                    viewModel.resetExportProgress()
                }
                is ExportProgress.Success -> {
                    val uri = it.fileUri
                    Snackbar.make(measurementDetailContent, R.string.measurement_detail_graph_export_success, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.measurement_detail_graph_export_success_view) {
                            startActivity(Intent(ACTION_VIEW).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                        }
                        .show()
                    viewModel.resetExportProgress()
                }
            }
        })
    }

    private fun setupUI() {
        with(list) {
            adapter = valuesAdapter
        }

        realDistances.editText?.setText(measurement.realDistances.parsedValues)
        note.editText?.setText(measurement.note)

        saveEdits.setOnClickListener {
            viewModel.saveEdits(note.editText?.text?.toString(), realDistances.editText?.text?.toString())
        }

        with(graph) {
            xAxis.setDrawLabels(false)
            xAxis.setDrawGridLines(false)
            legend.form = Legend.LegendForm.LINE
            description = null
        }

        saveGraph.setOnClickListener {
            saveGraph.isEnabled = false
            if (hasExternalStoragePermission) {
                viewModel.exportBitmap(graph.chartBitmap, exportFileName)
            } else {
                // TODO check rationale!
                requestStoragePermission()
            }
        }
    }

    private fun requestStoragePermission() {
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), EXPORT_CHART_STORAGE_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            EXPORT_CHART_STORAGE_PERMISSION -> {
                if (grantResults.size != 1) {
                    return
                }
                saveGraph.isEnabled = true
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.exportBitmap(graph.chartBitmap, exportFileName)
                } else if (shouldShowRationale) {
                    val fm = fragmentManager ?: return
                    StoragePermissionRationaleDialogFragment()
                        .apply { setTargetFragment(this@MeasurementDetailFragment, 0) }
                        .show(fm, STORAGE_PERMISSION_RATIONALE)
                } else {
                    Snackbar.make(measurementDetailContent, R.string.measurement_detail_storage_access_denied, Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onPositiveButtonClicked(requestCode: Int) {
        requestStoragePermission()
    }
}
