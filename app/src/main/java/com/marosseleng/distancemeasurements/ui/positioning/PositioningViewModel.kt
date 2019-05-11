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

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.wifi.ScanResult
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResult.STATUS_SUCCESS
import android.net.wifi.rtt.RangingResultCallback
import android.os.AsyncTask
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marosseleng.distancemeasurements.application
import com.marosseleng.distancemeasurements.wifiManager
import com.marosseleng.distancemeasurements.wifiRttManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @author Maroš Šeleng
 */
@RequiresApi(Build.VERSION_CODES.P)
class PositioningViewModel : ViewModel() {

    private val _measuredValues = MutableLiveData<List<Pair<String, Int>>>()
    val measuredValues: LiveData<List<Pair<String, Int>>>
        get() = _measuredValues

    private val availableAps = MutableLiveData<List<ScanResult>>()
    private var rangingJob: Job? = null

    private val rangingCallback = object : RangingResultCallback() {
        override fun onRangingResults(results: MutableList<RangingResult>) {
            val value = results
                .filter { it.status == STATUS_SUCCESS }
                .map { it.macAddress.toString() to (it.distanceMm / 10) }
            _measuredValues.postValue(value)
        }

        override fun onRangingFailure(code: Int) {
            Timber.e("==>onRangingFailure: %d", code)
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val results = wifiManager?.scanResults?.filter { it.is80211mcResponder } ?: emptyList()
                availableAps.postValue(results)
                delay(30_000L)
            }
        }
    }

    fun startScan() {
        val job = rangingJob
        if (job != null && job.isActive) {
            job.cancel()
        }
        rangingJob = viewModelScope.launch {
            while (true) {
                val availableAps = availableAps.value
                if (availableAps == null || availableAps.isEmpty()) {
                    delay(300)
                    continue
                }
                val request = RangingRequest.Builder()
                    .run {
                        addAccessPoints(availableAps)
                    }
                    .build()

                if (checkSelfPermission(application, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                    wifiRttManager?.startRanging(request, AsyncTask.THREAD_POOL_EXECUTOR, rangingCallback)
                } else {
                    // TODO handle permission rejection
                }
                delay(300) // TODO is it good or bad or wtf
            }
        }
    }

    fun stopScan() {
        rangingJob?.cancel()
        rangingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
//
//    private fun getKalmanFilter(macAddress: String, initialValue: Int): KalmanFilter? {
//        synchronized(filtersLock) {
//            if (kalmanFilters[macAddress] == null) {
//                val (kf, h) = createKalmanFilter(initialValue)
//                kalmanFilters[macAddress] = kf
//                if (kalmanFilters_H[macAddress] == null) {
//                    kalmanFilters_H[macAddress] = h
//                }
//            }
//        }
//
//        return kalmanFilters[macAddress]
//    }
//
//    private fun createKalmanFilter(initialValue: Int): Pair<KalmanFilter, RealMatrix> {
////        val measurementNoise = 0.1
////        val processNoise = 1e-5
//
//        // A = [ 1 ]
//        val A = Array2DRowRealMatrix(doubleArrayOf(1.0))
//        // B = null
//        val B: RealMatrix? = null
//        // H = [ 1 ]
//        val H = Array2DRowRealMatrix(doubleArrayOf(1.0))
//        // x = [ 10 ]
//        var x: RealVector = ArrayRealVector(doubleArrayOf(initialValue.toDouble()))
//        // Q = [ 1e-5 ]
//        val Q = Array2DRowRealMatrix(doubleArrayOf(1.0))
////        val Q: RealMatrix? = null
//        // P = [ 1 ]
//        val P0 = Array2DRowRealMatrix(doubleArrayOf(1.0))
//        // R = [ 0.1 ]
//        val R = Array2DRowRealMatrix(doubleArrayOf(1.0))
////        val R: RealMatrix? = null
//
//        val pm = DefaultProcessModel(A, B, Q, x, P0)
//        val mm = DefaultMeasurementModel(H, R)
//
//        return Pair(KalmanFilter(pm, mm), H)
//    }
}