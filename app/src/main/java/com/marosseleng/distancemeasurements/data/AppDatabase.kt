package com.marosseleng.distancemeasurements.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.marosseleng.distancemeasurements.data.Converters
import com.marosseleng.distancemeasurements.data.Dao
import com.marosseleng.distancemeasurements.data.MeasuredValue
import com.marosseleng.distancemeasurements.data.Measurement

/**
 * @author Maroš Šeleng
 */
@Database(entities = [Measurement::class, MeasuredValue::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): Dao
}
