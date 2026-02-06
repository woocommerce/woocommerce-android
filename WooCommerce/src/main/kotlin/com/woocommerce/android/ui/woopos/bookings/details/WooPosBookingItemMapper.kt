package com.woocommerce.android.ui.woopos.bookings.details

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsState
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WooPosBookingItemMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val formatPrice: WooPosFormatPrice,
    private val bookingStatusMapper: WooPosBookingStatusMapper,
) {
    fun mapBookingItem(order: Order, selectedId: Long?): WooPosBookingsState.BookingItemViewState {
        val status = bookingStatusMapper.mapBookingStatus(order.status)

        return WooPosBookingsState.BookingItemViewState(
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
