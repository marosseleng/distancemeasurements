package com.marosseleng.distancemeasurements.ui.newmeasurement

import android.net.wifi.ScanResult
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
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
import kotlinx.android.synthetic.main.fragment_new_wifi_rtt_measurement.*
import kotlinx.android.synthetic.main.inner_measurement_setup.*
import kotlinx.android.synthetic.main.item_beacon.view.*

/**
 * @author Maroš Šeleng
 */
@RequiresApi(Build.VERSION_CODES.P)
class NewWifiRttMeasurementFragment : Fragment() {

    private lateinit var viewModel: WifiRttMeasurementViewModel
    private lateinit var apsAdapter: RttApAdapter
    private lateinit var valuesAdapter: RawMeasuredValueAdapter
    private lateinit var selectedBeaconDescription: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_new_wifi_rtt_measurement, container, false)
        selectedBeaconDescription = view.findViewById(R.id.anchorDescription)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get()

        apsAdapter = RttApAdapter {
            viewModel.setSelectedDevice(it)
            devicesWrapper.isVisible = false
            measurementWrapper.isVisible = true
            selectedBeaconDescription.text = it.SSID
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
    }

    private fun bindViewModel() {
        viewModel.availableAps.observe(this, Observer {
            devicesListEmpty.isVisible = it.isEmpty()
            devicesList.isVisible = !devicesListEmpty.isVisible
            apsAdapter.aps = it
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
                    Snackbar.make(
                        bottomNavigation,
                        R.string.general_measurement_snackbar_not_saved,
                        Snackbar.LENGTH_SHORT
                    )
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
            adapter = apsAdapter
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
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.container
        val title: TextView = itemView.anchorDescription
    }
}