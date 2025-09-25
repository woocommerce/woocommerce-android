package com.woocommerce.android.ui.bookings.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingListHandler: BookingListHandler
) : ScopedViewModel(savedStateHandle) {
    private val loadingState = MutableStateFlow(LoadingState.Idle)

    private var bookingsFetchJob: Job? = null

    val state = combine(
        bookingListHandler.bookingsFlow,
        loadingState,
    ) { bookings, loadingState ->
        State(
            bookings = bookings,
            loadingState = loadingState,
            onRefresh = { fetchBookings(LoadingState.Refreshing) },
            onLoadMore = { loadMore() }
        )
    }.asLiveData()

    init {
        fetchBookings(initialLoadingState = LoadingState.Loading)
    }

    private fun fetchBookings(initialLoadingState: LoadingState) {
        bookingsFetchJob?.cancel()
        bookingsFetchJob = launch {
            loadingState.value = initialLoadingState
            bookingListHandler.loadBookings(forceRefresh = true)
                .onFailure {
                    // Show error message
                }
            loadingState.value = LoadingState.Idle
        }
    }

    private fun loadMore() {
        launch {
            // If a fetch is already in progress, wait for it to complete before loading more
            bookingsFetchJob?.join()

            loadingState.value = LoadingState.Appending
            bookingListHandler.loadMore()
                .onFailure {
                    // Show error message
                }
            loadingState.value = LoadingState.Idle
        }
    }

    data class State(
        val bookings: List<Booking>, // To be replaced with Ui model
        val loadingState: LoadingState,
        val onRefresh: () -> Unit,
        val onLoadMore: () -> Unit
    )

    enum class LoadingState {
        Idle, Loading, Refreshing, Appending
    }
}
