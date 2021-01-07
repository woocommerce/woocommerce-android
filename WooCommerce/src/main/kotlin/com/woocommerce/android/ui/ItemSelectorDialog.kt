package com.woocommerce.android.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateBackWithResult

class ItemSelectorDialog : DialogFragment() {
    companion object {
        const val TAG: String = "ItemSelectorDialog"
    }

    private val args: ItemSelectorDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedIndex = getCurrentItemIndex()

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(args.title)
            .setSingleChoiceItems(args.keys, selectedIndex) { dialog, which ->
                navigateBackWithResult(args.requestKey, args.values[which])
                dialog.dismiss()
            }
        return builder.create()
    }

    private fun getCurrentItemIndex(): Int {
        return args.values.indexOfFirst { it == args.selectedItem }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
