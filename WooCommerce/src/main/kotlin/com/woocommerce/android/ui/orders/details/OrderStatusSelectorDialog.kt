package com.woocommerce.android.ui.orders.details

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.navigateBackWithResult

/**
 * Dialog displays a list of order statuses and allows for selecting a single order status for
 * manually changing order statuses.
 *
 */
class OrderStatusSelectorDialog : DialogFragment() {
    companion object {
        const val KEY_ORDER_STATUS_RESULT = "key_order_status_result"
    }

    private var selectedOrderStatus: String? = null
    private val navArgs: OrderStatusSelectorDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        selectedOrderStatus = navArgs.currentStatus
        val selectedIndex = getCurrentOrderStatusIndex()

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(resources.getString(R.string.orderstatus_select_status))
            .setCancelable(true)
            .setSingleChoiceItems(navArgs.orderStatusList, selectedIndex) { _, which ->
                selectedOrderStatus = navArgs.orderStatusList[which]
                AnalyticsTracker.track(
                    Stat.FILTER_ORDERS_BY_STATUS_DIALOG_OPTION_SELECTED,
                    mapOf("status" to selectedOrderStatus)
                )
            }
            .setPositiveButton(R.string.apply) { _, _ ->
                val newSelectedIndex = getCurrentOrderStatusIndex()
                if (newSelectedIndex != selectedIndex) {
                    AnalyticsTracker.track(
                        Stat.SET_ORDER_STATUS_DIALOG_APPLY_BUTTON_TAPPED,
                        mapOf("status" to selectedOrderStatus)
                    )
                    navigateBackWithResult(KEY_ORDER_STATUS_RESULT, selectedOrderStatus)
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

    private fun getCurrentOrderStatusIndex() =
        navArgs.orderStatusList.indexOfFirst { it == selectedOrderStatus }
}
