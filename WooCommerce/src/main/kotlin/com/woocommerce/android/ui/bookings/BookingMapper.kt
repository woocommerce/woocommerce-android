package com.woocommerce.android.ui.bookings

import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.bookings.list.BookingListItem
import com.woocommerce.android.util.CurrencyFormatter
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
    private val getLocations: GetLocations
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
        staffMemberStatus: BookingStaffMemberStatus?
    ): BookingAppointmentDetailsModel {
        val durationMinutes = Duration.between(start, end).toMinutes()
        return BookingAppointmentDetailsModel(
            date = detailsDateFormatter.format(start),
            time = "${timeRangeFormatter.format(start)} - ${timeRangeFormatter.format(end)}",
            staff = staffMemberStatus,
            // TODO replace mocked values when available from API
            location = "238 Willow Creek Drive, Montgomery AL 36109",
            duration = "$durationMinutes min",
            price = currencyFormatter.formatCurrency(cost, currency)
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
