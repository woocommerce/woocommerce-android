package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BookingDetailsViewModelTest : BaseUnitTest() {

    private val currencyFormatter = mock<CurrencyFormatter>()
    private val bookingMapper = BookingMapper(currencyFormatter)

    private val bookingFlow = MutableSharedFlow<Booking>()

    @Before
    fun setup() {
        whenever(currencyFormatter.formatCurrency(any<String>(), any<String>(), any())).thenReturn("$0.00")
    }

    @Test
    fun `given bookingId in SavedStateHandle, when ViewModel created, then toolbar title formatted`() {
        // Given
        val bookingId = 123L
        val savedState = SavedStateHandle(mapOf("bookingId" to bookingId))
        val resourceProvider = mock<ResourceProvider> {
            on { getString(R.string.booking_details_title, bookingId) } doReturn "Booking #$bookingId"
        }

        // When
        val viewModel = createViewModel(savedState, resourceProvider)

        // Then
        val state = viewModel.state.value
        assertThat(state?.toolbarTitle).isEqualTo("Booking #$bookingId")
    }

    @Test
    fun `when onAttendanceStatusSelected called, then state updates with new status`() {
        // Given
        val bookingId = 456L
        val savedState = SavedStateHandle(mapOf("bookingId" to bookingId))
        val resourceProvider = mock<ResourceProvider> {
            on { getString(R.string.booking_details_title, bookingId) } doReturn "Booking #$bookingId"
        }
        val viewModel = createViewModel(savedState, resourceProvider)

        val booking = getSampleBooking(1)
        testBlocking { bookingFlow.emit(booking) }

        // When
        val state = viewModel.state.getOrAwaitValue()
        state.onAttendanceStatusSelected(BookingAttendanceStatus.CANCELLED)

        // Then
        val updated = viewModel.state.value?.bookingSummary?.attendanceStatus
        assertThat(updated).isEqualTo(BookingAttendanceStatus.CANCELLED)
    }

    @Test
    fun `given booking emitted, when observed by ViewModel, then state contains mapped objects`() = testBlocking {
        // Given
        val bookingId = 789L
        val savedState = SavedStateHandle(mapOf("bookingId" to bookingId))
        val resourceProvider = mock<ResourceProvider> {
            on { getString(R.string.booking_details_title, bookingId) } doReturn "Booking #$bookingId"
        }
        val viewModel = createViewModel(savedState, resourceProvider)

        // When
        val booking = getSampleBooking(2)
        bookingFlow.emit(booking)

        // Then
        val state = viewModel.state.getOrAwaitValue()
        assertThat(state.bookingSummary).isNotNull
        assertThat(state.bookingsAppointmentDetails).isNotNull
        assertThat(state.bookingCustomerDetails).isNotNull
    }

    private fun createViewModel(
        savedState: SavedStateHandle,
        resourceProvider: ResourceProvider
    ): BookingDetailsViewModel {
        val bookingsRepository = mock<BookingsRepository> {
            on { observeBooking(any()) } doReturn bookingFlow
        }
        return BookingDetailsViewModel(
            savedState = savedState,
            resourceProvider = resourceProvider,
            bookingsRepository = bookingsRepository,
            bookingMapper = bookingMapper
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
            orderId = 1L,
            orderItemId = 1L,
            parentId = 0L,
            personCounts = listOf(1L),
            localTimezone = ""
        )
    }
}
