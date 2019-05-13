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

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.MacAddress
import android.net.wifi.ScanResult
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResult.STATUS_SUCCESS
import android.net.wifi.rtt.RangingResultCallback
import android.os.AsyncTask
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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

@RequiresApi(Build.VERSION_CODES.P)
class WifiRttMeasurementViewModel : ViewModel() {

    private companion object {
        const val DEFAULT_SAMPLING_RATE = 100L
    }

    private val _availableAps = MutableLiveData<List<ScanResult>>()
    val availableAps: LiveData<List<ScanResult>>
        get() = _availableAps

    private val _measurementProgress = startWith<MeasurementProgress>(MeasurementProgress.NotStarted)
    val measurementInProgress: LiveData<MeasurementProgress>
        get() = _measurementProgress

    private val singleResults = MutableLiveData<MeasuredValue.Factory>()
    val measuredValues = accumulateAtStart(emitWhile(singleResults) {
        _measurementProgress.value is MeasurementProgress.Started
    })

    private var scanResultsRefreshJob: Job? = viewModelScope.launch(Dispatchers.IO) {
        while (true) {
            val results = wifiManager?.scanResults?.filter { it.is80211mcResponder } ?: emptyList()
            _availableAps.postValue(results)
            delay(30_000L)
        }
    }
    private var rangingJob: Job? = null

    private var selectedDevice: MeasurementAnchorDevice? = null
    private var rangingRequest: RangingRequest? = null
    var samplingRateMillis: Long = 1000L
        set(value) {
            field = if (value < 0) DEFAULT_SAMPLING_RATE else value
        }

    private val rangingCallback = object : RangingResultCallback() {
        override fun onRangingResults(results: MutableList<RangingResult>) {
            val result = results.firstOrNull() ?: return
            setMacAddress(result.macAddress)
            if (result.status == STATUS_SUCCESS) {
                singleResults.postValue(MeasuredValue.Factory(result.distanceMm, System.currentTimeMillis()))
            }
        }

        override fun onRangingFailure(code: Int) {
            Timber.w("==onRangingFailure: %d", code)
        }
    }

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

    fun setSelectedDevice(scanResult: ScanResult) {
        selectedDevice = scanResult.toMeasurementAnchorDevice()
        rangingRequest = scanResult.toRangingRequest()
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

    private fun startScan() {
        val job = rangingJob
        if (job != null && job.isActive) {
            job.cancel()
        }
        val request = rangingRequest ?: return
        rangingJob = viewModelScope.launch {
            while (true) {
                if (ContextCompat.checkSelfPermission(
                        application,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PERMISSION_GRANTED
                ) {
                    wifiRttManager?.startRanging(request, AsyncTask.THREAD_POOL_EXECUTOR, rangingCallback)
                } else {
                    // TODO handle permission rejection
                }
                delay(samplingRateMillis)
            }
        }
    }

    private fun stopScan() {
        rangingJob?.cancel()
        rangingJob = null
    }

    private fun save() {
        _measurementProgress.postValue(MeasurementProgress.Saving)
        val measurement = Measurement(
            deviceName = selectedDevice?.name ?: "???",
            deviceAddress = selectedDevice?.address ?: "???",
            measurementType = MeasurementType.RTT,
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

        stopScan()
    }

    private fun setMacAddress(macAddress: MacAddress?) {
        if (selectedDevice?.address != macAddress?.toString()) {
            selectedDevice = selectedDevice?.copy(address = macAddress.toString())
        }
    }
}