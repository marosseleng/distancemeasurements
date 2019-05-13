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
import android.os.Environment
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.marosseleng.distancemeasurements.*
import com.marosseleng.distancemeasurements.data.MeasuredValue
import com.marosseleng.distancemeasurements.data.Measurement
import com.marosseleng.distancemeasurements.data.MeasurementType
import com.marosseleng.distancemeasurements.data.RealDistances
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MeasurementDetailViewModel(private val measurementId: Long) : ViewModel() {

    val measurement: LiveData<Measurement?> = dao.getMeasurementByIdLiveData(measurementId)

    /**
     * Values represent the distances from the APs in meters
     */
    val values: LiveData<List<MeasuredValue>>
        get() = dao.getMeasurementValuesLiveData(measurementId)

    private val _exportProgress = startWith<ExportProgress>(ExportProgress.NotStarted)
    val exportProgress: LiveData<ExportProgress>
        get() = _exportProgress

    private val realDistances: LiveData<RealDistances>
        get() = dao.getRealDistances(measurementId)

    private val graphValues: LiveData<List<Float>> = map(values) {
        it.map { measuredValue ->
            measuredValue.measuredValue.toFloat()
        }
    }

    val dataSets = combineLatest(measurement, graphValues, realDistances) { measurement, measured, real ->
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
            if (measurement?.measurementType == MeasurementType.RTT) {
                Pair(index.toFloat(), fl / 10f)
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

    fun saveEdits(note: String?, realDistances: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val distances = RealDistances().apply { parseUserInput(realDistances) }
            measurement.value?.copy(note = note, realDistances = distances)
                ?.let {
                    dao.updateMeasurement(it)
                }
        }
    }

    fun exportBitmap(bitmap: Bitmap) {
        _exportProgress.postValue(ExportProgress.Running)
        viewModelScope.launch(Dispatchers.IO) {
            val measurement = measurement.value ?: return@launch
            val fileName = getExportFileName(measurement)
            val targetDirectory = File(Environment.getExternalStorageDirectory().absolutePath + "/Distancemeasurements")
            if (!targetDirectory.exists()) {
                if (!targetDirectory.mkdirs()) {
                    _exportProgress.postValue(
                        ExportProgress.Failure(
                            IllegalStateException("Cannot create target directory for exports.")
                        )
                    )
                }
            }
            val targetFilePath = targetDirectory.absolutePath + "/" + fileName + ".png"
            val existingSimilarFiles = targetDirectory.listFiles { _, name ->
                name.contains("$fileName.png")
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

    fun exportValues() {
        _exportProgress.postValue(ExportProgress.Running)
        viewModelScope.launch(Dispatchers.IO) {
            val measurement = measurement.value ?: return@launch
            val fileName = getExportFileName(measurement)
            val targetDirectory = File(Environment.getExternalStorageDirectory().absolutePath + "/Distancemeasurements")
            if (!targetDirectory.exists()) {
                if (!targetDirectory.mkdirs()) {
                    _exportProgress.postValue(ExportProgress.Failure(IllegalStateException("Cannot create target directory for exports.")))
                }
            }
            val targetFilePath = targetDirectory.absolutePath + "/" + fileName + ".csv"
            val existingSimilarFiles = targetDirectory.listFiles { _, name ->
                name.contains("$fileName.csv")
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
                val targetFilePath2 = "${targetDirectory.absolutePath}/${fileName}_$nextFileNumberString.csv"
                File(targetFilePath2)
            } else {
                File(targetFilePath)
            }
            try {
                targetFile.writeText("Timestamp, Measured value, Computed distance(cm)\n" + prepareCsvText())
                _exportProgress.postValue(ExportProgress.Success(targetFile.toFileUri()))
            } catch (e: Exception) {
                _exportProgress.postValue(ExportProgress.Failure(e))
            }
        }
    }

    fun resetExportProgress() {
        _exportProgress.postValue(ExportProgress.NotStarted)
    }

    private fun calculateDistanceMeters(signalLevelInDb: Double, freqInMHz: Double = 2400.0): Double {
        val exp = (27.55 - 20 * Math.log10(freqInMHz) + Math.abs(signalLevelInDb)) / 20.0
        return Math.pow(10.0, exp)
    }

    /**
     * Centimeters
     */
    private val distanceComputer: (Int) -> Int = { (calculateDistanceMeters(it.toDouble()) * 100).toInt() }

    private fun getExportFileName(measurement: Measurement): String {
        val dateFormatted =
            DateUtils.formatDateTime(
                application,
                measurement.timestamp,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
            )
        return "${measurement.measurementType}-$dateFormatted-(${measurement.note ?: ""})"
    }

    private fun prepareCsvText() =
        values.value?.joinToString("\n") { "${it.timestamp}, ${it.measuredValue}, ${distanceComputer(it.measuredValue)}" }
            ?: ""

    class Factory(val measurementId: Long) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MeasurementDetailViewModel(measurementId) as T
        }
    }
}