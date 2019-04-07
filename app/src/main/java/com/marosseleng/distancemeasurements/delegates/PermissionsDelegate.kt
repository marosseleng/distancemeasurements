package com.marosseleng.distancemeasurements.delegates

import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat
import com.marosseleng.distancemeasurements.application
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * @author Maroš Šeleng
 */
class PermissionsDelegate(private vararg val permissions: String) : ReadOnlyProperty<Any, Boolean> {
    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return permissions.all { ContextCompat.checkSelfPermission(application, it) == PERMISSION_GRANTED }
    }
}