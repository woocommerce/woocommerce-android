package com.woocommerce.android.ui.bookings

import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalCoroutinesApi::class)
class BookingMapperTest : BaseUnitTest() {

    private val currencyFormatter: CurrencyFormatter = mock()
    private lateinit var mapper: BookingMapper

    @Before
    fun setup() {
        mapper = BookingMapper(currencyFormatter)
    }

    @Test
    fun `given booking, when mapped to summary model, then maps fields correctly`() {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start.plusSeconds(60 * 60)
        val booking = sampleBooking(
            status = BookingEntity.Status.Confirmed,
            start = start,
            end = end,
            cost = "55.00",
            currency = "USD"
        )

        val expectedDate = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneOffset.UTC)
            .format(start)

        // WHEN
        val model = mapper.run { booking.toBookingSummaryModel() }

        // THEN
        assertThat(model.date).isEqualTo(expectedDate)
        assertThat(model.name).isEqualTo("Women’s Haircut")
        assertThat(model.customerName).isEqualTo("Margarita Nikolaevna")
        assertThat(model.attendanceStatus).isEqualTo(BookingAttendanceStatus.BOOKED)
        assertThat(model.status).isEqualTo(BookingStatus.Confirmed)
    }

    @Test
    fun `given booking, when mapped to appointment details model, then maps fields and formats price and time correctly`() {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start.plusSeconds(90 * 60) // 90 minutes
        val booking = sampleBooking(
            status = BookingEntity.Status.Paid,
            start = start,
            end = end,
            cost = "55.00",
            currency = "USD"
        )

        whenever(currencyFormatter.formatCurrency(eq("55.00"), eq("USD"), eq(true))).thenReturn("$55.00")

        val expectedDate = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
            .withZone(ZoneOffset.UTC)
            .format(start)
        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneOffset.UTC)
        val expectedTime = "${timeFormatter.format(start)} - ${timeFormatter.format(end)}"

        // WHEN
        val model = mapper.run { booking.toAppointmentDetailsModel() }

        // THEN
        assertThat(model.date).isEqualTo(expectedDate)
        assertThat(model.time).isEqualTo(expectedTime)
        assertThat(model.staff).isEqualTo("Marianne Renoir")
        assertThat(model.location).isEqualTo("238 Willow Creek Drive, Montgomery AL 36109")
        assertThat(model.duration).isEqualTo("90 min")
        assertThat(model.price).isEqualTo("$55.00")
    }

    @Test
    fun `given booking with unknown status, when mapped, then preserves Unknown key`() {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Unknown("weird-status"))

        // WHEN
        val model = mapper.run { booking.toBookingSummaryModel() }

        // THEN
        assertThat(model.status).isInstanceOf(BookingStatus.Unknown::class.java)
        val unknown = model.status as BookingStatus.Unknown
        assertThat(unknown.key).isEqualTo("weird-status")
    }

    private fun sampleBooking(
        status: BookingEntity.Status = BookingEntity.Status.Confirmed,
        start: Instant = Instant.parse("2025-07-05T11:00:00Z"),
        end: Instant = start.plus(Duration.ofHours(1)),
        cost: String = "0.00",
        currency: String = "USD"
    ): BookingEntity {
        return BookingEntity(
            id = LocalOrRemoteId.RemoteId(1L),
            localSiteId = LocalOrRemoteId.LocalId(1),
            start = start,
            end = end,
            allDay = false,
            status = status,
            cost = cost,
            currency = currency,
            customerId = 1L,
            productId = 1L,
            resourceId = 1L,
            dateCreated = start,
            dateModified = end,
            googleCalendarEventId = "",
            orderId = 1L,
            orderItemId = 1L,
            parentId = 0L,
            personCounts = listOf(1L),
            localTimezone = "UTC"
        )
    }
}
