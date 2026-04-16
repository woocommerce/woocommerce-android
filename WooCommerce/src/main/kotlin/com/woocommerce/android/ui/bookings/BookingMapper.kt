package com.woocommerce.android.ui.bookings

import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsUiModel
import com.woocommerce.android.ui.bookings.compose.BookingLocationStatus
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.bookings.details.AttendanceUpdateStatus
import com.woocommerce.android.ui.bookings.details.CancelStatus
import com.woocommerce.android.ui.bookings.list.BookingListItem
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateFormatter
import com.woocommerce.android.util.normalizeDuration
import com.woocommerce.android.util.toHumanReadableFormat
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingCustomerInfo
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingPaymentInfo
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.isAttendanceStatusEditable
import org.wordpress.android.fluxc.persistence.entity.isCancellable
import java.math.BigDecimal
import java.time.Duration
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

class BookingMapper @Inject constructor(
    private val dateFormatter: DateFormatter,
    private val currencyFormatter: CurrencyFormatter,
    private val getLocations: GetLocations,
    private val resourceProvider: ResourceProvider
) {
    private val detailsDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        .withZone(ZoneOffset.UTC)

    fun Booking.toBookingSummaryModel(
        paymentStatus: PaymentStatus,
        attendanceUpdateStatus: AttendanceUpdateStatus,
    ): BookingSummaryModel {
        val isCancelled = status == BookingEntity.Status.Cancelled
        return BookingSummaryModel(
            date = dateFormatter.formatDateTime(start),
            name = order.productInfo?.name ?: "-",
            customerName = order.customerInfo?.fullName(),
            paymentStatus = paymentStatus,
            isCancelled = isCancelled,
            attendanceStatus = if (isCancelled) null else attendanceStatus.toUiModel(),
            attendanceUpdateStatus = attendanceUpdateStatus,
        )
    }

    fun Booking.toListItem(paymentStatus: PaymentStatus): BookingListItem {
        return BookingListItem(
            id = id.value,
            summary = toBookingSummaryModel(paymentStatus, AttendanceUpdateStatus.Idle)
        )
    }

    fun Booking.toAppointmentDetailsModel(
        staffMemberStatus: BookingStaffMemberStatus?,
        cancelStatus: CancelStatus,
        rescheduleButtonVisible: Boolean = false,
        attendanceUpdateStatus: AttendanceUpdateStatus = AttendanceUpdateStatus.Idle,
        locationStatus: BookingLocationStatus = BookingLocationStatus.Loading,
    ): BookingAppointmentDetailsModel {
        val duration = Duration.between(start, end)
            .normalizeDuration()
            .toHumanReadableFormat(resourceProvider)
        return BookingAppointmentDetailsModel(
            date = detailsDateFormatter.format(start),
            time = "${dateFormatter.formatTime(start)} - ${dateFormatter.formatTime(end)}",
            staff = staffMemberStatus,
            location = locationStatus,
            cancelStatus = cancelStatus,
            cancelButtonVisible = isCancellable,
            rescheduleButtonVisible = rescheduleButtonVisible,
            duration = duration,
            attendanceStatus = attendanceStatus.toUiModel(),
            isAttendanceStatusEditable = isAttendanceStatusEditable,
            attendanceUpdateStatus = attendanceUpdateStatus,
        )
    }

    suspend fun BookingCustomerInfo?.toCustomerDetailsModel(customerNote: String?): BookingCustomerDetailsUiModel {
        if (this == null) return BookingCustomerDetailsUiModel.EMPTY

        return BookingCustomerDetailsUiModel(
            name = fullName(),
            email = billingEmail,
            phone = billingPhone,
            billingAddress = address()?.getFullAddress(),
            customerNote = customerNote?.ifEmpty { null }
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

    private fun BookingEntity.AttendanceStatus.toUiModel(): BookingAttendanceStatus? = when (this) {
        BookingEntity.AttendanceStatus.Attended -> BookingAttendanceStatus.Attended
        BookingEntity.AttendanceStatus.Unattended -> BookingAttendanceStatus.Unattended
        is BookingEntity.AttendanceStatus.Unknown -> null
    }

    private fun BookingCustomerInfo.fullName(): String? {
        return "${billingFirstName.orEmpty()} ${billingLastName.orEmpty()}".trim().ifEmpty { null }
    }

    fun buildCancelDialogMessage(booking: Booking): UiString {
        val customerName = booking.order.customerInfo?.fullName()?.let { UiString.UiStringText(it) }
            ?: UiString.UiStringRes(R.string.customer_detail_guest_customer)
        val serviceName = booking.order.productInfo?.name ?: "-"
        val date = detailsDateFormatter.format(booking.start)
        val time = dateFormatter.formatTime(booking.start)
        return UiString.UiStringRes(
            R.string.booking_cancel_dialog_message_v2,
            listOf(
                customerName,
                UiString.UiStringText(serviceName),
                UiString.UiStringText(date),
                UiString.UiStringText(time)
            )
        )
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
