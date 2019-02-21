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
 * Dialog displays a list of order statuses and allows for selecting a single order status to filter by.
 *
 * This fragment should be instantiated using the [OrderStatusSelectorDialog.newInstance] method. Calling classes
 * can obtain the results of selection through the [OrderListFilterListener].
 */
class OrderStatusSelectorDialog : DialogFragment() {
    companion object {
        const val TAG: String = "OrderStatusSelectorDialog"

        private const val ALL_FILTER_ID: String = "all"

        fun newInstance(
            orderStatusOptions: Map<String, WCOrderStatusModel>,
            currentFilter: String?,
            title: String? = null,
            listener: OrderListFilterListener
        ): OrderStatusSelectorDialog {
            val fragment = OrderStatusSelectorDialog()
            fragment.orderStatusOptions = orderStatusOptions
            fragment.listener = listener
            fragment.dialogTitle = title
            fragment.selectedFilter = currentFilter ?: ALL_FILTER_ID
            return fragment
        }
    }

    interface OrderListFilterListener {
        fun onFilterSelected(orderStatus: String?)
    }

    private val filterMap: Map<String, String> by lazy {
        mapOf(ALL_FILTER_ID to getString(R.string.all)).plus(
                orderStatusOptions.values.associate { it.statusKey to it.label }
        )
    }

    var listener: OrderListFilterListener? = null
    var dialogTitle: String? = null
    var selectedFilter: String? = null
    lateinit var orderStatusOptions: Map<String, WCOrderStatusModel>

    private fun getCurrentOrderStatusIndex(): Int {
        return filterMap.keys.indexOfFirst { it == selectedFilter }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedIndex = getCurrentOrderStatusIndex()

        val title = dialogTitle ?: resources.getString(R.string.orderlist_filter_by)
        return AlertDialog.Builder(context)
                .setTitle(title)
                .setCancelable(true)
                .setSingleChoiceItems(filterMap.values.toTypedArray(), selectedIndex) { _, which ->
                    selectedFilter = filterMap.keys.toTypedArray()[which]

                    AnalyticsTracker.track(Stat.FILTER_ORDERS_BY_STATUS_DIALOG_OPTION_SELECTED,
                            mapOf("status" to selectedFilter))
                }
                .setPositiveButton(R.string.apply) { dialog, _ ->
                    AnalyticsTracker.track(Stat.FILTER_ORDERS_BY_STATUS_DIALOG_APPLY_FILTER_BUTTON_TAPPED)

                    val newSelectedIndex = getCurrentOrderStatusIndex()
                    if (newSelectedIndex != selectedIndex) {
                        // If 'All' is selected filter, pass null to signal a filterless refresh
                        listener?.onFilterSelected(selectedFilter.takeUnless { it == ALL_FILTER_ID })
                    }
                    dialog.dismiss()
                }.create()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
