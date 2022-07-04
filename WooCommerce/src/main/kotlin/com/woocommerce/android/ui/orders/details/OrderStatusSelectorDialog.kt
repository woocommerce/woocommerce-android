package com.woocommerce.android.ui.orders.details

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel.*

/**
 * Dialog displays a list of order statuses and allows for selecting a single order status for
 * manually changing order statuses.
 *
 */
class OrderStatusSelectorDialog : DialogFragment() {
    companion object {
        const val KEY_ORDER_STATUS_RESULT = "key_order_status_result"
        private const val REFUNDED_ID: String = "refunded"
    }

    private val navArgs: OrderStatusSelectorDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var selectedOrderStatus = navArgs.currentStatus
        val orderStatusList: Array<Order.OrderStatus> =
            // remove the Refunded status from the dialog to avoid reporting problems (unless it's already Refunded)
            navArgs.orderStatusList
                .filter { selectedOrderStatus == REFUNDED_ID || it.statusKey != REFUNDED_ID }
                .toTypedArray()

        fun getCurrentOrderStatusIndex() =
            orderStatusList.indexOfFirst { it.statusKey == selectedOrderStatus }

        val selectedIndex = getCurrentOrderStatusIndex()

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(resources.getString(R.string.orderstatus_select_status))
            .setCancelable(true)
            .setSingleChoiceItems(orderStatusList.map { it.label }.toTypedArray(), selectedIndex) { _, which ->
                selectedOrderStatus = orderStatusList[which].statusKey
                AnalyticsTracker.track(
                    AnalyticsEvent.FILTER_ORDERS_BY_STATUS_DIALOG_OPTION_SELECTED,
                    mapOf("status" to selectedOrderStatus)
                )
            }
            .setPositiveButton(R.string.apply) { _, _ ->
                val newSelectedIndex = getCurrentOrderStatusIndex()
                if (newSelectedIndex != selectedIndex) {
                    AnalyticsTracker.track(
                        AnalyticsEvent.SET_ORDER_STATUS_DIALOG_APPLY_BUTTON_TAPPED,
                        mapOf("status" to selectedOrderStatus)
                    )
                    navigateBackWithResult(
                        key = KEY_ORDER_STATUS_RESULT,
                        result = OrderStatusUpdateSource.Dialog(
                            oldStatus = navArgs.currentStatus,
                            newStatus = selectedOrderStatus
                        )
                    )
                }
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
