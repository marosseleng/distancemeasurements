package com.marosseleng.distancemeasurements.delegates

import androidx.fragment.app.Fragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * @author Maroš Šeleng
 */
class ShouldShowRationaleDelegate(private val permission: String) : ReadOnlyProperty<Fragment, Boolean> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): Boolean {
        return thisRef.shouldShowRequestPermissionRationale(permission)
    }
}