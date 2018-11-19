package com.woocommerce.android.ui.orders

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
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
            fragment.selectedFilter = currentFilter?.value ?: "all"
            return fragment
        }
    }

    interface OrderListFilterListener {
        fun onFilterSelected(orderStatus: String?)
    }

    private val filterOptions: Array<String> by lazy {
        arrayOf(resources.getString(R.string.all)).plus(CoreOrderStatus.values().map { statusToLabelMap[it] }.toTypedArray())
    }

    private val stringIds: Array<String> by lazy {
        arrayOf("all").plus(CoreOrderStatus.values().map { it.value }.toTypedArray())
    }

    private val statusToLabelMap: Map<CoreOrderStatus, String> by lazy {
        mapOf(
                CoreOrderStatus.PENDING to getString(R.string.orderstatus_pending),
                CoreOrderStatus.PROCESSING to getString(R.string.orderstatus_processing),
                CoreOrderStatus.ON_HOLD to getString(R.string.orderstatus_hold),
                CoreOrderStatus.COMPLETED to getString(R.string.orderstatus_completed),
                CoreOrderStatus.CANCELLED to getString(R.string.orderstatus_cancelled),
                CoreOrderStatus.REFUNDED to getString(R.string.orderstatus_refunded),
                CoreOrderStatus.FAILED to getString(R.string.orderstatus_failed)
        )
    }

    var listener: OrderListFilterListener? = null
    var selectedFilter: String? = null

    private fun getOrderStatusAtIndex(index: Int) = stringIds[index]

    private fun getCurrentOrderStatusIndex(): Int {
        return stringIds.indexOfFirst { it == selectedFilter }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedIndex = getCurrentOrderStatusIndex()

        return AlertDialog.Builder(context)
                .setTitle(resources.getString(R.string.orderlist_filter_by))
                .setCancelable(true)
                .setSingleChoiceItems(filterOptions, selectedIndex) { _, which ->
                    val selectedLabel = filterOptions[which]

                    AnalyticsTracker.track(Stat.FILTER_ORDERS_BY_STATUS_DIALOG_OPTION_SELECTED,
                            mapOf("status" to selectedLabel))

                    selectedFilter = getOrderStatusAtIndex(which)
                }
                .setPositiveButton(R.string.orderlist_filter_apply) { dialog, _ ->
                    AnalyticsTracker.track(Stat.FILTER_ORDERS_BY_STATUS_DIALOG_APPLY_FILTER_BUTTON_TAPPED)

                    val newSelectedIndex = getCurrentOrderStatusIndex()
                    if (newSelectedIndex != selectedIndex) {
                        // If 'All' is selected filter, pass null to signal a filterless refresh
                        listener?.onFilterSelected(if (newSelectedIndex > 0) selectedFilter else null)
                    }
                    dialog.cancel()
                }.create()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
