package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
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

class WooPosBookingViewStateMapperTest {

    companion object {
        private const val FORMATTED_TIME_RANGE = "11:00 AM – 12:00 PM"
    }

    private val formatPrice: WooPosFormatPrice = mock {
        on { invoke(any<BigDecimal>(), any()) } doAnswer { invocation ->
            val price = invocation.arguments[0] as BigDecimal
            "$${price.toPlainString()}"
        }
    }
    private val timeRangeFormatter: WooPosBookingTimeRangeFormatter = mock()
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

    private val paymentStatusResolver: WooPosPaymentStatusResolver = mock()

    private lateinit var mapper: WooPosBookingViewStateMapper

    @Before
    fun setup() {
        whenever(timeRangeFormatter.format(any(), any())).thenReturn(FORMATTED_TIME_RANGE)
        mapper = WooPosBookingViewStateMapper(resourceProvider, formatPrice, paymentStatusResolver, timeRangeFormatter)
    }

    @Test
    fun `given booking, when mapped to item view state, then maps fields correctly`() = runTest {
        // GIVEN
        val booking = sampleBooking()
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.UNPAID)

        // WHEN
        val result = mapper.mapToItemViewState(booking, selectedBookingId = null)

        // THEN
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.timeRange).isEqualTo(FORMATTED_TIME_RANGE)
        assertThat(result.subtitle).isEqualTo("Women's Haircut \u00B7 Margarita Nikolaevna")
        assertThat(result.isSelected).isFalse()
        assertThat(result.paymentStatus).isEqualTo(PaymentStatus.UNPAID)
        assertThat(result.isCancelled).isFalse()
    }

    @Test
    fun `given cancelled booking, when mapped to item view state, then isCancelled is true`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Cancelled)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.FAILED)

        // WHEN
        val result = mapper.mapToItemViewState(booking, selectedBookingId = null)

        // THEN
        assertThat(result.isCancelled).isTrue()
        assertThat(result.paymentStatus).isEqualTo(PaymentStatus.FAILED)
    }

    @Test
    fun `given booking, when mapped to details view state, then formats date time and duration correctly`() = runTest {
        // GIVEN
        val start = Instant.parse("2025-07-05T11:00:00Z")
        val end = start.plus(Duration.ofMinutes(90))
        val booking = sampleBooking(start = start, end = end)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.PAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        val expectedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withZone(ZoneOffset.UTC)
            .format(start)
        assertThat(result.appointmentDate).isEqualTo(expectedDate)
        assertThat(result.appointmentTime).isEqualTo(FORMATTED_TIME_RANGE)
        assertThat(result.duration).isEqualTo("1 hour 30 minutes")
    }

    @Test
    fun `given booking with null paymentInfo, when mapped to details, then payment section shows dashes`() = runTest {
        // GIVEN
        val booking = sampleBooking(paymentInfo = null)
        whenever(paymentStatusResolver.resolve(any(), anyOrNull())).thenReturn(PaymentStatus.UNPAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        assertThat(result.paymentSection.serviceAmount).isEqualTo("-")
        assertThat(result.paymentSection.taxAmount).isEqualTo("-")
        assertThat(result.paymentSection.discountAmount).isEqualTo("-")
        assertThat(result.paymentSection.totalAmount).isEqualTo("-")
    }

    @Test
    fun `given booking with discount, when mapped to details, then discount is negative formatted`() = runTest {
        // GIVEN
        val paymentInfo = BookingPaymentInfo(
            paymentMethodId = "cod",
            paymentMethodTitle = "Cash on Delivery",
            subtotal = BigDecimal("100"),
            subtotalTax = BigDecimal("0"),
            total = BigDecimal("90"),
            totalTax = BigDecimal("9"),
        )
        val booking = sampleBooking(paymentInfo = paymentInfo)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.PAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        assertThat(result.paymentSection.discountAmount).isEqualTo("-$10")
        assertThat(result.paymentSection.totalAmount).isEqualTo("$99")
    }

    @Test
    fun `given booking with editable attendance status CheckedIn, when mapped, then attendance section shows ATTENDED`() = runTest {
        // GIVEN
        val booking = sampleBooking(
            status = BookingEntity.Status.Confirmed,
            attendanceStatus = BookingEntity.AttendanceStatus.Attended,
        )
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.PAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        assertThat(result.attendanceSection).isNotNull
        assertThat(result.attendanceSection?.selection).isEqualTo(WooPosBookingsState.AttendanceState.ATTENDED)
    }

    @Test
    fun `given booking with non-editable attendance, when mapped, then attendance section is null`() = runTest {
        // GIVEN
        val booking = sampleBooking(
            status = BookingEntity.Status.Cancelled,
            attendanceStatus = BookingEntity.AttendanceStatus.Unattended,
        )
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.FAILED)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        assertThat(result.attendanceSection).isNull()
    }

    @Test
    fun `given booking with null customerInfo, when mapped, then customer section is null`() = runTest {
        // GIVEN
        val booking = sampleBooking(customerInfo = null, customerNote = null)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.UNPAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        assertThat(result.customerSection).isNull()
    }

    @Test
    fun `given booking with blank fields, when mapped, then fields resolve to null`() = runTest {
        // GIVEN
        val customerInfo = BookingCustomerInfo(
            billingFirstName = "",
            billingLastName = "",
            billingEmail = "",
            billingPhone = "",
        )
        val booking = sampleBooking(customerInfo = customerInfo, customerNote = "")
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.UNPAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        assertThat(result.customerSection).isNull()
    }

    @Test
    fun `given paid booking, when mapped to details, then actions include EmailReceipt`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Paid)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.PAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
        assertThat(actions).anyMatch { it is WooPosBookingsState.BookingAction.EmailReceipt }
    }

    @Test
    fun `given unpaid booking, when mapped to details, then actions do not include EmailReceipt`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Unpaid)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.UNPAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
        assertThat(actions).noneMatch { it is WooPosBookingsState.BookingAction.EmailReceipt }
    }

    @Test
    fun `given paid booking, when mapped to details, then actions include IssueRefund`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Paid)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.PAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
        assertThat(actions).anyMatch { it is WooPosBookingsState.BookingAction.IssueRefund }
    }

    @Test
    fun `given complete booking, when mapped to details, then actions include IssueRefund`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Complete)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.PAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
        assertThat(actions).anyMatch { it is WooPosBookingsState.BookingAction.IssueRefund }
    }

    @Test
    fun `given unpaid booking, when mapped to details, then actions do not include IssueRefund`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Unpaid)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.UNPAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
        assertThat(actions).noneMatch { it is WooPosBookingsState.BookingAction.IssueRefund }
    }

    @Test
    fun `given cancelled booking, when mapped to details, then actions do not include IssueRefund`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Cancelled)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.FAILED)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
        assertThat(actions).noneMatch { it is WooPosBookingsState.BookingAction.IssueRefund }
    }

    @Test
    fun `given cancellable booking, when mapped to details, then actions include CancelBooking`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Unpaid)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.UNPAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
        assertThat(actions).anyMatch { it is WooPosBookingsState.BookingAction.CancelBooking }
    }

    @Test
    fun `given cancelled booking, when mapped to details, then actions do not include CancelBooking`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Cancelled)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.FAILED)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
        assertThat(actions).noneMatch { it is WooPosBookingsState.BookingAction.CancelBooking }
    }

    @Test
    fun `given cancelled unpaid booking, when mapped to details, then collectPaymentLabel is shown`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Cancelled)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.UNPAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        assertThat(result.paymentSection.collectPaymentLabel).isNotNull()
    }

    @Test
    fun `given cancelled paid booking, when mapped to details, then collectPaymentLabel is null`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Cancelled)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.PAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        assertThat(result.paymentSection.collectPaymentLabel).isNull()
    }

    @Test
    fun `given complete booking, when mapped to details, then actions do not include CancelBooking`() = runTest {
        // GIVEN
        val booking = sampleBooking(status = BookingEntity.Status.Complete)
        whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.PAID)

        // WHEN
        val result = mapper.mapToDetailsViewState(booking, resourceName = null)

        // THEN
        val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
        assertThat(actions).noneMatch { it is WooPosBookingsState.BookingAction.CancelBooking }
    }

    @Test
    fun `given any booking, when mapped to details, then actions always include ViewOrder with correct orderId`() =
        runTest {
            // GIVEN
            val booking = sampleBooking(id = 42L)
            whenever(paymentStatusResolver.resolve(any(), any())).thenReturn(PaymentStatus.UNPAID)

            // WHEN
            val result = mapper.mapToDetailsViewState(booking, resourceName = null)

            // THEN
            val actions = (result.actionsState as WooPosBookingsState.BookingActionsState.Loaded).actions
            val viewOrderAction = actions.filterIsInstance<WooPosBookingsState.BookingAction.ViewOrder>()
            assertThat(viewOrderAction).hasSize(1)
            assertThat(viewOrderAction.first().orderId).isEqualTo(420L)
        }

    private fun sampleBooking(
        id: Long = 1L,
        status: BookingEntity.Status = BookingEntity.Status.Confirmed,
        start: Instant = Instant.parse("2025-07-05T11:00:00Z"),
        end: Instant = start.plus(Duration.ofHours(1)),
        attendanceStatus: BookingEntity.AttendanceStatus = BookingEntity.AttendanceStatus.Unattended,
        customerInfo: BookingCustomerInfo? = BookingCustomerInfo(
            billingFirstName = "Margarita",
            billingLastName = "Nikolaevna",
            billingEmail = "margarita@example.com",
            billingPhone = "555-1234",
        ),
        paymentInfo: BookingPaymentInfo? = BookingPaymentInfo(
            paymentMethodId = "cod",
            paymentMethodTitle = "Cash on Delivery",
            subtotal = BigDecimal("55"),
            subtotalTax = BigDecimal("0"),
            total = BigDecimal("55"),
            totalTax = BigDecimal("5.50"),
        ),
        customerNote: String? = "Customer Note",
    ): BookingEntity {
        return BookingEntity(
            id = RemoteId(id),
            localSiteId = LocalId(1),
            start = start,
            end = end,
            allDay = false,
            status = status,
            cost = "55.00",
            currency = "USD",
            customerId = 1L,
            productId = 1L,
            resourceId = 1L,
            dateCreated = start,
            dateModified = end,
            googleCalendarEventId = "",
            orderId = id * 10,
            orderItemId = 1L,
            parentId = 0L,
            personCounts = null,
            localTimezone = "UTC",
            attendanceStatus = attendanceStatus,
            customerNote = customerNote,
            note = "",
            order = BookingOrderInfo(
                productInfo = BookingProductInfo(name = "Women's Haircut"),
                customerInfo = customerInfo,
                paymentInfo = paymentInfo,
            ),
        )
    }
}
