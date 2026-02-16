package com.woocommerce.android.ui.woopos.bookings.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosBookingNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingsRepository: BookingsRepository,
) : ViewModel() {

    private val bookingId: Long = requireNotNull(savedStateHandle[BOOKING_NOTE_ROUTE_BOOKING_ID_KEY])

    private val _state = MutableStateFlow(WooPosBookingNoteState())
    val state: StateFlow<WooPosBookingNoteState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>()
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _errorEvent = MutableSharedFlow<Unit>()
    val errorEvent: SharedFlow<Unit> = _errorEvent.asSharedFlow()

    init {
        loadExistingNote()
    }

    private fun loadExistingNote() {
        viewModelScope.launch {
            val booking = bookingsRepository.getBooking(bookingId)
            val existingNote = booking?.note.orEmpty()
            _state.update {
                it.copy(
                    initialNote = existingNote,
                    noteText = existingNote,
                )
            }
        }
    }

    fun onNoteChanged(text: String) {
        _state.update { it.copy(noteText = text) }
    }

    fun onSendClicked(): Boolean {
        val currentState = _state.value
        if (currentState.noteText.isBlank() || currentState.buttonState == WooPosButtonState.LOADING) {
            return false
        }

        _state.update { it.copy(buttonState = WooPosButtonState.LOADING) }
        viewModelScope.launch {
            val result = bookingsRepository.updateNote(
                bookingId = bookingId,
                note = currentState.noteText.trim()
            )
            result.onFailure {
                _state.update {
                    it.copy(buttonState = WooPosButtonState.ENABLED)
                }
                _errorEvent.emit(Unit)
            }
            result.onSuccess {
                _navigationEvent.emit(
                    WooPosNavigationEvent.GoBackWithResult(
                        key = BOOKING_NOTE_RESULT_KEY,
                        value = true
                    )
                )
            }
        }
        return true
    }

}

data class WooPosBookingNoteState(
    val initialNote: String = "",
    val noteText: String = "",
    val buttonState: WooPosButtonState = WooPosButtonState.DISABLED,
) {
    val sendButtonState: WooPosButtonState
        get() = when {
            buttonState == WooPosButtonState.LOADING -> WooPosButtonState.LOADING
            noteText.isBlank() -> WooPosButtonState.DISABLED
            else -> WooPosButtonState.ENABLED
        }

    val sendButtonText: SendButtonText
        get() = if (noteText.isBlank()) SendButtonText.ADD else SendButtonText.SEND
}

enum class SendButtonText {
    ADD, SEND
}
