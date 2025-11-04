package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
class BookingDetailsViewModelTest : BaseUnitTest() {
    private val bookingId = 1L
    private val initialBooking = getSampleBooking(bookingId)
    private val bookingFlow = MutableStateFlow(initialBooking)
    private val savedStateHandle = SavedStateHandle(mapOf("mode" to BookingDetailsFragment.Mode.ShowBooking(bookingId)))

    private val currencyFormatter = mock<CurrencyFormatter>()
    private val resourceProvider = mock<ResourceProvider>()
    private val getLocations = mock<GetLocations>()
    private val bookingMapper = BookingMapper(currencyFormatter, getLocations, resourceProvider)
    private val bookingsRepository = mock<BookingsRepository> {
        on { observeBooking(any()) } doReturn bookingFlow
        onBlocking { fetchBooking(any()) } doReturn Result.success(bookingFlow.value)
    }
    private val networkStatus = mock<NetworkStatus> {
        on { isConnected() } doReturn true
    }

    @Before
    fun setup() {
        whenever(currencyFormatter.formatCurrency(any<String>(), any<String>(), any())).thenReturn("$0.00")
        whenever(
            resourceProvider.getString(
                eq(R.string.booking_details_title),
                any()
            )
        ).thenReturn("Booking #${initialBooking.id.value}")

        // Stub duration formatting strings used by BookingMapper for exact days
        whenever(
            resourceProvider.getQuantityString(
                quantity = any(),
                default = eq(R.string.booking_duration_days),
                zero = anyOrNull(),
                one = eq(R.string.booking_duration_day)
            )
        ).thenAnswer {
            val qty = it.getArgument<Int>(0)
            if (qty == 1) "$qty day" else "$qty days"
        }
    }

    @Test
    fun `given booking, when emitted after ViewModel created, then toolbar title uses booking id`() = testBlocking {
        // When
        val viewModel = createViewModel()

        // Then
        val state = viewModel.state.getOrAwaitValue()
        assertThat(state.toolbarTitle).isEqualTo("Booking #$bookingId")
    }

    @Test
    fun `when onAttendanceStatusSelected called, then updateAttendanceStatus called`() = testBlocking {
        // Given
        val viewModel = createViewModel()

        // When
        val state = viewModel.state.getOrAwaitValue()
        state.onAttendanceStatusSelected(BookingAttendanceStatus.NoShow)

        // Then
        verify(bookingsRepository, times(1)).updateAttendanceStatus(
            bookingId = initialBooking.id.value,
            attendanceStatus = BookingEntity.AttendanceStatus.NoShow
        )
    }

    @Test
    fun `given booking emitted, when observed by ViewModel, then state is updated`() = testBlocking {
        // Given
        val viewModel = createViewModel()

        // When
        val booking = getSampleBooking(2)
        bookingFlow.emit(booking)

        // Then
        val state = viewModel.state.getOrAwaitValue()
        assertThat(state.bookingUiState).isNotNull
        assertThat(state.bookingUiState?.orderId).isEqualTo(2L)
    }

    @Test
    fun `when init, then fetchBooking is triggered`() = testBlocking {
        // When
        createViewModel()

        // Then
        verify(bookingsRepository, times(1)).fetchBooking(bookingId)
    }

    @Test
    fun `when offline on init, then offline snackbar shown and fetch not called`() = testBlocking {
        // Given
        whenever(networkStatus.isConnected()).thenReturn(false)
        val viewModel = createViewModel()

        // When
        val event = viewModel.event.getOrAwaitValue()

        // Then
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        verify(bookingsRepository, times(0)).fetchBooking(any())
    }

    @Test
    fun `when fetchBooking fails, then error snackbar shown`() = testBlocking {
        // Given
        whenever(bookingsRepository.fetchBooking(any())).thenReturn(Result.failure(Exception("Fetch failed")))
        val viewModel = createViewModel()

        // When
        val event = viewModel.event.getOrAwaitValue()

        // Then
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
    }

