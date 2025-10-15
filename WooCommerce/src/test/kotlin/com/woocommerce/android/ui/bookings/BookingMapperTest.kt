package com.woocommerce.android.ui.bookings

import com.woocommerce.android.R
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.ui.bookings.details.CancelStatus
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingCustomerInfo
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingOrderInfo
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingPaymentInfo
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingProductInfo
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalCoroutinesApi::class)
class BookingMapperTest : BaseUnitTest() {

    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(any<BigDecimal>(), any(), eq(true)) } doAnswer {
            val amount = it.getArgument<BigDecimal>(0)
            val currency = it.getArgument<String>(1)
            "$currency${amount.setScale(2)}"
        }
    }
    private val getLocations: GetLocations = mock()
    private val resourceProvider: ResourceProvider = mock()
    private lateinit var mapper: BookingMapper

    @Before
    fun setup() {
        // Stub ResourceProvider localized strings for durations using quantity strings
        whenever(
            resourceProvider.getQuantityString(
                quantity = any(),
                default = eq(com.woocommerce.android.R.string.booking_duration_minutes),
                zero = anyOrNull(),
                one = eq(com.woocommerce.android.R.string.booking_duration_minute)
            )
        ).thenAnswer {
            val qty = it.getArgument<Int>(0)
            if (qty == 1) "$qty minute" else "$qty minutes"
        }
        whenever(
            resourceProvider.getQuantityString(
                quantity = any(),
                default = eq(com.woocommerce.android.R.string.booking_duration_hours),
                zero = anyOrNull(),
                one = eq(com.woocommerce.android.R.string.booking_duration_hour)
            )
        ).thenAnswer {
            val qty = it.getArgument<Int>(0)
            if (qty == 1) "$qty hour" else "$qty hours"
        }
        whenever(
            resourceProvider.getQuantityString(
                quantity = any(),
                default = eq(com.woocommerce.android.R.string.booking_duration_days),
                zero = anyOrNull(),
                one = eq(com.woocommerce.android.R.string.booking_duration_day)
            )
        ).thenAnswer {
            val qty = it.getArgument<Int>(0)
            if (qty == 1) "$qty day" else "$qty days"
        }
        whenever(
            resourceProvider.getQuantityString(
                quantity = any(),
                default = eq(com.woocommerce.android.R.string.booking_duration_seconds),
                zero = anyOrNull(),
                one = eq(com.woocommerce.android.R.string.booking_duration_second)
            )
        ).thenAnswer {
            val qty = it.getArgument<Int>(0)
            if (qty == 1) "$qty second" else "$qty seconds"
        }

        mapper = BookingMapper(currencyFormatter, getLocations, resourceProvider)
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
        assertThat(model.name).isEqualTo(booking.order.productInfo?.name)
        assertThat(model.customerName)
            .isEqualTo("${booking.order.customerInfo?.billingFirstName} ${booking.order.customerInfo?.billingLastName}")
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
        val staffMemberStatus = BookingStaffMemberStatus.Loaded("Marianne Renoir")

        whenever(currencyFormatter.formatCurrency(eq("55.00"), eq("USD"), eq(true))).thenReturn("$55.00")

        val expectedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withZone(ZoneOffset.UTC)
            .format(start)
        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneOffset.UTC)
        val expectedTime = "${timeFormatter.format(start)} - ${timeFormatter.format(end)}"

        // WHEN
        val model = mapper.run { booking.toAppointmentDetailsModel(staffMemberStatus, CancelStatus.Idle) }

        // THEN
        assertThat(model.date).isEqualTo(expectedDate)
        assertThat(model.time).isEqualTo(expectedTime)
        assertThat(model.staff).isEqualTo(staffMemberStatus)
        assertThat(model.location).isEqualTo("238 Willow Creek Drive, Montgomery AL 36109")
        assertThat(model.duration).isEqualTo("1 hour 30 minutes")
        assertThat(model.price).isEqualTo("$55.00")
        assertThat(model.cancelStatus).isEqualTo(CancelStatus.Idle)
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

    @Test
    fun `given payment info with valid values, when mapped to payment details model, then maps fields correctly`() {
        // GIVEN
        val paymentInfo = BookingPaymentInfo(
            paymentMethodId = "cod",
            paymentMethodTitle = "Cash on Delivery",
            subtotal = BigDecimal("100.00"),
            subtotalTax = BigDecimal("10.00"),
            total = BigDecimal("90.00"), // With discount
            totalTax = BigDecimal("9.00")
        )
        val currency = "$"

        // WHEN
        val model = mapper.run { paymentInfo.toPaymentDetailsModel(currency) }

        // THEN
        assertThat(model.service).isEqualTo("$100.00") // subtotal
        assertThat(model.tax).isEqualTo("$9.00") // totalTax
        assertThat(model.discount).isEqualTo("- $10.00") // discount = total - subtotal = 90 - 100 = -10, abs = 10
        assertThat(model.total).isEqualTo("$99.00") // total + totalTax = 90 + 9
    }

    @Test
    fun `given payment info with zero discount, when mapped to payment details model, then shows dash for discount`() {
        // GIVEN
        val paymentInfo = BookingPaymentInfo(
            paymentMethodId = "cod",
            paymentMethodTitle = "Cash on Delivery",
            subtotal = BigDecimal("100.00"),
            subtotalTax = BigDecimal("0.00"),
            total = BigDecimal("100.00"), // No discount
            totalTax = BigDecimal("10.00")
        )
        val currency = "$"

        // WHEN
        val model = mapper.run { paymentInfo.toPaymentDetailsModel(currency) }

        // THEN
        assertThat(model.service).isEqualTo("$100.00")
        assertThat(model.tax).isEqualTo("$10.00")
        assertThat(model.discount).isEqualTo("-") // No discount
        assertThat(model.total).isEqualTo("$110.00")
    }

    @Test
    fun `given booking, when building cancel dialog message, then formats using booking details`() {
        // GIVEN
        val start = Instant.parse("2025-09-12T16:00:00Z")
        val booking = sampleBooking(start = start, end = start.plus(Duration.ofHours(1)))
        val expectedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withZone(ZoneOffset.UTC).format(start)
        val expectedTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneOffset.UTC).format(start)

        // WHEN
        val message = mapper.buildCancelDialogMessage(booking)

        // THEN
        val customerName =
            "${booking.order.customerInfo?.billingFirstName} ${booking.order.customerInfo?.billingLastName}"
        assertThat(message)
            .isEqualTo(
                UiString.UiStringRes(
                    R.string.booking_cancel_dialog_message,
                    listOf(
                        UiString.UiStringText(customerName),
                        UiString.UiStringText("${booking.order.productInfo?.name}"),
                        UiString.UiStringText(expectedDate),
                        UiString.UiStringText(expectedTime)
                    )
                )
            )
    }

    @Test
    fun `given booking without customer, when building cancel dialog message, then falls back to guest`() {
        // GIVEN
        val start = Instant.parse("2025-09-12T16:00:00Z")
        val booking = sampleBooking(start = start, customerInfo = null)
        val expectedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withZone(ZoneOffset.UTC).format(start)
        val expectedTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneOffset.UTC).format(start)

        // WHEN
        val message = mapper.buildCancelDialogMessage(booking)

        // THEN
        assertThat(message)
            .isEqualTo(
                UiString.UiStringRes(
                    R.string.booking_cancel_dialog_message,
                    listOf(
                        UiString.UiStringRes(R.string.customer_detail_guest_customer),
                        UiString.UiStringText("${booking.order.productInfo?.name}"),
                        UiString.UiStringText(expectedDate),
                        UiString.UiStringText(expectedTime)
                    )
                )
            )
    }

    @Test
    fun `given duration under one hour, when mapped to appointment details, then formats minutes`() {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start.plusSeconds(45 * 60) // 45 minutes
        val booking = sampleBooking(
            start = start,
            end = end,
        )
        val staffMemberStatus = BookingStaffMemberStatus.Loaded("Alex Doe")
        whenever(currencyFormatter.formatCurrency(eq("0.00"), eq("USD"), eq(true))).thenReturn("$0.00")

        // WHEN
        val model = mapper.run { booking.toAppointmentDetailsModel(staffMemberStatus, CancelStatus.Idle) }

        // THEN
        assertThat(model.duration).isEqualTo("45 minutes")
    }

    @Test
    fun `given duration is exact hours, when mapped to appointment details, then formats hours only`() {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start.plus(Duration.ofHours(2)) // 2 hours
        val booking = sampleBooking(
            start = start,
            end = end,
        )
        val staffMemberStatus = BookingStaffMemberStatus.Loaded("Alex Doe")
        whenever(currencyFormatter.formatCurrency(eq("0.00"), eq("USD"), eq(true))).thenReturn("$0.00")

        // WHEN
        val model = mapper.run { booking.toAppointmentDetailsModel(staffMemberStatus, CancelStatus.Idle) }

        // THEN
        assertThat(model.duration).isEqualTo("2 hours")
    }

    @Test
    fun `given duration is exact days, when mapped to appointment details, then formats days only`() {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start.plus(Duration.ofHours(24)) // 1 day
        val booking = sampleBooking(
            start = start,
            end = end,
        )
        val staffMemberStatus = BookingStaffMemberStatus.Loaded("Alex Doe")
        whenever(currencyFormatter.formatCurrency(eq("0.00"), eq("USD"), eq(true))).thenReturn("$0.00")

        // WHEN
        val model = mapper.run { booking.toAppointmentDetailsModel(staffMemberStatus, CancelStatus.Idle) }

        // THEN
        assertThat(model.duration).isEqualTo("1 day")
    }

    @Test
    fun `given duration is days plus hours, when mapped to appointment details, then formats days and hours`() {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start.plus(Duration.ofHours(27)) // 1 day 3 hours
        val booking = sampleBooking(
            start = start,
            end = end,
        )
        val staffMemberStatus = BookingStaffMemberStatus.Loaded("Alex Doe")
        whenever(currencyFormatter.formatCurrency(eq("0.00"), eq("USD"), eq(true))).thenReturn("$0.00")

        // WHEN
        val model = mapper.run { booking.toAppointmentDetailsModel(staffMemberStatus, CancelStatus.Idle) }

        // THEN
        assertThat(model.duration).isEqualTo("1 day 3 hours")
    }

    @Test
    fun `given duration is days plus hours plus minutes, when mapped to appointment details, then formats days hours and minutes`() {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start
            .plus(Duration.ofDays(1))
            .plus(Duration.ofHours(2))
            .plus(Duration.ofMinutes(15)) // 1 day 2 hours 15 minutes
        val booking = sampleBooking(
            start = start,
            end = end,
        )
        val staffMemberStatus = BookingStaffMemberStatus.Loaded("Alex Doe")
        whenever(currencyFormatter.formatCurrency(eq("0.00"), eq("USD"), eq(true))).thenReturn("$0.00")

        // WHEN
        val model = mapper.run { booking.toAppointmentDetailsModel(staffMemberStatus, CancelStatus.Idle) }

        // THEN
        assertThat(model.duration).isEqualTo("1 day 2 hours 15 minutes")
    }

    @Test
    fun `given duration under one minute, when mapped to appointment details, then formats seconds`() {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start.plusSeconds(45) // 45 seconds
        val booking = sampleBooking(
            start = start,
            end = end,
        )
        val staffMemberStatus = BookingStaffMemberStatus.Loaded("Alex Doe")
        whenever(currencyFormatter.formatCurrency(eq("0.00"), eq("USD"), eq(true))).thenReturn("$0.00")

        // WHEN
        val model = mapper.run { booking.toAppointmentDetailsModel(staffMemberStatus, CancelStatus.Idle) }

        // THEN
        assertThat(model.duration).isEqualTo("45 seconds")
    }

    @Test
    fun `given duration within one minute of one hour, when mapped, then rounds up to full hour`() {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start.plus(Duration.ofHours(1)).minusSeconds(30) // 59s or less short should round up
        val booking = sampleBooking(start = start, end = end)
        val staffMemberStatus = BookingStaffMemberStatus.Loaded("Alex Doe")
        whenever(currencyFormatter.formatCurrency(eq("0.00"), eq("USD"), eq(true))).thenReturn("$0.00")

        // WHEN
        val model = mapper.run { booking.toAppointmentDetailsModel(staffMemberStatus, CancelStatus.Idle) }

        // THEN
        assertThat(model.duration).isEqualTo("1 hour")
    }

    @Test
    fun `given duration within one minute of one day, when mapped, then rounds up to full day`() {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start.plus(Duration.ofDays(1)).minusSeconds(45) // within 1 minute of full day
        val booking = sampleBooking(start = start, end = end)
        val staffMemberStatus = BookingStaffMemberStatus.Loaded("Alex Doe")
        whenever(currencyFormatter.formatCurrency(eq("0.00"), eq("USD"), eq(true))).thenReturn("$0.00")

        // WHEN
        val model = mapper.run { booking.toAppointmentDetailsModel(staffMemberStatus, CancelStatus.Idle) }

        // THEN
        assertThat(model.duration).isEqualTo("1 day")
    }

    private fun sampleBooking(
        status: BookingEntity.Status = BookingEntity.Status.Confirmed,
        start: Instant = Instant.parse("2025-07-05T11:00:00Z"),
        end: Instant = start.plus(Duration.ofHours(1)),
        cost: String = "0.00",
        currency: String = "USD",
        customerInfo: BookingCustomerInfo? = BookingCustomerInfo(
            billingFirstName = "Margarita",
            billingLastName = "Nikolaevna"
        )
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
            localTimezone = "UTC",
            order = BookingOrderInfo(
                status = "completed",
                productInfo = BookingProductInfo(name = "Women’s Haircut"),
                customerInfo = customerInfo,
            )
        )
    }
}
