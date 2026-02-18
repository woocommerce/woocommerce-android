package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingTimeRangeFormatter
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.BookingInfo
import com.woocommerce.android.util.DateFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosBookingInfoLoaderTest : BaseUnitTest() {

    private val bookingsRepository: BookingsRepository = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val dateFormatter: DateFormatter = mock()
    private val timeRangeFormatter: WooPosBookingTimeRangeFormatter = mock()

    private val sut = WooPosBookingInfoLoader(
        bookingsRepository = bookingsRepository,
        resourceProvider = resourceProvider,
        dateFormatter = dateFormatter,
        timeRangeFormatter = timeRangeFormatter,
    )

    private fun createBooking(): Booking = mock<Booking>().apply {
        whenever(start).thenReturn(Instant.parse("2025-03-15T10:00:00Z"))
        whenever(end).thenReturn(Instant.parse("2025-03-15T11:00:00Z"))
    }

    @Test
    fun `given booking cached, when resolveBookingInfo, then returns Loaded`() = testBlocking {
        val booking = createBooking()
        whenever(bookingsRepository.getBooking(42L)).thenReturn(booking)
        whenever(dateFormatter.formatDate(any<Instant>())).thenReturn("Mar 15, 2025")
        whenever(timeRangeFormatter.format(any(), any())).thenReturn("10:00 AM-11:00 AM")
        whenever(
            resourceProvider.getString(
                eq(R.string.woopos_orders_details_booking_info),
                any(),
                any(),
                any()
            )
        ).thenReturn("Booking #42 · Mar 15, 2025, 10:00 AM-11:00 AM")

        val result = sut.resolveBookingInfo(42L)

        assertThat(result).isInstanceOf(BookingInfo.Loaded::class.java)
        assertThat((result as BookingInfo.Loaded).text)
            .isEqualTo("Booking #42 · Mar 15, 2025, 10:00 AM-11:00 AM")
    }

    @Test
    fun `given booking not cached, when resolveBookingInfo, then returns Loading`() = testBlocking {
        whenever(bookingsRepository.getBooking(42L)).thenReturn(null)

        val result = sut.resolveBookingInfo(42L)

        assertThat(result).isInstanceOf(BookingInfo.Loading::class.java)
        assertThat((result as BookingInfo.Loading).bookingId).isEqualTo(42L)
    }

    @Test
    fun `given fetch succeeds, when fetchBookingInfo, then returns Loaded`() = testBlocking {
        val booking = createBooking()
        whenever(bookingsRepository.fetchBooking(42L)).thenReturn(Result.success(booking))
        whenever(dateFormatter.formatDate(any<Instant>())).thenReturn("Mar 15, 2025")
        whenever(timeRangeFormatter.format(any(), any())).thenReturn("10:00 AM-11:00 AM")
        whenever(
            resourceProvider.getString(
                eq(R.string.woopos_orders_details_booking_info),
                any(),
                any(),
                any()
            )
        ).thenReturn("Booking #42 · Mar 15, 2025, 10:00 AM-11:00 AM")

        val result = sut.fetchBookingInfo(42L)

        assertThat(result).isInstanceOf(BookingInfo.Loaded::class.java)
        assertThat((result as BookingInfo.Loaded).text)
            .isEqualTo("Booking #42 · Mar 15, 2025, 10:00 AM-11:00 AM")
    }

    @Test
    fun `given fetch fails, when fetchBookingInfo, then returns Error`() = testBlocking {
        whenever(bookingsRepository.fetchBooking(42L)).thenReturn(
            Result.failure(RuntimeException("Network error"))
        )
        whenever(resourceProvider.getString(R.string.woopos_orders_details_booking_info_error))
            .thenReturn("Failed to load booking info")

        val result = sut.fetchBookingInfo(42L)

        assertThat(result).isInstanceOf(BookingInfo.Error::class.java)
        assertThat((result as BookingInfo.Error).text).isEqualTo("Failed to load booking info")
    }
}
