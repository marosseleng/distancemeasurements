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

package com.marosseleng.distancemeasurements.ui.newmeasurement

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.EXTRA_NETWORK_INFO
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marosseleng.distancemeasurements.*
import com.marosseleng.distancemeasurements.data.MeasuredValue
import com.marosseleng.distancemeasurements.data.Measurement
import com.marosseleng.distancemeasurements.data.MeasurementAnchorDevice
import com.marosseleng.distancemeasurements.data.MeasurementType
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * @author Maroš Šeleng
 */
class WifiRssMeasurementViewModel : ViewModel() {

    private companion object {
        const val DEFAULT_SAMPLING_RATE = 100L
    }

    private val _measurementProgress = startWith<MeasurementProgress>(MeasurementProgress.NotStarted)
    val measurementInProgress: LiveData<MeasurementProgress>
        get() = _measurementProgress

    private val singleResults = MutableLiveData<MeasuredValue.Factory>()
    val measuredValues = accumulateFromStart(emitWhile(singleResults) {
        _measurementProgress.value is MeasurementProgress.Started
    })

    var samplingRateMillis: Long = DEFAULT_SAMPLING_RATE
        set(value) {
            field = if (value < 0) DEFAULT_SAMPLING_RATE else value
        }

    private var measurementJob: Job? = null

    private var selectedDevice: MeasurementAnchorDevice? = null

    private var wifiReceiver: WifiReceiver? = null

    fun cancelClicked() {
        // cancel measurement (if any is running) and delete record from DB
        if (_measurementProgress.value is MeasurementProgress.Started) {
        }
        _measurementProgress.postValue(MeasurementProgress.NotStarted)
        stopScan()
        selectedDevice = null
    }

    fun startStopClicked() {
        when (_measurementProgress.value) {
            MeasurementProgress.NotStarted -> startMeasurement()
            MeasurementProgress.Started -> stopMeasurementAndSave()
            else -> {
                // TODO what to do here?
            }
        }
    }

    fun retrySave() {
        save()
    }

    private fun startMeasurement() {
        if (_measurementProgress.value is MeasurementProgress.Started) {
            cancelClicked()
        }

        _measurementProgress.postValue(MeasurementProgress.Started)
        startScan()
    }

    private fun stopMeasurementAndSave() {
        stopScan()
        save()
    }

    /**
     * part of the public API
     */
    private fun startScan() {
        if (wifiReceiver == null) {
            wifiReceiver = WifiReceiver()
            application.registerReceiver(wifiReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
        } else {
            Timber.w("wifiReceiver is not null, is this weird? how did this happen?")
        }

        selectedDevice = wifiManager?.connectionInfo?.toMeasurementAnchorDevice()

        val job = measurementJob
        if (job != null) {
            if (job.isActive) {
                job.cancel()
            }
            measurementJob = null
            throw IllegalStateException("Job is nn.")
        }
        measurementJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val info = wifiManager?.connectionInfo
                if (info != null) {
                    singleResults.postValue(MeasuredValue.Factory(info.rssi, System.currentTimeMillis()))
                }
                delay(samplingRateMillis)
            }
        }
    }

    private fun stopScan() {
        measurementJob?.cancel()
        measurementJob = null
    }

    private fun save() {
        _measurementProgress.postValue(MeasurementProgress.Saving)
        val measurement = Measurement(
            deviceName = selectedDevice?.name ?: "???",
            deviceAddress = selectedDevice?.address ?: "???",
            measurementType = MeasurementType.RSSI,
            frequency = selectedDevice?.deviceFrequency ?: 2401
        )
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            Timber.e(throwable)
            _measurementProgress.postValue(MeasurementProgress.NotSaved)
        }) {
            val measurementId = withContext(Dispatchers.IO) {
                dao.insertMeasurement(measurement)
            }

            withContext(Dispatchers.IO) {
                val valuesToSave = measuredValues.value?.map { it()(measurementId) }?.toTypedArray()
                if (valuesToSave != null) {
                    dao.insertMeasuredValues(*valuesToSave)
                }
            }
            _measurementProgress.postValue(MeasurementProgress.Saved)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (wifiReceiver != null) {
            application.unregisterReceiver(wifiReceiver)
            wifiReceiver = null
        }
        cancelClicked()
    }

    private inner class WifiReceiver : BroadcastReceiver() {
        @SuppressLint("HardwareIds")
        override fun onReceive(context: Context?, intent: Intent?) {
            val newNetworkInfo = intent?.getParcelableExtra<NetworkInfo>(EXTRA_NETWORK_INFO)
            val newConnInfo = wifiManager?.connectionInfo
            if (selectedDevice == null) {
                Timber.w("Network state changed, but the selected device is null so doing nothing.")
                return
            }
            if (selectedDevice?.name != newConnInfo?.ssid || selectedDevice?.address != newConnInfo?.macAddress) {
                Timber.e(
                    "Network state changed!\nNew connection info: %s\nNew network info:%s",
                    newConnInfo,
                    newNetworkInfo
                )
                cancelClicked()
            } else {

            }
        }
    }
}
