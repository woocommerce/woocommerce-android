package com.woocommerce.android.ui.bookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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

    val state = combine(
        bookingListHandler.bookingsFlow,
        loadingState
    ) { bookings, loadingState ->
        State(
            bookings = bookings,
            loadingState = loadingState,
            onRefresh = { fetchBookings(LoadingState.Refreshing) }
        )
    }.asLiveData()

    init {
        fetchBookings(initialLoadingState = LoadingState.Loading)
    }

    private fun fetchBookings(initialLoadingState: LoadingState) {
        launch {
            loadingState.value = initialLoadingState
            bookingListHandler.loadBookings(forceRefresh = true)
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
    )

    enum class LoadingState {
        Idle, Loading, Refreshing, Appending
    }
}
