package com.woocommerce.android.ui.orders

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.WCOrderStatusModel

/**
 * Dialog displays a list of order statuses and allows for selecting a single order status for filtering or
 * manually changing order statuses.
 *
 * This fragment should be instantiated using the [OrderStatusSelectorDialog.newInstance] method. Calling classes
 * can obtain the results of selection through the [OrderStatusDialogListener].
 */
class OrderStatusSelectorDialog : DialogFragment() {
    companion object {
        const val TAG: String = "OrderStatusSelectorDialog"

        private const val ALL_FILTER_ID: String = "all"

        fun newInstance(
            orderStatusOptions: Map<String, WCOrderStatusModel>,
            currentStatus: String?,
            isFilter: Boolean,
            listener: OrderStatusDialogListener
        ): OrderStatusSelectorDialog {
            val fragment = OrderStatusSelectorDialog()
            fragment.orderStatusOptions = orderStatusOptions
            fragment.isFilter = isFilter
            fragment.listener = listener
            fragment.selectedOrderStatus = currentStatus ?: ALL_FILTER_ID
            return fragment
        }
    }

    interface OrderStatusDialogListener {
        fun onOrderStatusSelected(orderStatus: String?)
    }

    private val orderStatusMap: Map<String, String> by lazy {
        if (isFilter) {
            mapOf(ALL_FILTER_ID to getString(R.string.all)).plus(
                    orderStatusOptions.values.associate { it.statusKey to it.label }
            )
        } else {
            orderStatusOptions.values.associate { it.statusKey to it.label }
        }
    }

    var listener: OrderStatusDialogListener? = null
    var selectedOrderStatus: String? = null
    var isFilter: Boolean = false
    lateinit var orderStatusOptions: Map<String, WCOrderStatusModel>

    private fun getCurrentOrderStatusIndex(): Int {
        return orderStatusMap.keys.indexOfFirst { it == selectedOrderStatus }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedIndex = getCurrentOrderStatusIndex()

        val title = if (isFilter) {
            resources.getString(R.string.orderlist_filter_by)
        } else {
            resources.getString(R.string.orderstatus_select_status)
        }
        return AlertDialog.Builder(context)
                .setTitle(title)
                .setCancelable(true)
                .setSingleChoiceItems(orderStatusMap.values.toTypedArray(), selectedIndex) { _, which ->
                    selectedOrderStatus = orderStatusMap.keys.toTypedArray()[which]

                    AnalyticsTracker.track(Stat.FILTER_ORDERS_BY_STATUS_DIALOG_OPTION_SELECTED,
                            mapOf("status" to selectedOrderStatus))
                }
                .setPositiveButton(R.string.apply) { dialog, _ ->
                    val newSelectedIndex = getCurrentOrderStatusIndex()

                    // If in filter mode and 'All' is selected, pass null to signal a filterless refresh
                    val selectedItem = selectedOrderStatus.takeUnless { it == ALL_FILTER_ID }

                    AnalyticsTracker.track(Stat.FILTER_ORDERS_BY_STATUS_DIALOG_APPLY_FILTER_BUTTON_TAPPED)

                    if (newSelectedIndex != selectedIndex) {
                        listener?.onOrderStatusSelected(selectedItem)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
