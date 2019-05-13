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

import android.net.wifi.WifiManager
import android.net.wifi.rtt.WifiRttManager
import androidx.core.content.getSystemService

val application = Application.instance
val database = application.database
val dao = database.dao()
val wifiManager by lazy { application.getSystemService<WifiManager>() }
val wifiRttManager by lazy { application.getSystemService<WifiRttManager>() }