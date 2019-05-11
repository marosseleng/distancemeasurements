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
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.marosseleng.distancemeasurements.ImplementedTextWatcher
import com.marosseleng.distancemeasurements.R
import kotlinx.android.synthetic.main.fragment_positioning.*

/**
 * @author Maroš Šeleng
 */
class PositioningFragment : Fragment() {

    private companion object {
        const val STATE_ITEMS = "items"
        const val STATE_POSITIONING_VISIBLE = "positioning_mode"
        val COMPULAB_AP = ApInSpace("COMPULAB_WILD", "d0:c6:37:d2:23:5f", 340 to 40)
    }

    private lateinit var viewModel: PositioningViewModel
    private lateinit var apAdapter: PositioningApAdapter
    private lateinit var knownAps: MutableList<ApInSpace>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_positioning, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }

        positioningView?.isVisible = savedInstanceState?.getBoolean(STATE_POSITIONING_VISIBLE) == true
        apListContent?.isVisible = positioningView?.isVisible == false

        knownAps = savedInstanceState?.getParcelableArray(STATE_ITEMS)
            ?.mapNotNull { it as? ApInSpace }
            ?.toMutableList()
            ?: mutableListOf()

        if (!knownAps.contains(COMPULAB_AP)) {
            knownAps.add(COMPULAB_AP)
        }

        apAdapter = PositioningApAdapter(knownAps.toMutableList())

        apList?.addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
        apList?.adapter = apAdapter

        viewModel = ViewModelProviders.of(this).get()

        viewModel.measuredValues.observe(this, Observer { results ->
            val knownMacAddresses = knownAps.map { it.macAddress }
            val receivedDistances = results
                .filter { knownMacAddresses.contains(it.first) }
                .map { (macAddress, distanceCm) ->
                    knownAps.first { macAddress == it.macAddress } to distanceCm
                }.toMap()

            positioningView?.distances = receivedDistances
        })

        setupUi()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArray(STATE_ITEMS, knownAps.toTypedArray())
        outState.putBoolean(STATE_POSITIONING_VISIBLE, positioningView?.isVisible == true)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupUi() {
        invalidateButtons()
        address?.editText?.addTextChangedListener(object : ImplementedTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                invalidateButtons()
            }
        })
        name?.editText?.addTextChangedListener(object : ImplementedTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                invalidateButtons()
            }
        })
        positionX?.editText?.addTextChangedListener(object : ImplementedTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                invalidateButtons()
            }
        })
        positionY?.editText?.addTextChangedListener(object : ImplementedTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                invalidateButtons()
            }
        })
        add?.setOnClickListener {
            add?.isEnabled = false
            val addressText = (address?.editText?.text?.toString() ?: "")
            val nameText = name?.editText?.text?.toString()
            val xText = (positionX?.editText?.text?.toString() ?: "")
            val yText = (positionY?.editText?.text?.toString() ?: "")

            val newItem = ApInSpace(nameText, addressText, (xText.toIntOrNull() ?: 0) to (yText.toIntOrNull() ?: 0))
            apAdapter.addItem(newItem)
            knownAps.add(0, newItem)

            invalidateButtons()
            clearFields()
        }
        done?.setOnClickListener {
            done?.isEnabled = false
            clearFields()
            invalidateButtons()
            viewModel.startScan()
            positioningView?.isVisible = true
            apListContent?.isVisible = false
        }
    }

    private fun invalidateButtons() {
        val addressText = (address?.editText?.text?.toString() ?: "")
        val xText = (positionX?.editText?.text?.toString() ?: "")
        val yText = (positionY?.editText?.text?.toString() ?: "")

        val addEnabled = addressText.isNotEmpty() && xText.isNotEmpty() && yText.isNotEmpty()
        add?.isEnabled = addEnabled
        val doneEnabled = apAdapter.itemCount >= 3
        done?.isEnabled = doneEnabled
    }

    private fun clearFields() {
        address?.editText?.text?.clear()
        name?.editText?.text?.clear()
        positionX?.editText?.text?.clear()
        positionY?.editText?.text?.clear()
    }
}