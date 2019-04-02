package com.marosseleng.distancemeasurements

import android.content.pm.PackageManager

/**
 * @author Maroš Šeleng
 */
fun PackageManager.hasBluetoothLeFeature(): Boolean = hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

sealed class ScanningResult<out T> {
    object Idle : ScanningResult<Nothing>()
    object Loading : ScanningResult<Nothing>()
    class Success<T> : ScanningResult<T>()
    class Failure(val cause: Exception) : ScanningResult<Nothing>()
}
