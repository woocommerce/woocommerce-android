package com.woocommerce.android.ui.bookings.list

import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.compose.AttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingPaymentStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class BookingListViewState(
    val contentState: BookingListContentState,
    val tabState: BookingListTabState
)

data class BookingListContentState(
    val bookings: List<BookingListItem>,
    val loadingState: BookingListLoadingState,
    val onRefresh: () -> Unit,
    val onLoadMore: () -> Unit,
    val onBookingClick: (Long) -> Unit,
) {
    fun isNotEmpty() = bookings.isNotEmpty()
}

data class BookingListTabState(
    val selectedTab: BookingListTab,
    val onTabChanged: (BookingListTab) -> Unit
)

data class BookingListItem(
    val id: Long,
    val summary: BookingSummaryModel
)

enum class BookingListLoadingState {
    Idle, Loading, Refreshing, Appending
}

enum class BookingListTab {
    Today, Upcoming, All
}

fun Booking.toUiModel(): BookingListItem {
    val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(
        FormatStyle.MEDIUM,
        FormatStyle.SHORT
    ).withZone(ZoneId.systemDefault())

    // TODO replace the mocked details with real data when available from the API
    return BookingListItem(
        id = id.value,
        summary = BookingSummaryModel(
            date = dateFormatter.format(start),
            name = "Women’s Haircut",
            customerName = "Margarita Nikolaevna",
            attendanceStatus = AttendanceStatus.BOOKED,
            paymentStatus = BookingPaymentStatus.fromKey(status)
        )
    )
}
