package com.woocommerce.android.ui.bookings.note

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingOrderInfo
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BookingNoteViewModelTest : BaseUnitTest() {

    private val bookingId = 42L

    private val booking = BookingEntity(
        id = LocalOrRemoteId.RemoteId(bookingId),
        localSiteId = LocalOrRemoteId.LocalId(1),
        start = Instant.now(),
        end = Instant.now() + Duration.ofDays(1),
        allDay = false,
        status = BookingEntity.Status.Confirmed,
        cost = "100.00",
        currency = "USD",
        customerId = 1L,
        productId = 1L,
        resourceId = 1L,
        dateCreated = Instant.now(),
        dateModified = Instant.now(),
        googleCalendarEventId = "",
        orderId = bookingId,
        orderItemId = 1L,
        parentId = 0L,
        personCounts = listOf(1L),
        localTimezone = "",
        attendanceStatus = BookingEntity.AttendanceStatus.Attended,
        note = "Initial note",
        order = BookingOrderInfo(),
        customerNote = "Customer Note"
    )

    private val savedStateHandle: SavedStateHandle = BookingNoteFragmentArgs(bookingId).toSavedStateHandle()

    private val bookingsRepository = mock<BookingsRepository> {
        on { getBooking(any()) } doReturn booking
        on { updateNote(any(), any()) } doReturn Result.success(Unit)
    }

    @Test
    fun `given booking exists, when ViewModel created, then initial and edited notes are set`() = testBlocking {
        // Given + When
        val viewModel = createViewModel()

        // Then
        val state = viewModel.state.getOrAwaitValue()
        assertThat(state.initialNote).isEqualTo(booking.note)
        assertThat(state.editedNote).isEqualTo(booking.note)
        // save not visible when same (after trim)
        assertThat(state.isSaveVisible).isFalse()
        assertThat(state.isSaveEnabled).isTrue()
        assertThat(state.noteEditable).isTrue()
    }

    @Test
    fun `given booking missing, when ViewModel created, then exit event is triggered`() = testBlocking {
        // Given
        whenever(bookingsRepository.getBooking(any())).thenReturn(null)

        // When
        val viewModel = createViewModel()

        // Then
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `when onNoteChange called, then edited note and save visibility update`() = testBlocking {
        // Given
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()

        // When: whitespace-only change should not make save visible
        state.onNoteChange("  ${booking.note}   ")
        val updated1 = viewModel.state.getOrAwaitValue()

        // Then
        assertThat(updated1.editedNote).isEqualTo("  ${booking.note}   ")
        assertThat(updated1.isSaveVisible).isFalse()

        // When: real change
        val newNote = "New note"
        updated1.onNoteChange(newNote)
        val updated2 = viewModel.state.getOrAwaitValue()

        // Then
        assertThat(updated2.editedNote).isEqualTo(newNote)
        assertThat(updated2.isSaveVisible).isTrue()
    }

    @Test
    fun `when onSaveClicked succeeds, then repository called with trimmed note and exit event triggered`() = testBlocking {
        // Given
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()
        state.onNoteChange("  New note   ")
        viewModel.state.getOrAwaitValue()

        // When
        state.onSaveClicked()

        // Then
        verify(bookingsRepository, times(1)).updateNote(eq(bookingId), eq("New note"))
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
        // status goes back to Idle at the end
        val finalState = viewModel.state.getOrAwaitValue()
        assertThat(finalState.noteSaveStatus).isEqualTo(NoteSaveStatus.Idle)
    }

    @Test
    fun `when onSaveClicked in progress, then UI disables editing and save button`() = testBlocking {
        // Given: make updateNote suspend for a while to catch InProgress
        whenever(bookingsRepository.updateNote(any(), any())).doSuspendableAnswer {
            delay(100)
            Result.success(Unit)
        }
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()
        state.onNoteChange("Changed")
        viewModel.state.getOrAwaitValue()

        // When
        state.onSaveClicked()

        // Then: immediately after click, status should become InProgress
        val inProgress = viewModel.state.getOrAwaitValue()
        assertThat(inProgress.noteSaveStatus).isEqualTo(NoteSaveStatus.InProgress)
        assertThat(inProgress.isSaveEnabled).isFalse()
        assertThat(inProgress.noteEditable).isFalse()

        // Let it finish
        advanceUntilIdle()
        val finalState = viewModel.state.getOrAwaitValue()
        assertThat(finalState.noteSaveStatus).isEqualTo(NoteSaveStatus.Idle)
    }

    @Test
    fun `when onSaveClicked fails, then snackbar shown and status returns to Idle`() = testBlocking {
        // Given
        whenever(bookingsRepository.updateNote(any(), any())).thenReturn(Result.failure(Exception("fail")))
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()
        state.onNoteChange("Changed")
        viewModel.state.getOrAwaitValue()

        // When
        state.onSaveClicked()

        // Then
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.booking_note_screen_update_error))
        val finalState = viewModel.state.getOrAwaitValue()
        assertThat(finalState.noteSaveStatus).isEqualTo(NoteSaveStatus.Idle)
    }

    @Test
    fun `given no changes, when back pressed, then exit is triggered and no dialog`() = testBlocking {
        // Given
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()

        // When
        state.onBackPressed()

        // Then
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
        val latestState = viewModel.state.getOrAwaitValue()
        assertThat(latestState.dialogState).isNull()
    }

    @Test
    fun `given changed note, when back pressed, then show discard dialog with correct copy and buttons`() = testBlocking {
        // Given
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()
        state.onNoteChange("Changed note")
        viewModel.state.getOrAwaitValue()

        // When
        state.onBackPressed()

        // Then
        val withDialog = viewModel.state.getOrAwaitValue()
        val dialog = withDialog.dialogState
        check(dialog != null)
        // Verify message and buttons types/ids
        assertThat(dialog.message).isInstanceOf(UiString.UiStringRes::class.java)
        assertThat((dialog.message as UiString.UiStringRes).stringRes)
            .isEqualTo(R.string.booking_note_discard_changes_message)
        val positive = dialog.positiveButton
        val negative = dialog.negativeButton
        check(positive != null && negative != null)
        assertThat((positive.text as UiString.UiStringRes).stringRes)
            .isEqualTo(R.string.booking_note_discard_changes_discard)
        assertThat((negative.text as UiString.UiStringRes).stringRes)
            .isEqualTo(R.string.cancel)

        // Clicking negative should dismiss dialog and not exit
        negative.onClick()
        val afterDismiss = viewModel.state.getOrAwaitValue()
        assertThat(afterDismiss.dialogState).isNull()
    }

    @Test
    fun `when confirm discard in dialog, then exit is triggered`() = testBlocking {
        // Given
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()
        state.onNoteChange("Changed note")
        viewModel.state.getOrAwaitValue()
        state.onBackPressed()
        val withDialog = viewModel.state.getOrAwaitValue()
        val positive = withDialog.dialogState?.positiveButton
        check(positive != null)

        // When
        positive.onClick()

        // Then
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `when save in progress and back pressed, then do nothing (no dialog, no exit)`() = testBlocking {
        // Given a slow repository update to keep InProgress state
        whenever(bookingsRepository.updateNote(any(), any())).doSuspendableAnswer {
            delay(200)
            Result.success(Unit)
        }
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()
        state.onNoteChange("Changed")
        viewModel.state.getOrAwaitValue()

        // Start save
        state.onSaveClicked()
        val inProgress = viewModel.state.getOrAwaitValue()
        assertThat(inProgress.noteSaveStatus).isEqualTo(NoteSaveStatus.InProgress)

        // When
        inProgress.onBackPressed()

        // Then: dialog remains null while in progress
        val stillInProgress = viewModel.state.getOrAwaitValue()
        assertThat(stillInProgress.dialogState).isNull()

        // Clean up
        advanceUntilIdle()
    }

    private fun createViewModel(
        savedState: SavedStateHandle = savedStateHandle,
    ): BookingNoteViewModel {
        return BookingNoteViewModel(
            savedState = savedState,
            bookingsRepository = bookingsRepository
        ).apply {
            state.observeForever { }
        }
    }
}
