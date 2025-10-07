package com.woocommerce.android.ui.bookings.list

import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel

data class BookingListViewState(
    val contentState: BookingListContentState,
    val tabState: BookingListTabState,
    val controlsState: BookingListControlsState,
    val sortBottomSheetState: BookingListSortBottomSheetState?,
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

data class BookingListControlsState(
    val selectedSortOption: BookingListSortOption,
    val isFilterButtonVisible: Boolean,
    val onSortClick: () -> Unit,
    val onFilterClick: () -> Unit
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

enum class BookingListSortOption {
    NewestToOldest, OldestToNewest
}

data class BookingListSortBottomSheetState(
    val selectedOption: BookingListSortOption,
    val onSelect: (BookingListSortOption) -> Unit,
    val onDismiss: () -> Unit
)
