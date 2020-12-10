package com.woocommerce.android.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ItemSelectorDialogArgs

class ItemSelectorDialog : DialogFragment() {
    companion object {
        const val TAG: String = "ItemSelectorDialog"
    }

    private val args: ItemSelectorDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedIndex = getCurrentItemIndex()

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(args.title)
                .setSingleChoiceItems(args.values, selectedIndex) { dialog, which ->
                    val item = args.keys[which]
                    navigateBackWithResult(args.requestKey, item)
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