    @Test
    fun `when onRefresh called, then fetchBooking is triggered again`() = testBlocking {
        // Given
        val viewModel = createViewModel()

        // When
        val state = viewModel.state.getOrAwaitValue()
        state.onRefresh()

        // Then
        verify(bookingsRepository, times(2)).fetchBooking(bookingId)
        verify(bookingsRepository, times(2)).fetchResource(bookingFlow.value.resourceId)
    }

    @Test
    fun `given resource not cached, when init, then show loading state`() = testBlocking {
        // Given
        whenever(bookingsRepository.observeResource(any())).thenReturn(MutableStateFlow(null))
        whenever(bookingsRepository.fetchResource(any())).doSuspendableAnswer {
            delay(100) // To make sure loading state is observed
            Result.success(Unit)
        }
        val viewModel = createViewModel()

        // When
        val state = viewModel.state.getOrAwaitValue()

        // Then
        assertThat(state.bookingUiState?.bookingsAppointmentDetails?.staff)
            .isEqualTo(BookingStaffMemberStatus.Loading)
    }

    @Test
    fun `given resource not cached, when fetchResource fails, then return Unavailable`() = testBlocking {
        // Given
        whenever(bookingsRepository.observeResource(any())).thenReturn(MutableStateFlow(null))
        whenever(bookingsRepository.fetchResource(any())).doReturn(Result.failure(Exception("Fetch failed")))
        val viewModel = createViewModel()

        // When
        val state = viewModel.state.getOrAwaitValue()
        advanceUntilIdle()

        // Then
        assertThat(state.bookingUiState?.bookingsAppointmentDetails?.staff)
            .isEqualTo(BookingStaffMemberStatus.Unavailable)
    }

    @Test
    fun `when onCancelBooking called, then cancel dialog is shown with message`() = testBlocking {
        // Given
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()

        // When
        state.onCancelBooking()

        // Then
        val updated = viewModel.state.getOrAwaitValue()
        assertThat(updated.dialogState).isNotNull
    }

    @Test
    fun `given cancel dialog shown, when onDismissCancelDialog called, then dialog is hidden`() = testBlocking {
        // Given
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()
        state.onCancelBooking()
        val stateWithDialog = viewModel.state.getOrAwaitValue()
        assertThat(stateWithDialog.dialogState).isNotNull

        // When
        stateWithDialog.dialogState?.negativeButton?.onClick()

        // Then
        val updated = viewModel.state.getOrAwaitValue()
        assertThat(updated.dialogState).isNull()
    }

    @Test
    fun `given cancel dialog shown, when onConfirmCancelBooking called, then dialog is hidden`() = testBlocking {
        // Given
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()
        state.onCancelBooking()
        val stateWithDialog = viewModel.state.getOrAwaitValue()
        assertThat(stateWithDialog.dialogState).isNotNull()

        // When
        stateWithDialog.dialogState?.positiveButton?.onClick()

        // Then
        val updated = viewModel.state.getOrAwaitValue()
        assertThat(updated.dialogState).isNull()
    }

    @Test
    fun `given cancel dialog shown, when confirm, then repository cancelBooking called and cancel status shows progress then idle`() = testBlocking {
        // Given
        whenever(bookingsRepository.cancelBooking(any())).doSuspendableAnswer {
            delay(100)
            Result.success(Unit)
        }
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()
        state.onCancelBooking()
        val stateWithDialog = viewModel.state.getOrAwaitValue()

        // When
        stateWithDialog.dialogState?.positiveButton?.onClick()

        // Then: immediately after click (operation in progress)
        val during = viewModel.state.getOrAwaitValue()
        assertThat(during.bookingUiState?.bookingsAppointmentDetails?.cancelInProgressShown).isTrue()
        verify(bookingsRepository, times(1)).cancelBooking(bookingId)

        // And after operation completes, status returns to idle
        advanceUntilIdle()
        val after = viewModel.state.getOrAwaitValue()
        assertThat(after.bookingUiState?.bookingsAppointmentDetails?.cancelInProgressShown).isFalse()
    }

