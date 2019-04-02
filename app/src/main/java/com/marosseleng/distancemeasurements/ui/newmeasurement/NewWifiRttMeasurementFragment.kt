package com.marosseleng.distancemeasurements.ui.newmeasurement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.marosseleng.distancemeasurements.R

/**
 * @author Maroš Šeleng
 */
class NewWifiRttMeasurementFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_wifi_rtt_measurement, container, false)
    }
}