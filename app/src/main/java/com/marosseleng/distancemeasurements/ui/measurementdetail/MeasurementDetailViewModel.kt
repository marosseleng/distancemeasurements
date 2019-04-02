package com.marosseleng.distancemeasurements.ui.measurementdetail

import androidx.lifecycle.*
import com.marosseleng.distancemeasurements.Application
import com.marosseleng.distancemeasurements.data.MeasuredValue
import com.marosseleng.distancemeasurements.data.Measurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Typealias for a function that takes some sort of input and calculates the distance in meters
 */
typealias DistanceComputer = (Float) -> Double

/**
 * @author Maroš Šeleng
 */
class MeasurementDetailViewModel(val measurement: Measurement) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val job = Job()

    private val _values: MutableLiveData<List<MeasuredValue>> = MutableLiveData()
    /**
     * Values represent the distances from the APs in meters
     */
    val values: LiveData<List<MeasuredValue>>
        get() = _values

    val graphValues: LiveData<List<Pair<Double, Double>>> = Transformations.map(_values) {
        it.mapIndexed { index, measuredValue ->
            Pair(index.toDouble(), measuredValue.measuredValue.toDouble())
        }
    }

    private val distanceComputer: DistanceComputer = { 0.0 }
    // TODO livedata for statistical values

    init {
        launch {
            _values.value = Application.instance.database.dao().getMeasurementValues(measurement.id)
        }
    }

    fun setDistanceComputer(computer: DistanceComputer) {

    }

    class Factory(val measurement: Measurement) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MeasurementDetailViewModel(measurement) as T
        }
    }
}