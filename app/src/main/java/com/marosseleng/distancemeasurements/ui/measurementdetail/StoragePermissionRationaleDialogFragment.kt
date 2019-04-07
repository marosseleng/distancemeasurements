package com.marosseleng.distancemeasurements.ui.measurementdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.marosseleng.distancemeasurements.R
import kotlinx.android.synthetic.main.dialog_storage_permission_rationale.*

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
            (parentFragment as? MeasurementDetailFragment)?.requestStoragePermission()
            dismiss()
        }

        cancelStorage.setOnClickListener {
            dismiss()
        }
    }
}