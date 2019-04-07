package com.marosseleng.distancemeasurements.data

import android.bluetooth.le.ScanResult
import android.os.Parcel
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

    @ColumnInfo(name = "frequency")
    val frequency: Int = 0,

    @ColumnInfo(name = "measurement_type", index = true)
    val measurementType: MeasurementType,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "real_distances")
    val realDistances: RealDistances = RealDistances(),

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
    val measuredValue: Int,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    // TODO consider changing measuredValue's type to Int
    class Factory(val measuredValue: Int, private val timestamp: Long) {
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
        measuredValue = rssi,
        timestamp = System.currentTimeMillis()
    )
}

enum class MeasurementType {
    BLE,
    RSSI,
    RTT
}

data class MeasurementAnchorDevice(val name: String, val address: String, val deviceFrequency: Int)

// TODO unit tests!
class RealDistances() : Parcelable {

    /**
     * Comma-separated integer values in centimeters
     */
    var parsedValues: String = ""

    constructor(parcel: Parcel) : this() {
        parsedValues = parcel.readString() ?: ""
    }

    fun asList(): List<Int> = parsedValues.split(",").mapNotNull { it.toIntOrNull() }

    fun parseUserInput(input: String?) {
        if (input == null) {
            return
        }
        parsedValues = input.replace(Regex("[^0-9,]"), "")
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(parsedValues)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RealDistances> {
        override fun createFromParcel(parcel: Parcel): RealDistances {
            return RealDistances(parcel)
        }

        override fun newArray(size: Int): Array<RealDistances?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return "RealDistances[$parsedValues]"
    }
}