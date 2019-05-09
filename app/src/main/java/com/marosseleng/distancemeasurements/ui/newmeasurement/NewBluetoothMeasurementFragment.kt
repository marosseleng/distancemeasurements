package com.marosseleng.distancemeasurements.ui.newmeasurement

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.marosseleng.distancemeasurements.R
import com.marosseleng.distancemeasurements.ui.MainActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_new_bluetooth_measurement.*
import kotlinx.android.synthetic.main.inner_measurement_setup.*

/**
 * @author Maroš Šeleng
 */
class NewBluetoothMeasurementFragment : Fragment() {

    private lateinit var viewModel: BluetoothMeasurementViewModel
    private val mBluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }
    private lateinit var selectedBeaconDescription: TextView
    private lateinit var beaconsAdapter: BeaconsAdapter
    private lateinit var valuesAdapter: RawMeasuredValueAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_new_bluetooth_measurement, container, false)
        selectedBeaconDescription = view.findViewById(R.id.anchorDescription)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get()

        beaconsAdapter = BeaconsAdapter {
            viewModel.stopScan()
            devicesWrapper.isVisible = false
            measurementWrapper.isVisible = true
            viewModel.selectDeviceForMeasurement(it)
            selectedBeaconDescription.text = it.device?.name
        }
        valuesAdapter = RawMeasuredValueAdapter()
        valuesAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                valueList.smoothScrollToPosition(0)
            }
        })

        bindViewModel()
        setupUi()
        viewModel.startScan()
    }

    override fun onStart() {
        super.onStart()
        val activity = activity ?: return
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            Toast.makeText(activity, R.string.new_bluetooth_permission_denied, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        } else if (mBluetoothAdapter?.isEnabled == false) {
            Toast.makeText(activity, R.string.new_bluetooth_bluetooth_off, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun bindViewModel() {
        viewModel.devices.observe(this, Observer {
            devicesListEmpty.isVisible = it.isEmpty()
            devicesList.isVisible = !devicesListEmpty.isVisible
            beaconsAdapter.beacons = it
        })
        viewModel.measuredValues.observe(this, Observer {
            if (it.isEmpty()) {
                noValues.isVisible = true
                valueList.isVisible = false
                valuesAdapter.clear()
            } else {
                noValues.isVisible = false
                valueList.isVisible = true
                valuesAdapter.addItem(it[0].measuredValue)
            }
        })
        viewModel.measurementInProgress.observe(this, Observer {
            startStop.isEnabled = true
            startStop.text = ""
            when (it) {
                is MeasurementProgress.NotStarted -> {
                    startStop.setText(R.string.general_measurement_startstop_start)
                }
                is MeasurementProgress.Started -> {
                    startStop.setText(R.string.general_measurement_startstop_stopsave)
                }
                is MeasurementProgress.Saving -> {
                    startStop.setText(R.string.general_measurement_startstop_saving)
                    startStop.isEnabled = false
                    cancel.isEnabled = false
                }
                is MeasurementProgress.Saved -> {
                    startStop.setText(R.string.general_measurement_startstop_saved)
                    startStop.isEnabled = false
                    val anchorView = (activity as? MainActivity)?.getBottomNavigation() ?: valueList
                    Snackbar.make(anchorView, R.string.general_measurement_snackbar_saved, Snackbar.LENGTH_SHORT)
                        .setAnchorView(anchorView)
                        .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar?>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                if (event != DISMISS_EVENT_ACTION && isAdded) {
                                    transientBottomBar?.view?.postDelayed({
                                        findNavController().navigateUp()
                                    }, 100)
                                } else {
                                    // view clicked
                                }
                            }
                        })
                        .setAction(R.string.general_measurement_snackbar_saved_view) {

                        }
                        .show()

                }
                is MeasurementProgress.NotSaved -> {
                    startStop.setText(R.string.general_measurement_startstop_not_saved)
                    startStop.isEnabled = true
                    cancel.isEnabled = false
                    Snackbar.make(bottomNavigation, R.string.general_measurement_snackbar_not_saved, Snackbar.LENGTH_SHORT)
                        .setAnchorView(bottomNavigation)
                        .setAction(R.string.general_measurement_snackbar_not_saved_retry) {
                            viewModel.retrySave()
                        }
                        .show()
                }
            }
            samplingRate.isEnabled = it is MeasurementProgress.NotStarted
        })
    }

    private fun setupUi() {
        with(devicesList) {
            adapter = beaconsAdapter
            addItemDecoration(DividerItemDecoration(activity, RecyclerView.VERTICAL))
        }
        with(valueList) {
            adapter = valuesAdapter
            addItemDecoration(DividerItemDecoration(activity, RecyclerView.VERTICAL))
        }
        cancel.setOnClickListener {
            devicesWrapper.isVisible = true
            measurementWrapper.isVisible = false
            valuesAdapter.clear()
            viewModel.cancelClicked()
        }

        startStop.setOnClickListener {
            startStop.isEnabled = false
            // TODO grab sampling rate!
            viewModel.startStopClicked()
        }
    }
}
