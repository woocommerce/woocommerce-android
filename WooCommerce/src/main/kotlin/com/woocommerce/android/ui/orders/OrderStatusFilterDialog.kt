package com.woocommerce.android.ui.orders

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.ui.orders.OrderStatusFilterDialog.OrderListFilterListener
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

/**
 * Dialog displays a list of order statuses and allows for selecting a single order status to filter by.
 *
 * The list of order statuses is pulled from the [CoreOrderStatus] enum with the top "All" option manually added.
 *
 * This fragment should be instantiated using the [OrderStatusFilterDialog.newInstance] method. Calling classes
 * can obtain the results of selection through the [OrderListFilterListener].
 */
class OrderStatusFilterDialog : DialogFragment() {
    companion object {
        const val TAG: String = "OrderStatusFilterDialog"

        fun newInstance(
            currentFilter: CoreOrderStatus?,
            listener: OrderListFilterListener
        ): OrderStatusFilterDialog {
            val fragment = OrderStatusFilterDialog()
            fragment.listener = listener
            fragment.selectedFilter = currentFilter
            return fragment
        }
    }

    interface OrderListFilterListener {
        fun onFilterSelected(orderStatus: String?)
    }

    val filterOptions: Array<String> by lazy {
        arrayOf(resources.getString(R.string.all)).plus(CoreOrderStatus.values().map { it.label }.toTypedArray())
    }

    var listener: OrderListFilterListener? = null
    var selectedFilter: CoreOrderStatus? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedIndex = selectedFilter?.ordinal?.inc() ?: 0

        return AlertDialog.Builder(context)
                .setTitle(resources.getString(R.string.orderlist_filter_by))
                .setCancelable(true)
                .setSingleChoiceItems(filterOptions, selectedIndex) { dialog, which ->
                    val selectedLabel = filterOptions[which]

                    AnalyticsTracker.track(Stat.FILTER_ORDERS_BY_STATUS_DIALOG_OPTION_SELECTED,
                            mapOf("status" to selectedLabel))

                    selectedFilter = CoreOrderStatus.fromLabel(selectedLabel)
                    val newSelectedIndex = selectedFilter?.ordinal?.inc() ?: 0

                    if (newSelectedIndex != selectedIndex) {
                        listener?.onFilterSelected(selectedFilter?.value)
                    }

                    dialog.cancel()
                }
                .create()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
