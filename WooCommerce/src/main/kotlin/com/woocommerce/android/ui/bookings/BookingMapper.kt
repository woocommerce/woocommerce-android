package com.woocommerce.android.ui.bookings

import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.bookings.details.CancelStatus
import com.woocommerce.android.ui.bookings.list.BookingListItem
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingCustomerInfo
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingPaymentInfo
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.math.BigDecimal
import java.time.Duration
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

class BookingMapper @Inject constructor(
    private val currencyFormatter: CurrencyFormatter,
    private val getLocations: GetLocations,
    private val resourceProvider: ResourceProvider
) {
    private val summaryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
        FormatStyle.MEDIUM,
        FormatStyle.SHORT
    ).withZone(ZoneOffset.UTC)

    private val detailsDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        .withZone(ZoneOffset.UTC)
    private val timeRangeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withZone(ZoneOffset.UTC)

    fun Booking.toBookingSummaryModel(): BookingSummaryModel {
        return BookingSummaryModel(
            date = summaryDateFormatter.format(start),
            name = order.productInfo?.name ?: "-",
            customerName = order.customerInfo?.fullName(),
            attendanceStatus = BookingAttendanceStatus.BOOKED,
            status = status.toUiModel()
        )
    }

    fun Booking.toListItem(): BookingListItem {
        return BookingListItem(
            id = id.value,
            summary = toBookingSummaryModel()
        )
    }

    fun Booking.toAppointmentDetailsModel(
        staffMemberStatus: BookingStaffMemberStatus?,
        cancelStatus: CancelStatus,
    ): BookingAppointmentDetailsModel {
        val duration = Duration.between(start, end)
            .normalizeBookingDuration()
            .toHumanReadableFormat()
        return BookingAppointmentDetailsModel(
            date = detailsDateFormatter.format(start),
            time = "${timeRangeFormatter.format(start)} - ${timeRangeFormatter.format(end)}",
            staff = staffMemberStatus,
            // TODO replace mocked values when available from API
            location = "238 Willow Creek Drive, Montgomery AL 36109",
            price = currencyFormatter.formatCurrency(cost, currency),
            cancelStatus = cancelStatus,
            duration = duration,
        )
    }

    suspend fun BookingCustomerInfo?.toCustomerDetailsModel(): BookingCustomerDetailsModel {
        if (this == null) return BookingCustomerDetailsModel.EMPTY

        return BookingCustomerDetailsModel(
            name = fullName(),
            email = billingEmail,
            phone = billingPhone,
            billingAddress = address()?.getFullAddress()
        )
    }

    fun BookingPaymentInfo.toPaymentDetailsModel(currency: String): BookingPaymentDetailsModel {
        val discount = total - subtotal
        return BookingPaymentDetailsModel(
            service = currencyFormatter.formatCurrency(subtotal, currency), // Pre-discount subtotal
            tax = currencyFormatter.formatCurrency(totalTax, currency), // Tax on total after discount
            discount = if (discount.isNotEqualTo(BigDecimal.ZERO)) {
                "- ${currencyFormatter.formatCurrency(discount.abs(), currency)}"
            } else {
                "-"
            },
            total = currencyFormatter.formatCurrency(total + totalTax, currency) // Total including tax
        )
    }

    private fun BookingEntity.Status.toUiModel(): BookingStatus = when (this) {
        BookingEntity.Status.Paid -> BookingStatus.Paid
        BookingEntity.Status.PendingConfirmation -> BookingStatus.PendingConfirmation
        BookingEntity.Status.Cancelled -> BookingStatus.Cancelled
        BookingEntity.Status.Complete -> BookingStatus.Complete
        BookingEntity.Status.Confirmed -> BookingStatus.Confirmed
        BookingEntity.Status.Unpaid -> BookingStatus.Unpaid
        is BookingEntity.Status.Unknown -> BookingStatus.Unknown(this.key)
    }

    private fun BookingCustomerInfo.fullName(): String? {
        return "${billingFirstName.orEmpty()} ${billingLastName.orEmpty()}".trim().ifEmpty { null }
    }

    fun buildCancelDialogMessage(booking: Booking): UiString {
        val customerName = booking.order.customerInfo?.fullName()?.let { UiString.UiStringText(it) }
            ?: UiString.UiStringRes(R.string.customer_detail_guest_customer)
        val serviceName = booking.order.productInfo?.name ?: "-"
        val date = detailsDateFormatter.format(booking.start)
        val time = timeRangeFormatter.format(booking.start)
        return UiString.UiStringRes(
            R.string.booking_cancel_dialog_message,
            listOf(
                customerName,
                UiString.UiStringText(serviceName),
                UiString.UiStringText(date),
                UiString.UiStringText(time)
            )
        )
    }

    /**
     * Normalize booking duration by adjusting for precision issues.
     *
     * This function handles cases where a booking duration is very close to
     * common time boundaries (days/hours) but falls short due to precision issues.
     * It rounds up durations that are within one minute of these boundaries.
     */
    private fun Duration.normalizeBookingDuration(): Duration {
        val dayInSeconds = Duration.ofDays(1).seconds
        val hourInSeconds = Duration.ofHours(1).seconds
        val minuteInSeconds = Duration.ofMinutes(1).seconds

        var durationInSeconds = this.seconds
        val boundaries = listOf(dayInSeconds, hourInSeconds)
        for (boundary in boundaries) {
            val remainder = durationInSeconds % boundary
            val difference = if (remainder == 0L) 0L else boundary - remainder
            if (difference > 0 && difference <= minuteInSeconds) {
                durationInSeconds += difference
            }
        }
        return Duration.ofSeconds(durationInSeconds)
    }

    @Suppress("LongMethod")
    private fun Duration.toHumanReadableFormat(): String {
        if (this < Duration.ofMinutes(1)) {
            return resourceProvider.getQuantityString(
                quantity = seconds.toInt(),
                default = R.string.booking_duration_seconds,
                one = R.string.booking_duration_second
            )
        }

        val days = toDays()
        val hours = minusDays(days).toHours()
        val minutes = minusDays(days).minusHours(hours).toMinutes()

        return buildString {
            if (days > 0) {
                append(
                    resourceProvider.getQuantityString(
                        quantity = days.toInt(),
                        default = R.string.booking_duration_days,
                        one = R.string.booking_duration_day
                    )
                )
            }
            if (hours > 0) {
                append(" ")
                append(
                    resourceProvider.getQuantityString(
                        quantity = hours.toInt(),
                        default = R.string.booking_duration_hours,
                        one = R.string.booking_duration_hour
                    )
                )
            }
            if (minutes > 0) {
                append(" ")
                append(
                    resourceProvider.getQuantityString(
                        quantity = minutes.toInt(),
                        default = R.string.booking_duration_minutes,
                        one = R.string.booking_duration_minute
                    )
                )
            }
        }.trim()
    }

    private suspend fun BookingCustomerInfo.address(): Address? {
        val countryCode = billingCountry ?: return null
        val (country, state) = withContext(Dispatchers.IO) {
            getLocations(countryCode, billingState.orEmpty())
        }
        return Address(
            company = billingCompany.orEmpty(),
            firstName = billingFirstName.orEmpty(),
            lastName = billingLastName.orEmpty(),
            phone = billingPhone.orEmpty(),
            country = country,
            state = state,
            address1 = billingAddress1.orEmpty(),
            address2 = billingAddress2.orEmpty(),
            city = billingCity.orEmpty(),
            postcode = billingPostcode.orEmpty(),
            email = billingEmail.orEmpty()
        )
    }
}
