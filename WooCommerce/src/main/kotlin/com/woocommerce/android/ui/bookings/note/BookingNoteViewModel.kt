package com.woocommerce.android.ui.bookings.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingNoteViewModel @Inject constructor(
    savedState: SavedStateHandle,
    bookingsRepository: BookingsRepository,
) : ScopedViewModel(savedState) {

    private val navArgs: BookingNoteFragmentArgs by savedState.navArgs()

    private val initialNoteState = MutableStateFlow("")
    private val editedNoteState = MutableStateFlow("")

    val state: LiveData<BookingNoteViewState> = combine(
        initialNoteState,
        editedNoteState
    ) { initialNote, editedNote ->
        BookingNoteViewState(
            initialNote = initialNote,
            editedNote = editedNote,
            onNoteChange = ::onNoteChange
        )
    }.asLiveData()

    init {
        launch {
            val booking = bookingsRepository.getBooking(navArgs.bookingId)
            if (booking != null) {
                val initialNote = booking.note
                initialNoteState.value = initialNote
                editedNoteState.value = initialNote
            } else {
                triggerEvent(MultiLiveEvent.Event.Exit)
            }
        }
    }

    private fun onNoteChange(value: String) {
        editedNoteState.value = value
    }
}
