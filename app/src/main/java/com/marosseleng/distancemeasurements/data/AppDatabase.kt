package com.marosseleng.distancemeasurements.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * @author Maroš Šeleng
 */
@Database(entities = [Measurement::class, MeasuredValue::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): Dao
}
