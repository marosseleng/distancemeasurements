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
