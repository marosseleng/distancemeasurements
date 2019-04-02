package com.marosseleng.distancemeasurements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Maroš Šeleng
 */

/**
 * Creates [MutableLiveData] and immediately posts the specified [firstValue]
 */
fun <T> startWith(firstValue: T): MutableLiveData<T> {
    return MutableLiveData<T>().apply {
        postValue(firstValue)
    }
}

/**
 * Notifies the subscriber only when the data emitted by [source] is different than the previous
 */
fun <T, R> distinct(source: LiveData<T>, distinctor: T.() -> R): LiveData<T> {
    return MediatorLiveData<T>().apply {
        var tmpValue: T? = null

        fun notify() {
            postValue(tmpValue)
        }

        addSource(source) {
            if (it?.distinctor() != tmpValue?.distinctor()) {
                tmpValue = it
                notify()
            }
        }
    }
}

fun <T, R> group(source: LiveData<T>, distinctor: (T) -> R, livelinessCriteria: (T) -> Boolean): LiveData<List<T>> {
    return MediatorLiveData<List<T>>().apply {

        val results: MutableList<T> = mutableListOf()

        addSource(source) { newResult: T ->
            val indexOfFound = results.indexOfFirst { distinctor(it) == distinctor(newResult) }
            if (indexOfFound != -1) {
                // this item is already in the list
                results.removeAt(indexOfFound)
            }
            results.add(newResult)

            results.removeAll { !livelinessCriteria(it) }

            postValue(results)
        }
    }
}

/**
 * Emit items from the [source] while [condition] is true
 * TODO create version accepting LiveData<Boolean>
 */
fun <T> emitWhile(source: LiveData<T>, condition: () -> Boolean): LiveData<T> {
    return MediatorLiveData<T>().apply {
        addSource(source) { newResult: T ->
            if (condition()) {
                postValue(newResult)
            }
        }
    }
}

fun <T> accumulateFromStart(source: LiveData<T>): LiveData<List<T>> {
    return MediatorLiveData<List<T>>().apply {
        val currentValues = mutableListOf<T>()

        addSource(source) { newItem: T ->
            currentValues.add(0, newItem)
            postValue(currentValues)
        }
    }
}

fun <T> diff(
    source: LiveData<List<T>>,
    callbackFactory: (oldList: List<T>, newList: List<T>) -> DiffUtil.Callback
): LiveData<Pair<List<T>, DiffUtil.DiffResult>> {
    return MediatorLiveData<Pair<List<T>, DiffUtil.DiffResult>>().apply {
        var lastValue = emptyList<T>()
        addSource(source) {
            val callback = callbackFactory(lastValue, it)
            lastValue = it
            GlobalScope.launch(Dispatchers.Main) {
                postValue(it to withContext(Dispatchers.Default) {
                    DiffUtil.calculateDiff(callback)
                })
            }
        }
    }
}

/**
 * Emits a value emitted by [source] only when the current value of [filter] is `true`
 */
fun <T> filter(source: LiveData<T>, filter: LiveData<Boolean>): LiveData<T> {
    return MediatorLiveData<T>().apply {
        addSource(source) { newResult: T ->
            if (filter.value == true) {
                postValue(newResult)
            }
        }
    }
}

/**
 * Emits a value emitted by [source] only when the current value of [filter] is `false`
 */
fun <T> filterNot(source: LiveData<T>, filter: LiveData<Boolean>): LiveData<T> {
    return MediatorLiveData<T>().apply {
        addSource(source) { newResult: T ->
            if (filter.value == false) {
                postValue(newResult)
            }
        }
    }
}