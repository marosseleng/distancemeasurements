package com.marosseleng.distancemeasurements

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marosseleng.distancemeasurements.data.Dao
import com.marosseleng.distancemeasurements.data.AppDatabase
import com.marosseleng.distancemeasurements.data.Measurement
import com.marosseleng.distancemeasurements.data.MeasurementType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * @author Maroš Šeleng
 */
@RunWith(AndroidJUnit4::class)
class SimpleEntityReadWriteTest {
    private lateinit var dao: Dao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).build()
        dao = db.dao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

//    @Test
//    @Throws(Exception::class)
//    fun writeUserAndReadInList() {
//        val measurement = Measurement(deviceName = "example", measurementType = MeasurementType.RSSI)
//        runBlocking(Dispatchers.Main) {
//            val newId = dao.insertMeasurement(measurement)
//            assertThat(newId, not(equalTo(0L)))
//        }
//    }
}