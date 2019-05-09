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

package com.marosseleng.distancemeasurements.ui.measurements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.marosseleng.distancemeasurements.Application
import com.marosseleng.distancemeasurements.data.Measurement
import com.marosseleng.distancemeasurements.data.MeasurementType
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MeasurementsViewModel : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val job = Job()

    init {
        val dao = Application.instance.database.dao()
        launch {
            _measurements.postValue(dao.getAllMeasurements())
        }
    }

    private val _measurements = MutableLiveData<List<Measurement>>()
    val measurements: LiveData<List<Measurement>>
        get() = _measurements

    fun setMeasurementTypeFilter(position: Int) {
        val type = MeasurementType.values().find { it.ordinal == position }
        val dao = Application.instance.database.dao()
        launch {
            if (type != null) {
                _measurements.postValue(dao.getMeasurementsByType(type.ordinal))
            } else {
                _measurements.postValue(dao.getAllMeasurements())
            }
        }
    }
}
