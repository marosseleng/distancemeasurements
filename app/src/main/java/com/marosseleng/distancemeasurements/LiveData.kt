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

package com.marosseleng.distancemeasurements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

/**
 * Continuously adds an item emitted by [source] to the list (if not already present) and emits this list
 */
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

/**
 * Accumulates the values emitted by [source] and emits the the accumulated list with the newest value at position 0
 */
fun <T> accumulateAtStart(source: LiveData<T>): LiveData<List<T>> {
    return MediatorLiveData<List<T>>().apply {
        val currentValues = mutableListOf<T>()

        addSource(source) { newItem: T ->
            currentValues.add(0, newItem)
            postValue(currentValues)
        }
    }
}

/**
 * Returns the live data containing the Pair of the new value and the diff between the old and the new value
 */
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

/**
 * Monitors the emissions of [source1] and [source2], once both sources emits non-null value, it emits the pair of them
 */
fun <S, T> combineLatest(source1: LiveData<S>, source2: LiveData<T>): LiveData<Pair<S, T>> {
    return MediatorLiveData<Pair<S, T>>().apply {
        var lastFromSource1: S? = null
        var lastFromSource2: T? = null
        fun notify() {
            val tmp1 = lastFromSource1
            val tmp2 = lastFromSource2
            if (tmp1 != null && tmp2 != null) {
                postValue(tmp1 to tmp2)
            }
        }
        addSource(source1) {
            lastFromSource1 = it
            notify()
        }
        addSource(source2) {
            lastFromSource2 = it
            notify()
        }
    }
}

/**
 * Monitors the emissions of [source1] and [source2] [source3], once both sources emits non-null value, it emits the pair of them
 */
fun <S, T, U> combineLatest(
    source1: LiveData<S>,
    source2: LiveData<T>,
    source3: LiveData<U>
): LiveData<Triple<S, T, U>> {
    return MediatorLiveData<Triple<S, T, U>>().apply {
        var lastFromSource1: S? = null
        var lastFromSource2: T? = null
        var lastFromSource3: U? = null
        fun notify() {
            val tmp1 = lastFromSource1
            val tmp2 = lastFromSource2
            val tmp3 = lastFromSource3
            if (tmp1 != null && tmp2 != null && tmp3 != null) {
                postValue(Triple(tmp1, tmp2, tmp3))
            }
        }
        addSource(source1) {
            lastFromSource1 = it
            notify()
        }
        addSource(source2) {
            lastFromSource2 = it
            notify()
        }
        addSource(source3) {
            lastFromSource3 = it
            notify()
        }
    }
}

/**
 * Similar to [combineLatest] but allows to modify the resulting emission by providing own combinator
 */
fun <S, T, R> combineLatest(source1: LiveData<S>, source2: LiveData<T>, combinator: (S, T) -> R): LiveData<R> {
    return map(combineLatest(source1, source2)) {
        val (first, second) = it
        combinator(first, second)
    }
}

/**
 * Similar to [combineLatest] but allows to modify the resulting emission by providing own combinator
 */
fun <S, T, U, R> combineLatest(
    source1: LiveData<S>,
    source2: LiveData<T>,
    source3: LiveData<U>,
    combinator: (S, T, U) -> R
): LiveData<R> {
    return map(combineLatest(source1, source2, source3)) {
        val (first, second, third) = it
        combinator(first, second, third)
    }
}