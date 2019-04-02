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
}