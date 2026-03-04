package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.orders.OrderStatusColorKey
import com.woocommerce.android.ui.woopos.orders.PosOrderStatus
import com.woocommerce.android.viewmodel.ResourceProvider
import java.util.Locale
import javax.inject.Inject

class WooPosOrderStatusMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val locale: Locale,
) {
    fun mapOrderStatus(status: Order.Status): PosOrderStatus {
        val statusText = localizedLabel(status)
        return PosOrderStatus(
            text = statusText,
            colorKey = OrderStatusColorKey.fromStatus(status)
        )
    }

    private fun localizedLabel(status: Order.Status): String {
        return when (status) {
            Order.Status.Cancelled -> resourceProvider.getString(R.string.woopos_orders_status_cancelled)
            Order.Status.Completed -> resourceProvider.getString(R.string.woopos_orders_status_completed)
            is Order.Status.Custom ->
                status.value.replaceFirstChar { it.titlecase(locale) }.replace("-", " ")
            Order.Status.Failed -> resourceProvider.getString(R.string.woopos_orders_status_failed)
            Order.Status.OnHold -> resourceProvider.getString(R.string.woopos_orders_status_on_hold)
            Order.Status.Pending -> resourceProvider.getString(R.string.woopos_orders_status_pending)
            Order.Status.Processing -> resourceProvider.getString(R.string.woopos_orders_status_processing)
            Order.Status.Refunded -> resourceProvider.getString(R.string.woopos_orders_status_refunded)
        }
    }
}
