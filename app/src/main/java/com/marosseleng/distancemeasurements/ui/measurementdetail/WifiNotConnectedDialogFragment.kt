package com.marosseleng.distancemeasurements.ui.measurementdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marosseleng.distancemeasurements.R
import kotlinx.android.synthetic.main.dialog_wifi_not_connected.*

/**
 * @author Maroš Šeleng
 */
class WifiNotConnectedDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_wifi_not_connected, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        positiveButton.setOnClickListener {
            dismiss()
            val target = targetFragment ?: return@setOnClickListener
            if (target is PositiveButtonClickedListener) {
                target.onPositiveButtonClicked(targetRequestCode)
            }
        }

        negativeButton.setOnClickListener {
            dismiss()
            val target = targetFragment ?: return@setOnClickListener
            if (target is NegativeButtonClickedListener) {
                target.onNegativeButtonClicked(targetRequestCode)
            }
        }
    }
}