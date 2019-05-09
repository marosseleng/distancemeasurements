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

package com.marosseleng.distancemeasurements.data

import androidx.room.TypeConverter

/**
 * @author Maroš Šeleng
 */
class Converters {
    @TypeConverter
    fun fromMeasurementType(type: MeasurementType): Int = type.ordinal

    @TypeConverter
    fun toMeasurementType(dbValue: Int) = MeasurementType.values().first { it.ordinal == dbValue }

    @TypeConverter
    fun fromRealDistances(distances: RealDistances): String = distances.parsedValues

    @TypeConverter
    fun toRealDistances(dbValue: String) = RealDistances().apply { parsedValues = dbValue }
}