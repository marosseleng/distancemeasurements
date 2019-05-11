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

package com.marosseleng.distancemeasurements.ui.positioning

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import com.marosseleng.distancemeasurements.R
import kotlinx.android.synthetic.main.fragment_positioning.*

/**
 * @author Maroš Šeleng
 */
class PositioningFragment : Fragment() {

    private lateinit var viewModel: PositioningViewModel

    private val knownAps = listOf(
        ApInSpace("COMPULAB_WILD", "d0:c6:37:d2:23:5f", 340 to 40)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_positioning, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }

        viewModel = ViewModelProviders.of(this).get()

        val knownMacAddresses = knownAps.map { it.macAddress }

        viewModel.measuredValues.observe(this, Observer { results ->
            val receivedDistances = results
                .filter { knownMacAddresses.contains(it.first) }
                .map { (macAddress, distanceCm) ->
                    knownAps.first { macAddress == it.macAddress } to distanceCm
                }.toMap()

            positioningView?.distances = receivedDistances +
                    (ApInSpace("Kniznica", "e:f:g:h", 0 to 350) to 180) +
                    (ApInSpace("Sedacka", "i:j:k:l", 450 to 350) to 320)
        })

        viewModel.startScan()
    }
}