package com.woocommerce.android.ui.bookings.reschedule

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingAvailabilityDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingOrderInfo
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.BookingResourceEntity
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingRescheduleViewModelTest : BaseUnitTest() {

    private val sampleResource = BookingResourceEntity(
        id = LocalOrRemoteId.RemoteId(RESOURCE_ID),
        localSiteId = LocalOrRemoteId.LocalId(1),
        name = "Jane Doe",
        qty = 1,
        role = null,
        email = null,
        phoneNumber = null,
        imageId = 0,
        imageUrl = null,
        description = null,
    )

    private val bookingsRepository: BookingsRepository = mock {
        on { observeResource(eq(RESOURCE_ID)) } doReturn flowOf(sampleResource)
    }

    private val sampleAvailability = BookingAvailabilityDto(
        productId = 1L,
        resourceId = 1L,
        startDate = "",
        endDate = "",
        timezoneOffset = 0,
        availability = emptyMap(),
    )

    @Test
    fun `given upcoming booking next month, when loading, then range is 1st to last day of booking month`() =
        testBlocking {
            // GIVEN
            val now = LocalDateTime.of(2026, 3, 15, 10, 0, 0)
            val bookingStart = LocalDate.of(2026, 4, 10)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
            val booking = getSampleBooking(bookingStart)

            whenever(bookingsRepository.getBooking(BOOKING_ID)).thenReturn(booking)
            whenever(bookingsRepository.fetchProductAvailability(any(), any(), any(), any()))
                .thenReturn(Result.success(sampleAvailability))

            createViewModel(now)

            // THEN
            val expectedStart = LocalDate.of(2026, 4, 1).atStartOfDay()
            val expectedEnd = LocalDate.of(2026, 4, 30).atTime(LocalTime.MAX)
            verify(bookingsRepository).fetchProductAvailability(
                productId = eq(PRODUCT_ID),
                startDate = eq(expectedStart),
                endDate = eq(expectedEnd),
                resourceId = eq(RESOURCE_ID),
            )
        }

    @Test
    fun `given booking today, when loading, then start date uses current time`() = testBlocking {
        // GIVEN
        val now = LocalDateTime.of(2026, 4, 10, 14, 30, 0)
        val bookingStart = LocalDate.of(2026, 4, 10)
            .atTime(16, 0)
            .toInstant(ZoneOffset.UTC)
        val booking = getSampleBooking(bookingStart)

        whenever(bookingsRepository.getBooking(BOOKING_ID)).thenReturn(booking)
        whenever(bookingsRepository.fetchProductAvailability(any(), any(), any(), any()))
            .thenReturn(Result.success(sampleAvailability))

        createViewModel(now)

        // THEN
        val expectedEnd = LocalDate.of(2026, 4, 30).atTime(LocalTime.MAX)
        verify(bookingsRepository).fetchProductAvailability(
            productId = eq(PRODUCT_ID),
            startDate = eq(now),
            endDate = eq(expectedEnd),
            resourceId = eq(RESOURCE_ID),
        )
    }

    @Test
    fun `given past booking, when loading, then start date is today with current time`() = testBlocking {
        // GIVEN
        val now = LocalDateTime.of(2026, 4, 15, 9, 0, 0)
        val bookingStart = LocalDate.of(2026, 4, 3)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
        val booking = getSampleBooking(bookingStart)

        whenever(bookingsRepository.getBooking(BOOKING_ID)).thenReturn(booking)
        whenever(bookingsRepository.fetchProductAvailability(any(), any(), any(), any()))
            .thenReturn(Result.success(sampleAvailability))

        createViewModel(now)

        // THEN
        val expectedEnd = LocalDate.of(2026, 4, 30).atTime(LocalTime.MAX)
        verify(bookingsRepository).fetchProductAvailability(
            productId = eq(PRODUCT_ID),
            startDate = eq(now),
            endDate = eq(expectedEnd),
            resourceId = eq(RESOURCE_ID),
        )
    }

    @Test
    fun `given booking in past month, when loading, then range uses current month`() = testBlocking {
        // GIVEN
        val now = LocalDateTime.of(2026, 5, 10, 9, 0, 0)
        val bookingStart = LocalDate.of(2026, 3, 15)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
        val booking = getSampleBooking(bookingStart)

        whenever(bookingsRepository.getBooking(BOOKING_ID)).thenReturn(booking)
        whenever(bookingsRepository.fetchProductAvailability(any(), any(), any(), any()))
            .thenReturn(Result.success(sampleAvailability))

        createViewModel(now)

        // THEN
        val expectedEnd = LocalDate.of(2026, 5, 31).atTime(LocalTime.MAX)
        verify(bookingsRepository).fetchProductAvailability(
            productId = eq(PRODUCT_ID),
            startDate = eq(now),
            endDate = eq(expectedEnd),
            resourceId = eq(RESOURCE_ID),
        )
    }

    @Test
    fun `given successful fetch, when loading, then availability is Loaded`() = testBlocking {
        // GIVEN
        val booking = getSampleBooking(Instant.now())
        whenever(bookingsRepository.getBooking(BOOKING_ID)).thenReturn(booking)
        whenever(bookingsRepository.fetchProductAvailability(any(), any(), any(), any()))
            .thenReturn(Result.success(sampleAvailability))

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val state = viewModel.state.value!!
        assertThat(state.availabilityState).isInstanceOf(BookingRescheduleState.AvailabilityState.Loaded::class.java)
    }

    @Test
    fun `when team member loads after init, then availability is fetched only once`() = testBlocking {
        // GIVEN
        val resourceFlow = MutableStateFlow<BookingResourceEntity?>(null)
        val repository = mock<BookingsRepository> {
            on { observeResource(eq(RESOURCE_ID)) } doReturn resourceFlow
        }
        val booking = getSampleBooking(Instant.now())
        whenever(repository.getBooking(BOOKING_ID)).thenReturn(booking)
        whenever(repository.fetchProductAvailability(any(), any(), any(), any()))
            .thenReturn(Result.success(sampleAvailability))

        // WHEN
        createViewModel(repository = repository)
        resourceFlow.value = sampleResource

        // THEN
        verify(repository, times(1)).fetchProductAvailability(
            productId = any(),
            startDate = any(),
            endDate = any(),
            resourceId = any(),
        )
    }

    @Test
    fun `given successful fetch, when loading, then state has correct team member`() = testBlocking {
        // GIVEN
        val booking = getSampleBooking(Instant.now())
        whenever(bookingsRepository.getBooking(BOOKING_ID)).thenReturn(booking)
        whenever(bookingsRepository.fetchProductAvailability(any(), any(), any(), any()))
            .thenReturn(Result.success(sampleAvailability))

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val state = viewModel.state.value!!
        assertThat(state.teamMemberId).isEqualTo(RESOURCE_ID)
        assertThat(state.teamMemberName).isEqualTo("Jane Doe")
    }

    @Test
    fun `given failed fetch, when loading, then availability is Error`() = testBlocking {
        // GIVEN
        val booking = getSampleBooking(Instant.now())
        whenever(bookingsRepository.getBooking(BOOKING_ID)).thenReturn(booking)
        whenever(bookingsRepository.fetchProductAvailability(any(), any(), any(), any()))
            .thenReturn(Result.failure(Exception("Network error")))

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val state = viewModel.state.value!!
        assertThat(state.availabilityState).isEqualTo(BookingRescheduleState.AvailabilityState.Error)
    }

    @Test
    fun `given booking not found, when loading, then shows error and exits`() = testBlocking {
        // GIVEN
        whenever(bookingsRepository.getBooking(BOOKING_ID)).thenReturn(null)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event).isInstanceOf(MultiLiveEvent.Event.Exit::class.java)
    }

    @Test
    fun `given booking with zero productId, when loading, then shows error and exits`() = testBlocking {
        // GIVEN
        val booking = getSampleBooking(Instant.now(), productId = 0L)
        whenever(bookingsRepository.getBooking(BOOKING_ID)).thenReturn(booking)

        // WHEN
        val viewModel = createViewModel()

        // THEN
        val event = viewModel.event.getOrAwaitValue()
        assertThat(event).isInstanceOf(MultiLiveEvent.Event.Exit::class.java)
    }

    private fun createViewModel(
        now: LocalDateTime = LocalDateTime.now(),
        repository: BookingsRepository = bookingsRepository,
    ): BookingRescheduleViewModel {
        val fixedInstant = now.toInstant(ZoneOffset.UTC)
        val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
        return BookingRescheduleViewModel(
            savedState = SavedStateHandle(mapOf("bookingId" to BOOKING_ID)),
            bookingsRepository = repository,
            clock = clock,
        ).also {
            it.state.observeForever { }
        }
    }

    private fun getSampleBooking(
        start: Instant,
        productId: Long = PRODUCT_ID,
        resourceId: Long = RESOURCE_ID,
    ): BookingEntity {
        return BookingEntity(
            id = LocalOrRemoteId.RemoteId(BOOKING_ID),
            localSiteId = LocalOrRemoteId.LocalId(1),
            start = start,
            end = start + Duration.ofHours(1),
            allDay = false,
            status = BookingEntity.Status.Confirmed,
            cost = "100.00",
            currency = "USD",
            customerId = 1L,
            productId = productId,
            resourceId = resourceId,
            dateCreated = Instant.now(),
            dateModified = Instant.now(),
            googleCalendarEventId = "",
            orderId = 1L,
            orderItemId = 1L,
            parentId = 0L,
            personCounts = listOf(1L),
            localTimezone = "",
            attendanceStatus = BookingEntity.AttendanceStatus.Unattended,
            order = BookingOrderInfo(),
            customerNote = "",
        )
    }

    companion object {
        private const val BOOKING_ID = 100L
        private const val PRODUCT_ID = 47L
        private const val RESOURCE_ID = 13L
    }
}
