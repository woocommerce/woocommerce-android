package com.woocommerce.android.ui.bookings.list

import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel

data class BookingListViewState(
    val contentState: BookingListContentState,
    val tabState: BookingListTabState,
    val controlsState: BookingListControlsState,
    val sortBottomSheetState: BookingListSortBottomSheetState?,
    val searchState: BookingListSearchState,
    val showBackButton: Boolean,
    val onBackClick: () -> Unit
) {
    val toolbarTitle: UiString
        get() = if (searchState.isSearchActive) {
            UiString.UiStringText("")
        } else {
            UiString.UiStringRes(R.string.bookings_tab_title)
        }
}

data class BookingListContentState(
    val bookings: List<BookingListItem>,
    val loadingState: BookingListLoadingState,
    val selectedBooking: Long? = null,
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
    val enabledFiltersCount: Int,
    val onSortClick: () -> Unit,
    val onFilterClick: () -> Unit,
    val onClearFiltersClick: () -> Unit
) {
    val areFiltersActive: Boolean
        get() = enabledFiltersCount > 0
}

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
