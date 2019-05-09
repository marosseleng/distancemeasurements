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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marosseleng.distancemeasurements.*
import com.marosseleng.distancemeasurements.data.Measurement
import com.marosseleng.distancemeasurements.data.MeasurementType
import com.marosseleng.distancemeasurements.data.toMeasuredValueFactory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Maroš Šeleng
 */
class BluetoothMeasurementViewModel : ViewModel() {

    private val singleResults = MutableLiveData<ScanResult>()

    // emitting lists of results
    val devices = group(
        distinct(singleResults) { device?.address },
        distinctor = {
            it.device?.address
        },
        livelinessCriteria = {
            val currentResultTime = SystemClock.elapsedRealtimeNanos()
            (currentResultTime - it.timestampNanos) <= TWO_SECONDS_NANOS
        }
    )

    private val _measurementProgress = startWith<MeasurementProgress>(MeasurementProgress.NotStarted)
    val measurementInProgress: LiveData<MeasurementProgress>
        get() = _measurementProgress

    private var selectedDevice: ScanResult? = null

    val measuredValues = accumulateFromStart(emitWhile(map(singleResults, ScanResult::toMeasuredValueFactory)) {
        selectedDevice != null && _measurementProgress.value is MeasurementProgress.Started
    })

    private val mBluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = Application.instance.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            Timber.d("onScanFailed: $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            // TODO create MeasuredValue here!!
            if (result == null) {
                return
            }
            singleResults.postValue(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            Timber.d("onBatchScanResults: $results")
            // TODO create MeasuredValue here!!
            results?.forEach {
                singleResults.postValue(it)
            }
        }
    }

    // this field is redundant.. this functionality could be monitored using LiveData
    private val isScanning = AtomicBoolean(false)

    fun startScan() {
        if (!isScanning.getAndSet(true)) {
            val selectedDevice = selectedDevice
            val scanFilters: List<ScanFilter> = if (selectedDevice == null) {
                emptyList()
            } else {
                listOf(
                    ScanFilter.Builder()
                        .setDeviceAddress(selectedDevice.device.address)
                        .build()
                )
            }
            mBluetoothAdapter?.bluetoothLeScanner?.startScan(
                scanFilters,
                ScanSettings.Builder().build(),
                scanCallback
            )
        } else {
            Timber.e("Scan already in progress. WTF?")
        }
    }

    /**
     * Stops the scan performed by the bluetooth adapter
     */
    fun stopScan() {
        if (isScanning.getAndSet(false)) {
            mBluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } else {
            Timber.e("Can't stop the scan. Scan is not running.")
        }
    }

    fun selectDeviceForMeasurement(result: ScanResult) {
        stopScan()
        selectedDevice = result
        startScan()
    }

    fun cancelClicked() {
        // cancel measurement (if any is running) and delete record from DB
        if (_measurementProgress.value is MeasurementProgress.Started) {
        }
        _measurementProgress.postValue(MeasurementProgress.NotStarted)
        selectedDevice = null
        startScan()
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

    private fun save() {
        _measurementProgress.postValue(MeasurementProgress.Saving)
        val measurement = Measurement(
            deviceName = selectedDevice?.device?.name ?: "???",
            deviceAddress = selectedDevice?.device?.address ?: "???",
            measurementType = MeasurementType.BLE
        )
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _measurementProgress.postValue(MeasurementProgress.NotStarted)
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
}