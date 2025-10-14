package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
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

    private val currencyFormatter = mock<CurrencyFormatter>()
    private val resourceProvider = mock<ResourceProvider>()
    private val getLocations = mock<GetLocations>()
    private val bookingMapper = BookingMapper(currencyFormatter, getLocations)

    private val initialBooking = getSampleBooking(1)
    private val bookingFlow = MutableStateFlow(initialBooking)

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
        val bookingsRepository = mock<BookingsRepository> {
            on { observeBooking(any()) } doReturn bookingFlow
            onBlocking { fetchBooking(any()) } doReturn Result.success(Unit)
        }
        // When
        val networkStatus = mock<NetworkStatus> {
            on { isConnected() } doReturn true
        }
        BookingDetailsViewModel(
            savedState = savedState,
            resourceProvider = resourceProvider,
            bookingsRepository = bookingsRepository,
            bookingMapper = bookingMapper,
            networkStatus = networkStatus
        ).apply { state.observeForever { } }

        // Then
        verify(bookingsRepository, times(1)).fetchBooking(bookingId)
    }

    @Test
    fun `when offline on init, then offline snackbar shown and fetch not called`() = testBlocking {
        // Given
        val bookingId = 999L
        val savedState = SavedStateHandle(mapOf("bookingId" to bookingId))
        val bookingsRepository = mock<BookingsRepository> {
            on { observeBooking(any()) } doReturn bookingFlow
        }
        val networkStatus = mock<NetworkStatus> {
            on { isConnected() } doReturn false
        }
        val viewModel = BookingDetailsViewModel(
            savedState = savedState,
            resourceProvider = resourceProvider,
            bookingsRepository = bookingsRepository,
            bookingMapper = bookingMapper,
            networkStatus = networkStatus
        ).apply { event.observeForever { } }

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
        val bookingsRepository = mock<BookingsRepository> {
            on { observeBooking(any()) } doReturn bookingFlow
            onBlocking { fetchBooking(any()) } doReturn Result.failure(Exception("boom"))
        }
        val networkStatus = mock<NetworkStatus> {
            on { isConnected() } doReturn true
        }
        val viewModel = BookingDetailsViewModel(
            savedState = savedState,
            resourceProvider = resourceProvider,
            bookingsRepository = bookingsRepository,
            bookingMapper = bookingMapper,
            networkStatus = networkStatus
        ).apply { event.observeForever { } }

        // When
        val event = viewModel.event.getOrAwaitValue()

        // Then
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
    }

    private fun createViewModel(
        savedState: SavedStateHandle,
    ): BookingDetailsViewModel {
        val bookingsRepository = mock<BookingsRepository> {
            on { observeBooking(any()) } doReturn bookingFlow
            onBlocking { fetchBooking(any()) } doReturn Result.success(Unit)
        }
        val networkStatus = mock<NetworkStatus> {
            on { isConnected() } doReturn true
        }
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
