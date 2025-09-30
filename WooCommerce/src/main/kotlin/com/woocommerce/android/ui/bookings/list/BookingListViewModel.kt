package com.woocommerce.android.ui.bookings.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import javax.inject.Inject

@HiltViewModel
class BookingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingListHandler: BookingListHandler,
    private val filtersBuilder: BookingListFiltersBuilder
) : ScopedViewModel(savedStateHandle) {
    private val loadingState = MutableStateFlow(BookingListLoadingState.Idle)
    private val selectedTab = savedStateHandle.getStateFlow(viewModelScope, BookingListTab.Today)

    private var bookingsFetchJob: Job? = null
    private var bookingsLoadMoreJob: Job? = null

    private val contentState = combine(
        bookingListHandler.bookingsFlow.map { bookings -> bookings.map { it.toUiModel() } },
        loadingState
    ) { bookings, loadingState ->
        BookingListContentState(
            bookings = bookings,
            loadingState = loadingState,
            onRefresh = { fetchBookings(BookingListLoadingState.Refreshing) },
            onLoadMore = ::loadMore,
            onBookingClick = ::onBookingClick
        )
    }

    val state = combine(
        contentState,
        selectedTab
    ) { contentState, selectedTab ->
        BookingListViewState(
            contentState = contentState,
            tabState = BookingListTabState(
                selectedTab = selectedTab,
                onTabChanged = ::onTabChanged
            ),
            controlsState = BookingListControlsState(
                onSortClick = ::onSortClicked,
                onFilterClick = ::onFilterClicked
            )
        )
    }.asLiveData()

    init {
        monitorFilterChanges()
    }

    private fun monitorFilterChanges() {
        launch {
            selectedTab.collectLatest {
                // Cancel any ongoing fetch or load more operations
                bookingsFetchJob?.cancel()
                bookingsLoadMoreJob?.cancel()

                bookingsFetchJob = fetchBookings(BookingListLoadingState.Loading)
            }
        }
    }

    private fun fetchBookings(initialLoadingState: BookingListLoadingState) = launch {
        loadingState.value = initialLoadingState
        bookingListHandler.loadBookings(
            forceRefresh = true,
            filters = prepareFilters()
        ).onFailure {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
        }
        loadingState.value = BookingListLoadingState.Idle
    }

    private fun loadMore() {
        bookingsLoadMoreJob?.cancel()
        bookingsLoadMoreJob = launch {
            // If a fetch is already in progress, wait for it to complete before loading more
            bookingsFetchJob?.join()

            loadingState.value = BookingListLoadingState.Appending
            bookingListHandler.loadMore()
                .onFailure {
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
                }
            loadingState.value = BookingListLoadingState.Idle
        }
    }

    private fun onBookingClick(bookingId: Long) {
        triggerEvent(NavigateToBookingDetails(bookingId))
    }

    private fun onTabChanged(tab: BookingListTab) {
        selectedTab.value = tab
    }

    private fun onSortClicked() {
        // TODO Show sorting bottom sheet
    }

    private fun onFilterClicked() {
        // TODO Show filter bottom sheet
    }

    private fun prepareFilters(): List<BookingsFilterOption> = with(filtersBuilder) {
        listOfNotNull(
            selectedTab.value.asDateRangeFilter()
        )
    }

    data class NavigateToBookingDetails(val bookingId: Long) : MultiLiveEvent.Event()
}
