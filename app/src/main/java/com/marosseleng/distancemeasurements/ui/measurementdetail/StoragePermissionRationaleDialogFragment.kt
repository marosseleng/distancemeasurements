package com.marosseleng.distancemeasurements.ui.measurementdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marosseleng.distancemeasurements.R
import kotlinx.android.synthetic.main.dialog_location_permission_rationale.*
import kotlinx.android.synthetic.main.dialog_storage_permission_rationale.*

//import kotlinx.android.synthetic.main.dialog_storage_permission_rationale.*
//import kotlinx.android.synthetic.main.*

/**
 * @author Maroš Šeleng
 */
class StoragePermissionRationaleDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_storage_permission_rationale, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        allowStorage.setOnClickListener {
            dismiss()
            val target = targetFragment ?: return@setOnClickListener
            if (target is PositiveButtonClickedListener) {
                target.onPositiveButtonClicked(targetRequestCode)
            }
        }

        cancelStorage.setOnClickListener {
            dismiss()
            val target = targetFragment ?: return@setOnClickListener
            if (target is NegativeButtonClickedListener) {
                target.onNegativeButtonClicked(targetRequestCode)
            }
        }
    }
}

class LocationPermissionRationaleDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_location_permission_rationale, container, false)
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

interface PositiveButtonClickedListener {
    fun onPositiveButtonClicked(requestCode: Int)
}

interface NegativeButtonClickedListener {
    fun onNegativeButtonClicked(requestCode: Int)
}

interface BottomSheetDialogListener : PositiveButtonClickedListener, NegativeButtonClickedListener