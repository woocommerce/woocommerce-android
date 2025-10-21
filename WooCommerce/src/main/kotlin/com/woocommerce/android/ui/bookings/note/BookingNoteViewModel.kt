package com.woocommerce.android.ui.bookings.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class BookingNoteViewModel @Inject constructor(
    savedState: SavedStateHandle,
    bookingsRepository: BookingsRepository,
) : ScopedViewModel(savedState) {

    private val navArgs: BookingNoteFragmentArgs by savedState.navArgs()

    private val bookingFlow = bookingsRepository.observeBooking(navArgs.bookingId)
        .filterNotNull()
        .take(1)

    val state: LiveData<BookingNoteViewState> = combine(
        bookingFlow
    ) { booking ->
        BookingNoteViewState(
            note = booking[0].note
        )
    }.asLiveData()
}
