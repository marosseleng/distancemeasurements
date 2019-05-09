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

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

/**
 * @author Maroš Šeleng
 */
fun PackageManager.hasBluetoothLeFeature(): Boolean = hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

fun PackageManager.hasWifiRttFeature(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        && hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)

fun File.toFileUri(): Uri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
    toUri()
} else {
    FileProvider.getUriForFile(application, application.packageName + ".fileprovider", this)
}

/*
long map(long x, long in_min, long in_max, long out_min, long out_max)
{
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}
in: 1-3
out: 1-20
x<==1, 2, 3
y<==0, 7
(1) * 19 / 2 + 1



1       10        20
X        X         X
OOOOOOOOOOOOOOOOOOOO
 */