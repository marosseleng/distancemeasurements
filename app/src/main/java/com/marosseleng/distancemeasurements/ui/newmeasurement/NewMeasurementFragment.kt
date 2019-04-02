package com.marosseleng.distancemeasurements.ui.newmeasurement

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.marosseleng.distancemeasurements.BuildConfig
import com.marosseleng.distancemeasurements.R
import com.marosseleng.distancemeasurements.hasBluetoothLeFeature
import com.marosseleng.distancemeasurements.ui.numbers.BEACON_LIST_ENABLE_BLUETOOTH
import com.marosseleng.distancemeasurements.ui.numbers.BEACON_LIST_LOCATION_PERMISSION
import kotlinx.android.synthetic.main.fragment_new_measurement.*

/**
 * @author Maroš Šeleng
 */
class NewMeasurementFragment : Fragment() {

    private val validRadioButtonIds = setOf(R.id.newBluetooth, R.id.newRss, R.id.newRtt)

    private val mBluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_measurement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectMeasurementType.isEnabled = radios.checkedRadioButtonId in validRadioButtonIds

        radios.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId in validRadioButtonIds) {
                selectMeasurementType.isEnabled = true
            } else {
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException("Unknown radio button id checked: $checkedId")
                }
            }
        }

        selectMeasurementType.setOnClickListener {
            handleButtonClick(radios.checkedRadioButtonId)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        newBluetooth.isVisible = activity?.packageManager?.hasBluetoothLeFeature() == true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BEACON_LIST_LOCATION_PERMISSION -> {
                if (permissions.isEmpty() || grantResults.isEmpty()) {
                    return
                }
                val result = grantResults[0]
                val shouldRequestRationale =
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (result == PackageManager.PERMISSION_GRANTED) {
                    findNavController().navigate(R.id.newBluetooth)
                } else if (!shouldRequestRationale) {
                    // user checked "Don't ask me again"
                    TODO()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun handleButtonClick(checkedItemId: Int) {
        val activity = activity ?: return
        when (checkedItemId) {
            R.id.newBluetooth -> {
                if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                    val requestCode = BEACON_LIST_LOCATION_PERMISSION
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), requestCode)
                } else if (mBluetoothAdapter?.isEnabled == false) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, BEACON_LIST_ENABLE_BLUETOOTH)
                } else {
                    findNavController().navigate(NewMeasurementFragmentDirections.actionNewMeasurementToNewBluetooth())
                }
            }
            R.id.newRss -> {
                // TODO permissions and wifi on/off check!!
                findNavController().navigate(NewMeasurementFragmentDirections.actionNewMeasurementToNewRss())
            }
            R.id.newRtt -> {

            }
            else -> {
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException("Unknown checkedItemId: $checkedItemId")
                }
            }
        }
    }
}