    @Test
    fun `given cancel fails, when confirm, then show error snackbar and status returns idle`() = testBlocking {
        // Given
        whenever(bookingsRepository.cancelBooking(any())).thenReturn(Result.failure(Exception("Cancel failed")))
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()
        state.onCancelBooking()
        val stateWithDialog = viewModel.state.getOrAwaitValue()

        // When
        stateWithDialog.dialogState?.positiveButton?.onClick()

        // Then
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.booking_cancel_error))
        advanceUntilIdle()
        val after = viewModel.state.getOrAwaitValue()
        assertThat(after.bookingUiState?.bookingsAppointmentDetails?.cancelInProgressShown).isFalse()
    }

    @Test
    fun `when onMarkAsPaid invoked, then repository markAsPaid called and payment status shows progress then idle`() = testBlocking {
        // Given
        whenever(bookingsRepository.markAsPaid(any())).doSuspendableAnswer {
            delay(100)
            Result.success(Unit)
        }
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()

        // When
        state.onMarkAsPaid()

        // Then: immediately after click (operation in progress)
        val during = viewModel.state.getOrAwaitValue()
        assertThat(during.paymentUpdateStatus).isEqualTo(PaymentUpdateStatus.InProgress)
        verify(bookingsRepository, times(1)).markAsPaid(bookingId)

        // And after operation completes, status returns to idle
        advanceUntilIdle()
        val after = viewModel.state.getOrAwaitValue()
        assertThat(after.paymentUpdateStatus).isEqualTo(PaymentUpdateStatus.Idle)
    }

    @Test
    fun `when offline and onMarkAsPaid invoked, then offline snackbar shown and repo not called`() = testBlocking {
        // Given
        whenever(networkStatus.isConnected()).thenReturn(false)
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()

        // When
        state.onMarkAsPaid()

        // Then
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        verify(bookingsRepository, times(0)).markAsPaid(any())
    }

    @Test
    fun `given markAsPaid fails, when called, then show error snackbar and status returns idle`() = testBlocking {
        // Given
        whenever(bookingsRepository.markAsPaid(any())).thenReturn(Result.failure(Exception("Mark as paid failed")))
        val viewModel = createViewModel()
        val state = viewModel.state.getOrAwaitValue()

        // When
        state.onMarkAsPaid()

        // Then
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.booking_mark_as_paid_error))
        advanceUntilIdle()
        val after = viewModel.state.getOrAwaitValue()
        assertThat(after.paymentUpdateStatus).isEqualTo(PaymentUpdateStatus.Idle)
    }

    @Test
    fun `given Empty mode, when ViewModel created, then state is empty`() = testBlocking {
        // Given
        val savedState = SavedStateHandle(mapOf("mode" to BookingDetailsFragment.Mode.Empty))

        // When
        val viewModel = createViewModel(savedState)

        // Then
        val state = viewModel.state.getOrAwaitValue()
        assertThat(state.bookingUiState).isNull()
        assertThat(state.toolbarTitle).isEmpty()
    }

    private fun createViewModel(
        savedState: SavedStateHandle = savedStateHandle,
    ): BookingDetailsViewModel {
        return BookingDetailsViewModel(
            savedState = savedState,
            resourceProvider = resourceProvider,
            bookingsRepository = bookingsRepository,
            bookingMapper = bookingMapper,
            networkStatus = networkStatus
        ).apply {
            state.observeForever { }
        }
    }

    private fun getSampleBooking(id: Long): Booking {
        return BookingEntity(
            id = LocalOrRemoteId.RemoteId(id),
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
            orderId = id,
            orderItemId = 1L,
            parentId = 0L,
            personCounts = listOf(1L),
            localTimezone = "",
            attendanceStatus = BookingEntity.AttendanceStatus.Booked,
            order = BookingOrderInfo()
        )
    }
}
