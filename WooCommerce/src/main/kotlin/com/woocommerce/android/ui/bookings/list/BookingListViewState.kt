package com.woocommerce.android.ui.bookings.list

import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel

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
