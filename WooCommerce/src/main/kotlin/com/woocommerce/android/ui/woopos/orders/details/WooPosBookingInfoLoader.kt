package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingTimeRangeFormatter
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.BookingInfo
import com.woocommerce.android.util.DateFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WooPosBookingInfoLoader @Inject constructor(
    private val bookingsRepository: BookingsRepository,
    private val resourceProvider: ResourceProvider,
    private val dateFormatter: DateFormatter,
    private val timeRangeFormatter: WooPosBookingTimeRangeFormatter,
) {
    suspend fun resolveBookingInfo(bookingId: Long): BookingInfo {
        val booking = bookingsRepository.getBooking(bookingId)
        return if (booking != null) {
            BookingInfo.Loaded(formatBookingInfo(bookingId, booking))
        } else {
            BookingInfo.Loading(bookingId)
        }
    }

    suspend fun fetchBookingInfo(bookingId: Long): BookingInfo {
        return bookingsRepository.fetchBooking(bookingId).fold(
            onSuccess = { booking ->
                BookingInfo.Loaded(formatBookingInfo(bookingId, booking))
            },
            onFailure = {
                BookingInfo.Error(
                    resourceProvider.getString(R.string.woopos_orders_details_booking_info_error)
                )
            }
        )
    }

    private fun formatBookingInfo(bookingId: Long, booking: Booking): String {
        val date = dateFormatter.formatDate(booking.start)
        val timeRange = timeRangeFormatter.format(booking.start, booking.end)
        return resourceProvider.getString(
            R.string.woopos_orders_details_booking_info,
            bookingId,
            date,
            timeRange
        )
    }
}
