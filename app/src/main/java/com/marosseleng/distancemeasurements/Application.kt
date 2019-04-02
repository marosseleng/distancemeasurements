package com.marosseleng.distancemeasurements

import androidx.room.Room
import com.marosseleng.distancemeasurements.data.AppDatabase
import com.marosseleng.distancemeasurements.data.MeasuredValue
import com.marosseleng.distancemeasurements.data.Measurement
import com.marosseleng.distancemeasurements.data.MeasurementType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * @author Maroš Šeleng
 */
class Application : android.app.Application() {

    companion object {
        lateinit var instance: Application
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "database.db")
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}