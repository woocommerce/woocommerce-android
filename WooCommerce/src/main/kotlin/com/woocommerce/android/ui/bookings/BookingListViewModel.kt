package com.woocommerce.android.ui.bookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsStore
import javax.inject.Inject

@HiltViewModel
class BookingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingsStore: BookingsStore,
    private val selectedSite: SelectedSite,
) : ScopedViewModel(savedStateHandle) {
    private val isLoading = MutableStateFlow(false)

    val state = combine(
        bookingsStore.observeBookings(selectedSite.get()),
        isLoading
    ) { bookings, loading ->
        State(
            bookings = bookings,
            isLoading = loading,
            onRefresh = { refresh() }
        )
    }.asLiveData()

    init {
        launch { refresh() }
    }

    fun refresh() {
        if (isLoading.value) return
        launch {
            isLoading.value = true
            val result = bookingsStore.fetchBookings(selectedSite.get())
            if (result.isError) {
                // Surface a generic error
                triggerEvent(Event.ShowSnackbar(R.string.error_generic))
            }
            isLoading.value = false
        }
    }

    data class State(
        val bookings: List<org.wordpress.android.fluxc.persistence.entity.BookingEntity>,
        val isLoading: Boolean,
        val onRefresh: () -> Unit,
    )
}
