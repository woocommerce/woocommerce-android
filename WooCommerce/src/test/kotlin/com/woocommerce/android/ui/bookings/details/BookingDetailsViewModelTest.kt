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
    private val initialBooking = getSampleBooking(1)
    private val bookingFlow = MutableStateFlow(initialBooking)

    private val currencyFormatter = mock<CurrencyFormatter>()
    private val resourceProvider = mock<ResourceProvider>()
    private val getLocations = mock<GetLocations>()
    private val bookingMapper = BookingMapper(currencyFormatter, getLocations)
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
    }

    @Test
    fun `given booking, when emitted after ViewModel created, then toolbar title uses booking id`() = testBlocking {
        // Given
        val savedState = SavedStateHandle(mapOf("bookingId" to 123L))
        val expectedBookingId = 1L

        // When
        val viewModel = createViewModel(savedState)

        // Then
        val state = viewModel.state.getOrAwaitValue()
        assertThat(state.toolbarTitle).isEqualTo("Booking #$expectedBookingId")
    }

    @Test
    fun `when onAttendanceStatusSelected called, then state updates with new status`() = testBlocking {
        // Given
        val savedState = SavedStateHandle(mapOf("bookingId" to 456L))
        val viewModel = createViewModel(savedState)

        // When
        val state = viewModel.state.getOrAwaitValue()
        state.onAttendanceStatusSelected(BookingAttendanceStatus.CANCELLED)

        // Then
        val updated = viewModel.state.value?.bookingUiState?.bookingSummary?.attendanceStatus
        assertThat(updated).isEqualTo(BookingAttendanceStatus.CANCELLED)
    }

    @Test
    fun `given booking emitted, when observed by ViewModel, then state is updated`() = testBlocking {
        // Given
        val savedState = SavedStateHandle(mapOf("bookingId" to 789L))
        val viewModel = createViewModel(savedState)

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
        // Given
        val bookingId = 321L
        val savedState = SavedStateHandle(mapOf("bookingId" to bookingId))

        // When
        createViewModel(savedState = savedState)

        // Then
        verify(bookingsRepository, times(1)).fetchBooking(bookingId)
    }

    @Test
    fun `when offline on init, then offline snackbar shown and fetch not called`() = testBlocking {
        // Given
        val bookingId = 999L
        val savedState = SavedStateHandle(mapOf("bookingId" to bookingId))
        whenever(networkStatus.isConnected()).thenReturn(false)
        val viewModel = createViewModel(savedState)

        // When
        val event = viewModel.event.getOrAwaitValue()

        // Then
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        verify(bookingsRepository, times(0)).fetchBooking(any())
    }

    @Test
    fun `when fetchBooking fails, then error snackbar shown`() = testBlocking {
        // Given
        val bookingId = 111L
        val savedState = SavedStateHandle(mapOf("bookingId" to bookingId))
        whenever(bookingsRepository.fetchBooking(any())).thenReturn(Result.failure(Exception("Fetch failed")))
        val viewModel = createViewModel(savedState)

        // When
        val event = viewModel.event.getOrAwaitValue()

        // Then
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
    }

    @Test
    fun `when onRefresh called, then fetchBooking is triggered again`() = testBlocking {
        // Given
        val bookingId = 654L
        val savedState = SavedStateHandle(mapOf("bookingId" to bookingId))
        val viewModel = createViewModel(savedState)

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
        val savedState = SavedStateHandle(mapOf("bookingId" to 123L))
        whenever(bookingsRepository.observeResource(any())).thenReturn(MutableStateFlow(null))
        whenever(bookingsRepository.fetchResource(any())).doSuspendableAnswer {
            delay(100) // To make sure loading state is observed
            Result.success(Unit)
        }
        val viewModel = createViewModel(savedState)

        // When
        val state = viewModel.state.getOrAwaitValue()

        // Then
        assertThat(state.bookingUiState?.bookingsAppointmentDetails?.staff)
            .isEqualTo(BookingStaffMemberStatus.Loading)
    }

    @Test
    fun `given resource not cached, when fetchResource fails, then return Unavailable`() = testBlocking {
        // Given
        val savedState = SavedStateHandle(mapOf("bookingId" to 123L))
        whenever(bookingsRepository.observeResource(any())).thenReturn(MutableStateFlow(null))
        whenever(bookingsRepository.fetchResource(any())).doReturn(Result.failure(Exception("Fetch failed")))
        val viewModel = createViewModel(savedState)

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
        val savedState = SavedStateHandle(mapOf("bookingId" to 111L))
        val viewModel = createViewModel(savedState)
        val state = viewModel.state.getOrAwaitValue()

        // When
        state.onCancelBooking()

        // Then
        val updated = viewModel.state.getOrAwaitValue()
        assertThat(updated.cancelBookingDialogState).isNotNull
    }

    @Test
    fun `given cancel dialog shown, when onDismissCancelDialog called, then dialog is hidden`() = testBlocking {
        // Given
        val savedState = SavedStateHandle(mapOf("bookingId" to 222L))
        val viewModel = createViewModel(savedState)
        val state = viewModel.state.getOrAwaitValue()
        state.onCancelBooking()
        val stateWithDialog = viewModel.state.getOrAwaitValue()
        assertThat(stateWithDialog.cancelBookingDialogState).isNotNull

        // When
        stateWithDialog.cancelBookingDialogState?.negativeButton?.onClick()

        // Then
        val updated = viewModel.state.getOrAwaitValue()
        assertThat(updated.cancelBookingDialogState).isNull()
    }

    @Test
    fun `given cancel dialog shown, when onConfirmCancelBooking called, then dialog is hidden`() = testBlocking {
        // Given
        val savedState = SavedStateHandle(mapOf("bookingId" to 333L))
        val viewModel = createViewModel(savedState)
        val state = viewModel.state.getOrAwaitValue()
        state.onCancelBooking()
        val stateWithDialog = viewModel.state.getOrAwaitValue()
        assertThat(stateWithDialog.cancelBookingDialogState).isNotNull()

        // When
        stateWithDialog.cancelBookingDialogState?.positiveButton?.onClick()

        // Then
        val updated = viewModel.state.getOrAwaitValue()
        assertThat(updated.cancelBookingDialogState).isNull()
    }

    private fun createViewModel(
        savedState: SavedStateHandle,
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

    private fun getSampleBooking(id: Int): Booking {
        return BookingEntity(
            id = LocalOrRemoteId.RemoteId(id.toLong()),
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
            orderId = id.toLong(),
            orderItemId = 1L,
            parentId = 0L,
            personCounts = listOf(1L),
            localTimezone = "",
            order = BookingOrderInfo()
        )
    }
}
