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

package com.marosseleng.distancemeasurements.ui.measurementdetail

import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.map
import com.marosseleng.distancemeasurements.*
import com.marosseleng.distancemeasurements.data.MeasuredValue
import com.marosseleng.distancemeasurements.data.Measurement
import com.marosseleng.distancemeasurements.data.MeasurementType
import com.marosseleng.distancemeasurements.data.RealDistances
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Typealias for a function that takes some sort of input and calculates the distance in centimeters
 */
typealias DistanceComputer = (Int) -> Int

sealed class ExportProgress {
    object NotStarted : ExportProgress()
    object Running : ExportProgress()
    data class Success(val fileUri: Uri) : ExportProgress()
    data class Failure(val cause: Throwable) : ExportProgress()
}

/**
 * @author Maroš Šeleng
 */
class MeasurementDetailViewModel(private val measurement: Measurement) : ViewModel() {

    private val _values: MutableLiveData<List<MeasuredValue>> = MutableLiveData()
    /**
     * Values represent the distances from the APs in meters
     */
    val values: LiveData<List<MeasuredValue>>
        get() = _values

    private val graphValues: LiveData<List<Float>> = map(_values) {
        it.map { measuredValue ->
            measuredValue.measuredValue.toFloat()
        }
    }

    private val _exportProgress = startWith<ExportProgress>(ExportProgress.NotStarted)
    val exportProgress: LiveData<ExportProgress>
        get() = _exportProgress

    private val realDistances: LiveData<RealDistances>
        get() = dao.getRealDistances(measurement.id)

    val dataSets = combineLatest(graphValues, realDistances) { measured, real ->
        var rawRealList = real.asList()
        if (rawRealList.size > measured.size) {
            rawRealList = rawRealList.dropLast(rawRealList.size - measured.size)
        }
        val realList = if (rawRealList.size == 1) {
            (rawRealList + rawRealList.first())
        } else {
            rawRealList
        }
        val measuredList = if (measured.size == 1) {
            measured + measured.first()
        } else {
            measured
        }
        val outMin = 0
        val outMax = measuredList.lastIndex
        val inMin = 0
        val inMax = realList.lastIndex
        val measuredMapped = measuredList.mapIndexed { index, fl ->
            if (measurement.measurementType == MeasurementType.RTT) {
                Pair(index.toFloat(), fl)
            } else {
                Pair(index.toFloat(), distanceComputer(fl.toInt()).toFloat())
            }
        }
        val realMapped = realList.mapIndexed { index, i ->
            val newXVal = (((index - inMin) * (outMax - outMin)) / (inMax - inMin + outMin)).toFloat()
            Pair(newXVal, i.toFloat())
        }
        Pair(measuredMapped, realMapped)
    }

    private fun calculateDistance(signalLevelInDb: Double, freqInMHz: Double = 2400.0): Double {
        val exp = (27.55 - 20 * Math.log10(freqInMHz) + Math.abs(signalLevelInDb)) / 20.0
        return Math.pow(10.0, exp)
    }

    private val distanceComputer: DistanceComputer = { (calculateDistance(it.toDouble()) * 100).toInt()  }
    // TODO livedata for statistical values

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _values.postValue(Application.instance.database.dao().getMeasurementValues(measurement.id))
        }
    }

    fun setDistanceComputer(computer: DistanceComputer) {

    }

    fun exportBitmap(bitmap: Bitmap, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetDirectory = File(Environment.getExternalStorageDirectory().absolutePath + "/Distancemeasurements")
            if (!targetDirectory.exists()) {
                if (!targetDirectory.mkdirs()) {
                    _exportProgress.postValue(ExportProgress.Failure(IllegalStateException("Cannot create target directory for exports.")))
                }
            }
            val targetFilePath = targetDirectory.absolutePath + "/" + fileName + ".png"
            val existingSimilarFiles = targetDirectory.listFiles { _, name ->
                name.contains(fileName)
            }

            val targetFile: File = if (existingSimilarFiles.isNotEmpty()) {
                // we need to find the new filename
                val lastFileNumber = existingSimilarFiles
                    .mapNotNull {
                        it.nameWithoutExtension.split('_').getOrNull(1)?.toIntOrNull()
                    }
                    .sortedDescending()
                    .firstOrNull() ?: 0

                val nextFileNumberString = "${lastFileNumber + 1}"
                val targetFilePath2 = "${targetDirectory.absolutePath}/${fileName}_$nextFileNumberString.png"
                File(targetFilePath2)
            } else {
                File(targetFilePath)
            }
            targetFile.outputStream().use {
                try {
                    val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    if (success) {
                        _exportProgress.postValue(ExportProgress.Success(targetFile.toFileUri()))
                    } else {
                        _exportProgress.postValue(ExportProgress.Failure(Exception()))
                    }
                } catch (e: Exception) {
                    _exportProgress.postValue(ExportProgress.Failure(e))
                }
            }
        }
    }

    fun resetExportProgress() {
        _exportProgress.postValue(ExportProgress.NotStarted)
    }

    fun saveEdits(note: String?, realDistances: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val distances = RealDistances().apply { parseUserInput(realDistances) }
            dao.updateMeasurement(measurement.copy(note = note, realDistances = distances))
        }
    }

    class Factory(val measurement: Measurement) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MeasurementDetailViewModel(measurement) as T
        }
    }
}