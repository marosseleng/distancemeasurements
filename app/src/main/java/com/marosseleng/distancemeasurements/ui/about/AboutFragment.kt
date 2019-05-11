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

package com.marosseleng.distancemeasurements.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.marosseleng.distancemeasurements.R

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)
        val list = view.findViewById<RecyclerView>(R.id.dependenciesList)
        list.adapter = DependenciesAdapter()
        return view
    }

    class DependenciesAdapter : RecyclerView.Adapter<DependenciesAdapter.ViewHolder>() {

        private val libraries = listOf(
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core",
            "org.jetbrains.kotlinx:kotlinx-coroutines-android",
            "com.google.android.material:material",
            "androidx.constraintlayout:constraintlayout",
            "androidx.core:core-ktx",
            "androidx.appcompat:appcompat",
            "androidx.lifecycle:lifecycle-viewmodel-ktx",
            "androidx.lifecycle:lifecycle-extensions",
            "androidx.lifecycle:lifecycle-compiler",
            "androidx.room:room-runtime",
            "androidx.room:room-compiler",
            "androidx.room:room-ktx",
            "androidx.navigation:navigation-fragment-ktx",
            "androidx.navigation:navigation-ui-ktx",
            "com.jakewharton.timber:timber",
            "com.github.PhilJay:MPAndroidChart",
            "com.lemmingapex.trilateration:trilateration:1.0.2",
            "androidx.room:room-testing",
            "junit:junit",
            "androidx.test.ext:junit",
            "androidx.test.espresso:espresso-core:3.1.1"
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            )
        }

        override fun getItemCount() = libraries.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val library = libraries[position]
            holder.text.text = library
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text: TextView = itemView as TextView
        }
    }
}