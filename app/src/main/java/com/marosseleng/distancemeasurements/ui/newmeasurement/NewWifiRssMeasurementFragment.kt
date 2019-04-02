package com.marosseleng.distancemeasurements.ui.newmeasurement

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.marosseleng.distancemeasurements.ImplementedTextWatcher
import com.marosseleng.distancemeasurements.R
import com.marosseleng.distancemeasurements.ui.MainActivity
import com.marosseleng.distancemeasurements.wifiManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_new_wifi_rss_measurement.*
import kotlinx.android.synthetic.main.inner_measurement_setup.*
import timber.log.Timber

/**
 * @author Maroš Šeleng
 */
class NewWifiRssMeasurementFragment : Fragment() {

    private lateinit var viewModel: WifiRssMeasurementViewModel
    private lateinit var valuesAdapter: RawMeasuredValueAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_new_wifi_rss_measurement, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        methodIcon.setImageDrawable(context?.getDrawable(R.drawable.ic_wifi_black_24dp))
        anchorDescription.text = wifiManager?.connectionInfo?.ssid
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get()

        valuesAdapter = RawMeasuredValueAdapter()
        // TODO uncomment?
//        valuesAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
//            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
//                super.onItemRangeInserted(positionStart, itemCount)
//                valueList.smoothScrollToPosition(0)
//            }
//        })

        bindViewModel()
        setupUi()
    }

    private fun bindViewModel() {
        viewModel.measuredValues.observe(this, Observer {
            Timber.d("==>Observing: %s", it)
            if (it.isEmpty()) {
                noValues.isVisible = true
                valueList.isVisible = false
                valuesAdapter.clear()
            } else {
                noValues.isVisible = false
                valueList.isVisible = true
                valuesAdapter.addItem(it[0].measuredValue.toInt())
            }
        })

        viewModel.measurementInProgress.observe(this, Observer {
            startStop.isEnabled = true
            startStop.text = ""
            when (it) {
                is MeasurementProgress.NotStarted -> {
                    startStop.text = "Start"
                }
                is MeasurementProgress.Started -> {
                    startStop.text = "Stop & Save"
                }
                is MeasurementProgress.Saving -> {
                    startStop.text = "Saving…"
                    startStop.isEnabled = false
                    cancel.isEnabled = false
                }
                is MeasurementProgress.Saved -> {
                    startStop.text = "Saved"
                    startStop.isEnabled = false
                    val anchorView = (activity as? MainActivity)?.getBottomNavigation() ?: valueList
                    Snackbar.make(anchorView, "Measurement saved", Snackbar.LENGTH_SHORT)
                        .setAnchorView(anchorView)
                        .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar?>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                if (event != DISMISS_EVENT_ACTION) {
                                    transientBottomBar?.view?.postDelayed({
                                        findNavController().navigateUp()
                                    }, 100)
                                } else {
                                    // view clicked
                                }
                            }
                        })
                        .setAction("View") {

                        }
                        .show()

                }
                is MeasurementProgress.NotSaved -> {
                    startStop.text = "Not saved"
                    startStop.isEnabled = true
                    cancel.isEnabled = false
                    Snackbar.make(bottomNavigation, "Measurement saved", Snackbar.LENGTH_SHORT)
                        .setAnchorView(bottomNavigation)
                        .setAction("Retry") {
                            viewModel.retrySave()
                        }
                        .show()
                }
            }
            samplingRate.isEnabled = it is MeasurementProgress.NotStarted
        })
    }

    private fun setupUi() {
        valueList.adapter = valuesAdapter
        valueList.addItemDecoration(DividerItemDecoration(activity, RecyclerView.VERTICAL))
        note.editText?.addTextChangedListener(object : ImplementedTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                viewModel.noteChanged(s?.toString())
            }
        })
        cancel.setOnClickListener {
            viewModel.cancelClicked()
            findNavController().navigateUp()
        }
        startStop.setOnClickListener {
            startStop.isEnabled = false
            cancel.isEnabled = false
            viewModel.startStopClicked()
        }
    }

    override fun onResume() {
        super.onResume()
        updateViewVisibilities()
    }

    private fun updateViewVisibilities() {
        measurementWrapper.isVisible = wifiManager?.connectionInfo != null
        noWifi.isVisible = !measurementWrapper.isVisible
    }
}