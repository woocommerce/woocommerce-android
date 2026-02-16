package com.woocommerce.android.ui.woopos.bookings.note

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingOrderInfo
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosBookingNoteViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule(StandardTestDispatcher())

    private val bookingId = 42L

    private val booking = BookingEntity(
        id = RemoteId(bookingId),
        localSiteId = LocalId(1),
        start = Instant.now(),
        end = Instant.now(),
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
        orderId = 1L,
        orderItemId = 1L,
        parentId = 0L,
        personCounts = listOf(1L),
        localTimezone = "",
        attendanceStatus = BookingEntity.AttendanceStatus.Booked,
        note = "Existing note",
        order = BookingOrderInfo(),
        customerNote = ""
    )

    private val bookingsRepository = mock<BookingsRepository> {
        onBlocking { getBooking(any()) } doReturn booking
        onBlocking { updateNote(any(), any()) } doReturn Result.success(Unit)
    }

    private fun createSavedStateHandle() = SavedStateHandle(
        mapOf(BOOKING_NOTE_ROUTE_BOOKING_ID_KEY to bookingId)
    )

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = createSavedStateHandle()
    ) = WooPosBookingNoteViewModel(
        savedStateHandle = savedStateHandle,
        bookingsRepository = bookingsRepository,
    )

    @Test
    fun `given booking exists, when ViewModel created, then existing note is loaded`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.noteText).isEqualTo("Existing note")
        assertThat(state.initialNote).isEqualTo("Existing note")
    }

    @Test
    fun `given empty note, when ViewModel created, then button shows Add and is disabled`() = runTest {
        whenever(bookingsRepository.getBooking(any())).thenReturn(booking.copy(note = ""))
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.sendButtonText).isEqualTo(SendButtonText.ADD)
        assertThat(state.sendButtonState).isEqualTo(WooPosButtonState.DISABLED)
    }

    @Test
    fun `given existing note loaded, when ViewModel created, then button shows Send and is enabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.sendButtonText).isEqualTo(SendButtonText.SEND)
        assertThat(state.sendButtonState).isEqualTo(WooPosButtonState.ENABLED)
    }

    @Test
    fun `when note text changed to non-empty, then button shows Send and is enabled`() = runTest {
        whenever(bookingsRepository.getBooking(any())).thenReturn(booking.copy(note = ""))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("New note")

        val state = viewModel.state.value
        assertThat(state.noteText).isEqualTo("New note")
        assertThat(state.sendButtonText).isEqualTo(SendButtonText.SEND)
        assertThat(state.sendButtonState).isEqualTo(WooPosButtonState.ENABLED)
    }

    @Test
    fun `when note text changed to blank, then button shows Add and is disabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("   ")

        val state = viewModel.state.value
        assertThat(state.sendButtonText).isEqualTo(SendButtonText.ADD)
        assertThat(state.sendButtonState).isEqualTo(WooPosButtonState.DISABLED)
    }

    @Test
    fun `when send clicked, then note saved via repository with trimmed text`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("  New note  ")
        viewModel.onSendClicked()
        advanceUntilIdle()

        verify(bookingsRepository).updateNote(eq(bookingId), eq("New note"))
    }

    @Test
    fun `when send clicked and succeeds, then savedSuccessfully is true`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("New note")
        viewModel.onSendClicked()
        advanceUntilIdle()

        assertThat(viewModel.state.value.savedSuccessfully).isTrue()
    }

    @Test
    fun `when send clicked and fails, then saveError is true`() = runTest {
        whenever(bookingsRepository.updateNote(any(), any())).thenReturn(Result.failure(Exception("fail")))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("New note")
        viewModel.onSendClicked()
        advanceUntilIdle()

        assertThat(viewModel.state.value.saveError).isTrue()
        assertThat(viewModel.state.value.savedSuccessfully).isFalse()
    }

    @Test
    fun `when save in progress, then button shows loading state`() = runTest {
        whenever(bookingsRepository.updateNote(any(), any())).doSuspendableAnswer {
            delay(1000)
            Result.success(Unit)
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("New note")
        viewModel.onSendClicked()

        assertThat(viewModel.state.value.sendButtonState).isEqualTo(WooPosButtonState.LOADING)
    }

    @Test
    fun `when send clicked with blank note, then returns false and does not save`() = runTest {
        whenever(bookingsRepository.getBooking(any())).thenReturn(booking.copy(note = ""))
        val viewModel = createViewModel()
        advanceUntilIdle()

        val result = viewModel.onSendClicked()

        assertThat(result).isFalse()
    }

    @Test
    fun `when error shown, then saveError is cleared`() = runTest {
        whenever(bookingsRepository.updateNote(any(), any())).thenReturn(Result.failure(Exception("fail")))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("New note")
        viewModel.onSendClicked()
        advanceUntilIdle()

        assertThat(viewModel.state.value.saveError).isTrue()

        viewModel.onErrorShown()

        assertThat(viewModel.state.value.saveError).isFalse()
    }

    @Test
    fun `given booking not found, when ViewModel created, then state has empty note`() = runTest {
        whenever(bookingsRepository.getBooking(any())).thenReturn(null)
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.noteText).isEmpty()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given missing bookingId, when ViewModel created, then throws`() {
        createViewModel(savedStateHandle = SavedStateHandle())
    }
}
