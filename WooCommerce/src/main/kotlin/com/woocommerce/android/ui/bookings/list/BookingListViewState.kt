package com.woocommerce.android.ui.bookings.list

import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.compose.AttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingPaymentStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import java.text.SimpleDateFormat
import java.util.Date

data class BookingListViewState(
    val bookings: List<BookingListItem>,
    val loadingState: LoadingState,
    val onRefresh: () -> Unit,
    val onLoadMore: () -> Unit
) {
    enum class LoadingState {
        Idle, Loading, Refreshing, Appending
    }
}

data class BookingListItem(
    val id: Long,
    val summary: BookingSummaryModel
)

fun Booking.toUiModel(): BookingListItem {
    val dateFormatter = SimpleDateFormat.getDateTimeInstance(
        SimpleDateFormat.MEDIUM,
        SimpleDateFormat.SHORT
    )

    return BookingListItem(
        id = id.value,
        summary = BookingSummaryModel(
            date = dateFormatter.format(Date(start * 1000)),
            name = "Women’s Haircut",
            customerName = "Margarita Nikolaevna",
            attendanceStatus = AttendanceStatus.BOOKED,
            paymentStatus = BookingPaymentStatus.valueOf(status.uppercase())
        )
    )
}
