package com.marosseleng.distancemeasurements.data

import android.bluetooth.le.ScanResult
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

/**
 * @author Maroš Šeleng
 */
@Entity(tableName = "measurements")
@Parcelize
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "device_name")
    val deviceName: String,

    @ColumnInfo(name = "device_address")
    val deviceAddress: String,

    @ColumnInfo(name = "measurement_type", index = true)
    val measurementType: MeasurementType,

    @ColumnInfo(name = "note")
    val note: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

@Entity(
    tableName = "measured_values",
    foreignKeys = [ForeignKey(entity = Measurement::class, parentColumns = ["id"], childColumns = ["measurement_id"])]
)
@Parcelize
data class MeasuredValue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "measurement_id", index = true)
    val measurementId: Long,

    @ColumnInfo(name = "measured_value")
    val measuredValue: Float,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    // TODO consider changing measuredValue's type to Int
    class Factory(val measuredValue: Float, private val timestamp: Long) {
        operator fun invoke(): (Long) -> MeasuredValue = { measurementId ->
            MeasuredValue(
                measurementId = measurementId,
                measuredValue = measuredValue,
                timestamp = timestamp
            )
        }
    }
}

fun ScanResult.toMeasuredValueFactory(): MeasuredValue.Factory {
    return MeasuredValue.Factory(
        measuredValue = rssi.toFloat(),
        timestamp = System.currentTimeMillis()
    )
}

enum class MeasurementType {
    BLE,
    RSSI,
    RTT
}

data class MeasurementAnchorDevice(val name: String, val address: String)