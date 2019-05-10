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

package com.marosseleng.distancemeasurements.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.marosseleng.distancemeasurements.R
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

/**
 * @author Maroš Šeleng
 */
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onSupportNavigateUp() =
        findNavController(R.id.navHostFragment).navigateUp(appBarConfiguration)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost = findNavController(R.id.navHostFragment)
        bottomNavigation.setupWithNavController(navHost)

        navHost.addOnDestinationChangedListener { controller, destination, arguments ->
            Timber.d("Destination changed: %s, %s, %s", controller, destination, arguments)
        }

        // TODO add toolbar
//        setupActionBarWithNavController(navHost, AppBarConfiguration(navHost.graph))
        appBarConfiguration =
            AppBarConfiguration(setOf(R.id.measurements, R.id.newMeasurement, R.id.positioning, R.id.about))
        setupActionBarWithNavController(navHost, appBarConfiguration)
    }

    fun getBottomNavigation(): View = bottomNavigation
}
