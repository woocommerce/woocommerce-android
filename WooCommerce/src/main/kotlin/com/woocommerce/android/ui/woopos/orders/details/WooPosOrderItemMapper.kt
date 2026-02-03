package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WooPosOrderItemMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val formatPrice: WooPosFormatPrice,
    private val orderStatusMapper: WooPosOrderStatusMapper,
) {
    fun mapOrderItem(order: Order, selectedId: Long?): WooPosOrdersState.OrderItemViewState {
        val status = orderStatusMapper.mapOrderStatus(order.status)

        return WooPosOrdersState.OrderItemViewState(
            id = order.id,
            title = "#${order.number}",
            date = order.dateCreated.formatToMMMddYYYYAtHHmm(
                atWord = resourceProvider.getString(R.string.date_time_connector)
            ),
            total = formatPrice(order.total, order.currency),
            customerEmail = order.customer?.email ?: order.billingAddress.email,
            isSelected = order.id == selectedId,
            status = status,
            statusSlug = order.status.toString(),
            createdAtMillis = order.dateCreated.time
        )
    }
}
