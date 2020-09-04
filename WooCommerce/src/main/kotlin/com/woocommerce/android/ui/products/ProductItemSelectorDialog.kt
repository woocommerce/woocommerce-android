package com.woocommerce.android.ui.products

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductBackOrders
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductTaxStatus

/**
 * Dialog displays a list of product items such as
 * [CoreProductBackOrders], [CoreProductStockStatus], [CoreProductTaxStatus] and
 * allows for selecting a single product item
 *
 * This fragment should be instantiated using the [ProductItemSelectorDialog.newInstance] method.
 * Calling classes can obtain the results of selection through the [onActivityResult]
 * via [ProductItemSelectorDialog.getTargetFragment].
 *
 * The [resultCode] passed to this fragment is used to classify the product item i.e.
 * [CoreProductBackOrders], [CoreProductStockStatus] or [CoreProductTaxStatus]
 */
class ProductItemSelectorDialog : DialogFragment() {
    companion object {
        const val TAG: String = "ProductItemSelectorDialog"

        fun newInstance(
            listener: Fragment,
            requestCode: Int,
            dialogTitle: String,
            listItemMap: Map<String, String>,
            selectedListItem: String?
        ): ProductItemSelectorDialog {
            val fragment = ProductItemSelectorDialog()
            fragment.setTargetFragment(listener, requestCode)
            fragment.retainInstance = true
            fragment.resultCode = requestCode
            fragment.dialogTitle = dialogTitle
            fragment.listItemMap = listItemMap
            fragment.selectedListItem = selectedListItem
            return fragment
        }
    }

    interface ProductItemSelectorDialogListener {
        fun onProductItemSelected(resultCode: Int, selectedItem: String?)
    }

    private var resultCode: Int = -1
    private var selectedListItem: String? = null

    private var dialogTitle: String? = null
    private var listItemMap: Map<String, String>? = null

    private var listener: ProductItemSelectorDialogListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = targetFragment as ProductItemSelectorDialogListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedIndex = getCurrentProductItemListIndex()

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(dialogTitle)
                .setSingleChoiceItems(listItemMap?.values?.toTypedArray(), selectedIndex) { dialog, which ->
                    listener?.onProductItemSelected(resultCode, listItemMap?.keys?.toTypedArray()?.get(which))
                    dialog.dismiss()
                }
        return builder.create()
    }

    private fun getCurrentProductItemListIndex(): Int {
        return listItemMap?.values?.indexOfFirst { it == selectedListItem } ?: 0
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
