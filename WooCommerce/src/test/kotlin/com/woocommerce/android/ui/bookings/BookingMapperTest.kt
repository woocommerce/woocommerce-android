package com.woocommerce.android.ui.bookings

import com.woocommerce.android.R
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.ui.bookings.details.AttendanceUpdateStatus
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
    private val resourceProvider: ResourceProvider = mock {
        on { getQuantityString(any(), any(), anyOrNull(), anyOrNull()) } doAnswer { invocation ->
            val quantity = invocation.arguments[0] as Int
            val default = invocation.arguments[1] as Int
            val one = invocation.arguments[3] as Int?
            when (quantity) {
                1 -> when (one) {
                    R.string.booking_duration_second -> "1 second"
                    R.string.booking_duration_minute -> "1 minute"
                    R.string.booking_duration_hour -> "1 hour"
                    R.string.booking_duration_day -> "1 day"
                    else -> ""
                }

                else -> when (default) {
                    R.string.booking_duration_seconds -> "$quantity seconds"
                    R.string.booking_duration_minutes -> "$quantity minutes"
                    R.string.booking_duration_hours -> "$quantity hours"
                    R.string.booking_duration_days -> "$quantity days"
                    else -> ""
                }
            }
        }
    }
    private lateinit var mapper: BookingMapper

    @Before
    fun setup() {
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
        val model = mapper.run { booking.toBookingSummaryModel(AttendanceUpdateStatus.Idle) }

        // THEN
        assertThat(model.date).isEqualTo(expectedDate)
        assertThat(model.name).isEqualTo(booking.order.productInfo?.name)
        assertThat(model.customerName)
            .isEqualTo("${booking.order.customerInfo?.billingFirstName} ${booking.order.customerInfo?.billingLastName}")
        assertThat(model.attendanceStatus).isEqualTo(BookingAttendanceStatus.Booked)
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
        val model = mapper.run { booking.toBookingSummaryModel(AttendanceUpdateStatus.Idle) }

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
    fun `given on-hold order with COD payment method, when mapped to summary model, then status is PayOnSite`() {
        // GIVEN
        val booking = sampleBooking().let { original ->
            val paymentInfo = BookingPaymentInfo(
                paymentMethodId = "cod",
                paymentMethodTitle = "Cash on Delivery",
                subtotal = BigDecimal("55.00"),
                subtotalTax = BigDecimal("0.00"),
                total = BigDecimal("55.00"),
                totalTax = BigDecimal("0.00")
            )
            val orderWithPayment = original.order.copy(paymentInfo = paymentInfo, status = "on-hold")
            original.copy(order = orderWithPayment)
        }

        // WHEN
        val model = mapper.run { booking.toBookingSummaryModel(AttendanceUpdateStatus.Idle) }

        // THEN
        assertThat(model.status).isEqualTo(BookingStatus.PayOnSite)
    }

    @Test
    fun `given cancellable statuses, when mapped to appointment details, then cancel button visible`() {
        whenever(currencyFormatter.formatCurrency(any<String>(), any(), eq(true))).thenAnswer {
            val amount = it.getArgument<String>(0)
            val currency = it.getArgument<String>(1)
            "$currency$amount"
        }
        val statuses = listOf(
            BookingEntity.Status.Confirmed,
            BookingEntity.Status.Paid,
            BookingEntity.Status.Unpaid,
            BookingEntity.Status.PendingConfirmation,
            BookingEntity.Status.Unknown("some-new-status")
        )
        statuses.forEach { status ->
            val booking = sampleBooking(status = status, cost = "55.00", currency = "USD")
            val model =
                mapper.run { booking.toAppointmentDetailsModel(BookingStaffMemberStatus.Loading, CancelStatus.Idle) }
            assertThat(model.cancelButtonVisible).describedAs("status $status").isTrue()
        }
    }

    @Test
    fun `given non-cancellable statuses, when mapped to appointment details, then cancel button hidden`() {
        whenever(currencyFormatter.formatCurrency(any<String>(), any(), eq(true))).thenAnswer {
            val amount = it.getArgument<String>(0)
            val currency = it.getArgument<String>(1)
            "$currency$amount"
        }
        val statuses = listOf(
            BookingEntity.Status.Cancelled,
            BookingEntity.Status.InCart,
            BookingEntity.Status.Complete
        )
        statuses.forEach { status ->
            val booking = sampleBooking(status = status, cost = "55.00", currency = "USD")
            val model =
                mapper.run { booking.toAppointmentDetailsModel(BookingStaffMemberStatus.Loading, CancelStatus.Idle) }
            assertThat(model.cancelButtonVisible).describedAs("status $status").isFalse()
        }
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
            attendanceStatus = BookingEntity.AttendanceStatus.Booked,
            order = BookingOrderInfo(
                status = "completed",
                productInfo = BookingProductInfo(name = "Women’s Haircut"),
                customerInfo = customerInfo,
            )
        )
    }
}
