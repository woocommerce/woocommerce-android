package com.woocommerce.android.ui.bookings.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingListHandler: BookingListHandler
) : ScopedViewModel(savedStateHandle) {
    private val loadingState = MutableStateFlow(BookingListViewState.LoadingState.Idle)

    private var bookingsFetchJob: Job? = null

    val state = combine(
        bookingListHandler.bookingsFlow.map { bookings -> bookings.map { it.toUiModel() } },
        loadingState,
    ) { bookings, loadingState ->
        BookingListViewState(
            bookings = bookings,
            loadingState = loadingState,
            onRefresh = { fetchBookings(BookingListViewState.LoadingState.Refreshing) },
            onLoadMore = { loadMore() }
        )
    }.asLiveData()

    init {
        fetchBookings(initialLoadingState = BookingListViewState.LoadingState.Loading)
    }

    private fun fetchBookings(initialLoadingState: BookingListViewState.LoadingState) {
        bookingsFetchJob?.cancel()
        bookingsFetchJob = launch {
            loadingState.value = initialLoadingState
            bookingListHandler.loadBookings(forceRefresh = true)
                .onFailure {
                    // Show error message
                }
            loadingState.value = BookingListViewState.LoadingState.Idle
        }
    }

    private fun loadMore() {
        launch {
            // If a fetch is already in progress, wait for it to complete before loading more
            bookingsFetchJob?.join()

            loadingState.value = BookingListViewState.LoadingState.Appending
            bookingListHandler.loadMore()
                .onFailure {
                    // Show error message
                }
            loadingState.value = BookingListViewState.LoadingState.Idle
        }
    }
}
