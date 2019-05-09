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

/**
 * @author Maroš Šeleng
 */
@RequiresApi(Build.VERSION_CODES.P)
class WifiRttMeasurementViewModel : ViewModel() {

    private val _availableAps = MutableLiveData<List<ScanResult>>()
    val availableAps: LiveData<List<ScanResult>>
        get() = _availableAps

    private val _measurementProgress = startWith<MeasurementProgress>(MeasurementProgress.NotStarted)
    val measurementInProgress: LiveData<MeasurementProgress>
        get() = _measurementProgress

    private val singleResults = MutableLiveData<MeasuredValue.Factory>()
    val measuredValues = accumulateFromStart(emitWhile(singleResults) {
        _measurementProgress.value is MeasurementProgress.Started
    })

    private var scanResultsRefreshJob: Job? = viewModelScope.launch(Dispatchers.IO) {
        while (true) {
            Timber.d("==>Inside while, after Delay, thred: %s", Thread.currentThread().name)
            val results = wifiManager?.scanResults?.filter { it.is80211mcResponder } ?: emptyList()
            _availableAps.postValue(results)
            delay(30_000L)
        }
    }
    private var rangingJob: Job? = null

    private var selectedDevice: MeasurementAnchorDevice? = null
    private var rangingRequest: RangingRequest? = null
    private var samplingRate: Long = 1000L

    private val rangingCallback = object : RangingResultCallback() {
        override fun onRangingResults(results: MutableList<RangingResult>) {
            val result = results.firstOrNull() ?: return
            setMacAddress(result.macAddress)
            if (result.status == STATUS_SUCCESS) {
                singleResults.postValue(MeasuredValue.Factory(result.distanceMm, System.currentTimeMillis()))
            }
            Timber.d("==>onRangingResults: %s", results.joinToString("\n"))
        }

        override fun onRangingFailure(code: Int) {
            Timber.d("==onRangingFailure: %d", code)
        }
    }

    fun cancelClicked() {
        // cancel measurement (if any is running) and delete record from DB
        if (_measurementProgress.value is MeasurementProgress.Started) {
        }
        _measurementProgress.postValue(MeasurementProgress.NotStarted)
        Timber.d("==>cancel()")
        stopScan()
        Timber.d("==>Job cancelled")
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
        selectedDevice = scanResult.tiMeasurementAnchorDevice()
        rangingRequest = scanResult.toRangingRequest()
    }

    fun setSamplingRate(rate: Long) {
        samplingRate = rate
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
                delay(samplingRate)
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

    private fun setMacAddress(macAddress: MacAddress?) {
        if (selectedDevice?.address != macAddress?.toString()) {
            selectedDevice = selectedDevice?.copy(address = macAddress.toString())
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
fun ScanResult.toRangingRequest(): RangingRequest = RangingRequest.Builder().run {
    addAccessPoint(this@toRangingRequest)
    build()
}

fun ScanResult.tiMeasurementAnchorDevice(): MeasurementAnchorDevice {
    return MeasurementAnchorDevice(name = SSID, address = "NOT SET", deviceFrequency = frequency)
}