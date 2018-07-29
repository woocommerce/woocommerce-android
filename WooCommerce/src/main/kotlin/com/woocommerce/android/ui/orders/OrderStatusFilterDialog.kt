package com.woocommerce.android.ui.orders

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.woocommerce.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus

/**
 * Dialog displays a list of order statuses and allows for selecting a single order status to filter by.
 *
 * The list of order statuses is pulled from the [OrderStatus] enum with the top "All" option manually added.
 *
 * This fragment should be instantiated using the [OrderStatusFilterDialog.newInstance] method. Calling classes
 * can obtain the results of selection through the [OrderListFilterListener].
 */
class OrderStatusFilterDialog : DialogFragment() {
    companion object {
        const val TAG: String = "OrderStatusFilterDialog"

        fun newInstance(
            currentFilter: OrderStatus?,
            listener: OrderListFilterListener
        ): OrderStatusFilterDialog {
            val fragment = OrderStatusFilterDialog()
            fragment.listener = listener
            fragment.selectedFilter = currentFilter
            return fragment
        }
    }

    interface OrderListFilterListener {
        fun filterSelected(orderStatus: OrderStatus?)
    }

    val filterOptions: Array<String> by lazy {
        arrayOf(resources.getString(R.string.all)).plus(OrderStatus.values().map { it.label }.toTypedArray())
    }

    var listener: OrderListFilterListener? = null
    var selectedFilter: OrderStatus? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedIndex = selectedFilter?.ordinal?.inc() ?: 0

        return AlertDialog.Builder(context)
                .setTitle(resources.getString(R.string.orderlist_filter_by))
                .setCancelable(true)
                .setSingleChoiceItems(filterOptions, selectedIndex) { _, which ->
                    val selectedLabel = filterOptions[which]
                    selectedFilter = OrderStatus.fromLabel(selectedLabel)
                }
                .setPositiveButton(R.string.orderlist_filter_apply) { dialog, _ ->
                    listener?.filterSelected(selectedFilter)
                    dialog.cancel()
                }.create()
    }
}
