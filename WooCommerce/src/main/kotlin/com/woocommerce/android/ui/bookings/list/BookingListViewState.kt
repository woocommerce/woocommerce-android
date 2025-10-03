package com.woocommerce.android.ui.bookings.list

import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class BookingListViewState(
    val contentState: BookingListContentState,
    val tabState: BookingListTabState,
    val searchState: BookingListSearchState
) {
    // TODO combine with other filters when available
    val areFiltersActive: Boolean
        get() = tabState.selectedTab != BookingListTab.All
}

data class BookingListContentState(
    val bookings: List<BookingListItem>,
    val loadingState: BookingListLoadingState,
    val onRefresh: () -> Unit,
    val onLoadMore: () -> Unit,
    val onBookingClick: (Long) -> Unit,
) {
    fun isNotEmpty() = bookings.isNotEmpty()
}

data class BookingListSearchState(
    val query: String?,
    val onQueryChanged: (String?) -> Unit
) {
    val isSearchActive: Boolean
        get() = query != null
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
            attendanceStatus = BookingAttendanceStatus.BOOKED,
            status = status.toUiModel()
        )
    )
}

private fun BookingEntity.Status.toUiModel(): BookingStatus = when (this) {
    BookingEntity.Status.Paid -> BookingStatus.Paid
    BookingEntity.Status.PendingConfirmation -> BookingStatus.PendingConfirmation
    BookingEntity.Status.Cancelled -> BookingStatus.Cancelled
    BookingEntity.Status.Complete -> BookingStatus.Complete
    BookingEntity.Status.Confirmed -> BookingStatus.Confirmed
    BookingEntity.Status.Unpaid -> BookingStatus.Unpaid
    is BookingEntity.Status.Unknown -> BookingStatus.Unknown(this.key)
}
