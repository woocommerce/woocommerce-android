package com.woocommerce.android.ui.woopos.bookings.note

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsAnalyticsTracker
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
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
        attendanceStatus = BookingEntity.AttendanceStatus.Unattended,
        note = "Existing note",
        order = BookingOrderInfo(),
        customerNote = ""
    )

    private val analyticsTracker: WooPosBookingsAnalyticsTracker = mock()
    private val bookingsRepository = mock<BookingsRepository> {
        on { getBooking(any()) } doReturn booking
        on { updateNote(any(), any()) } doReturn Result.success(Unit)
    }

    private fun createSavedStateHandle() = SavedStateHandle(
        mapOf(BOOKING_NOTE_ROUTE_BOOKING_ID_KEY to bookingId)
    )

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = createSavedStateHandle()
    ) = WooPosBookingNoteViewModel(
        savedStateHandle = savedStateHandle,
        bookingsRepository = bookingsRepository,
        analyticsTracker = analyticsTracker,
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
    fun `given empty note, when ViewModel created, then save button is disabled`() = runTest {
        whenever(bookingsRepository.getBooking(any())).thenReturn(booking.copy(note = ""))
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.saveButtonState).isEqualTo(WooPosButtonState.DISABLED)
    }

    @Test
    fun `given existing note loaded, when ViewModel created, then save button is disabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.saveButtonState).isEqualTo(WooPosButtonState.DISABLED)
    }

    @Test
    fun `when note text changed to different value, then save button is enabled`() = runTest {
        whenever(bookingsRepository.getBooking(any())).thenReturn(booking.copy(note = ""))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("New note")

        val state = viewModel.state.value
        assertThat(state.noteText).isEqualTo("New note")
        assertThat(state.saveButtonState).isEqualTo(WooPosButtonState.ENABLED)
    }

    @Test
    fun `given existing note, when note text cleared, then save button is enabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("")

        val state = viewModel.state.value
        assertThat(state.saveButtonState).isEqualTo(WooPosButtonState.ENABLED)
    }

    @Test
    fun `given existing note, when note text unchanged, then save button is disabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("Existing note")

        assertThat(viewModel.state.value.saveButtonState).isEqualTo(WooPosButtonState.DISABLED)
    }

    @Test
    fun `given existing note, when only whitespace differs, then save button is disabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("Existing note  ")

        assertThat(viewModel.state.value.saveButtonState).isEqualTo(WooPosButtonState.DISABLED)
    }

    @Test
    fun `when save clicked, then note saved via repository with trimmed text`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("  New note  ")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        verify(bookingsRepository).updateNote(eq(bookingId), eq("New note"))
    }

    @Test
    fun `when save clicked and succeeds, then navigation event emitted`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onNoteChanged("New note")
            viewModel.onSaveClicked()

            val event = awaitItem()
            assertThat(event).isEqualTo(
                WooPosNavigationEvent.GoBackWithResult(
                    key = BOOKING_NOTE_RESULT_KEY,
                    value = true
                )
            )
        }
    }

    @Test
    fun `when save clicked and fails, then error event emitted`() = runTest {
        whenever(bookingsRepository.updateNote(any(), any())).thenReturn(Result.failure(Exception("fail")))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.errorEvent.test {
            viewModel.onNoteChanged("New note")
            viewModel.onSaveClicked()

            awaitItem()
        }
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
        viewModel.onSaveClicked()

        assertThat(viewModel.state.value.saveButtonState).isEqualTo(WooPosButtonState.LOADING)
    }

    @Test
    fun `when save clicked with blank note, then repository called with empty trimmed text`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNoteChanged("   ")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        verify(bookingsRepository).updateNote(eq(bookingId), eq(""))
    }

    @Test
    fun `given booking not found, when ViewModel created, then state has empty note`() = runTest {
        whenever(bookingsRepository.getBooking(any())).thenReturn(null)
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.noteText).isEmpty()
    }

    @Test
    fun `given existing note, when ViewModel created, then isEditMode is true`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isEditMode).isTrue()
    }

    @Test
    fun `given empty note, when ViewModel created, then isEditMode is false`() = runTest {
        whenever(bookingsRepository.getBooking(any())).thenReturn(booking.copy(note = ""))
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isEditMode).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given missing bookingId, when ViewModel created, then throws`() {
        createViewModel(savedStateHandle = SavedStateHandle())
    }
}
