package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.DateFormatter
import com.woocommerce.android.util.normalizeDuration
import com.woocommerce.android.util.toHumanReadableFormat
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingCustomerInfo
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.isAttendanceStatusEditable
import org.wordpress.android.fluxc.persistence.entity.isCancellable
import java.math.BigDecimal
import java.time.Duration
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

class WooPosBookingViewStateMapper @Inject constructor(
    private val dateFormatter: DateFormatter,
    private val resourceProvider: ResourceProvider,
    private val formatPrice: WooPosFormatPrice,
) {

    suspend fun mapToItemViewState(
        booking: BookingEntity,
        selectedBookingId: Long?,
    ): WooPosBookingsState.BookingItemViewState {
        return WooPosBookingsState.BookingItemViewState(
            id = booking.id.value,
            title = booking.order.productInfo?.name ?: "#${booking.id.value}",
            date = dateFormatter.formatDateTime(booking.start),
            total = booking.order.paymentInfo?.let { formatPrice(it.total, booking.currency) } ?: "",
            customerEmail = booking.order.customerInfo?.billingEmail?.ifBlank { null },
            isSelected = booking.id.value == selectedBookingId,
            status = mapBookingStatus(booking.status),
            statusSlug = booking.status.key,
            createdAtMillis = booking.start.toEpochMilli(),
            attendanceBadge = mapAttendanceBadge(booking.attendanceStatus),
        )
    }

    suspend fun mapToDetailsViewState(
        booking: BookingEntity,
        resourceName: String?,
    ): WooPosBookingsState.BookingDetailsViewState {
        val detailsDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withZone(ZoneOffset.UTC)

        val bookingName = booking.order.productInfo?.name ?: "#${booking.id.value}"
        val appointmentTime = formatTimeRange(booking)

        val customerInfo = booking.order.customerInfo
        val customerName = buildCustomerName(customerInfo)

        val headerSubtitle = buildString {
            append(bookingName)
            customerName?.let { append(" \u00B7 $it") }
        }

        return WooPosBookingsState.BookingDetailsViewState(
            id = booking.id.value,
            orderId = booking.orderId,
            number = "#${booking.id.value}",
            status = mapBookingStatus(booking.status),
            actionsState = WooPosBookingsState.BookingActionsState.Loaded(
                buildList {
                    add(WooPosBookingsState.BookingAction.EmailReceipt(booking.orderId))
                    if (booking.isCancellable) {
                        add(
                            WooPosBookingsState.BookingAction.CancelBooking(
                                bookingId = booking.id.value,
                                orderId = booking.orderId
                            )
                        )
                    }
                }
            ),
            headerTitle = appointmentTime,
            headerSubtitle = headerSubtitle,
            attendanceBadge = mapAttendanceBadge(booking.attendanceStatus),
            bookingName = bookingName,
            appointmentDate = detailsDateFormatter.format(booking.start),
            appointmentTime = appointmentTime,
            duration = Duration.between(booking.start, booking.end)
                .normalizeDuration()
                .toHumanReadableFormat(resourceProvider),
            teamMember = resourceName,
            location = null,
            customerSection = buildCustomerSection(customerInfo, customerName, booking.customerNote),
            attendanceSection = buildAttendanceSection(booking),
            paymentSection = buildPaymentSection(booking),
            bookingNote = booking.note.ifBlank { null },
        )
    }

    private fun buildCustomerName(customerInfo: BookingCustomerInfo?): String? {
        return listOfNotNull(
            customerInfo?.billingFirstName,
            customerInfo?.billingLastName
        ).joinToString(" ").ifBlank { null }
    }

    private fun buildAttendanceSection(
        booking: BookingEntity
    ): WooPosBookingsState.AttendanceSection? {
        if (!booking.isAttendanceStatusEditable) return null
        val selection = when (booking.attendanceStatus) {
            BookingEntity.AttendanceStatus.Attended -> WooPosBookingsState.AttendanceState.ATTENDED
            BookingEntity.AttendanceStatus.Unattended -> WooPosBookingsState.AttendanceState.UNATTENDED
            else -> null
        }
        return WooPosBookingsState.AttendanceSection(selection = selection)
    }

    private fun buildPaymentSection(
        booking: BookingEntity
    ): WooPosBookingsState.PaymentSection {
        val paymentInfo = booking.order.paymentInfo
        val currency = booking.currency
        val isPaid = booking.status == BookingEntity.Status.Paid ||
            booking.status == BookingEntity.Status.Complete ||
            booking.status == BookingEntity.Status.Cancelled

        val totalAmount = paymentInfo?.let { formatPrice(it.total + it.totalTax, currency) } ?: "-"

        return WooPosBookingsState.PaymentSection(
            serviceAmount = paymentInfo?.let { formatPrice(it.subtotal, currency) } ?: "-",
            taxAmount = paymentInfo?.let { formatPrice(it.totalTax, currency) } ?: "-",
            discountAmount = paymentInfo?.let {
                val discount = it.total - it.subtotal
                if (discount.compareTo(BigDecimal.ZERO) != 0) {
                    "-${formatPrice(discount.abs(), currency)}"
                } else {
                    "-"
                }
            } ?: "-",
            totalAmount = totalAmount,
            paidWithLabel = if (isPaid) paymentInfo?.paymentMethodTitle else null,
            collectPaymentLabel = if (!isPaid) totalAmount else null,
        )
    }

    private fun buildCustomerSection(
        customerInfo: BookingCustomerInfo?,
        customerName: String?,
        customerNote: String?,
    ): WooPosBookingsState.CustomerSection? {
        val email = customerInfo?.billingEmail?.ifBlank { null }
        val phone = customerInfo?.billingPhone?.ifBlank { null }
        val billingAddress = buildBillingAddress(customerInfo)
        val note = customerNote?.ifBlank { null }

        val hasContent = customerName != null || email != null || phone != null ||
            billingAddress != null || note != null
        if (!hasContent) return null

        return WooPosBookingsState.CustomerSection(
            name = customerName,
            email = email,
            phone = phone,
            billingAddress = billingAddress,
            note = note,
        )
    }

    private fun buildBillingAddress(customerInfo: BookingCustomerInfo?): String? {
        if (customerInfo == null) return null
        val parts = listOfNotNull(
            customerInfo.billingAddress1,
            customerInfo.billingAddress2,
            customerInfo.billingCity,
            customerInfo.billingState,
            customerInfo.billingPostcode,
            customerInfo.billingCountry,
        ).filter { it.isNotBlank() }
        return parts.joinToString(", ").ifBlank { null }
    }

    private fun mapAttendanceBadge(
        status: BookingEntity.AttendanceStatus
    ): WooPosBookingsState.AttendanceState? {
        return when (status) {
            BookingEntity.AttendanceStatus.Attended -> WooPosBookingsState.AttendanceState.ATTENDED
            BookingEntity.AttendanceStatus.Unattended -> WooPosBookingsState.AttendanceState.UNATTENDED
            else -> null
        }
    }

    private fun formatTimeRange(booking: BookingEntity): String {
        val startTime = dateFormatter.formatTime(booking.start)
        val endTime = dateFormatter.formatTime(booking.end)
        val amPmPattern = Regex("\\s*([AaPp][Mm])$")
        val startSuffix = amPmPattern.find(startTime)?.groupValues?.get(1)
        val endSuffix = amPmPattern.find(endTime)?.groupValues?.get(1)
        return if (startSuffix != null && startSuffix.equals(endSuffix, ignoreCase = true)) {
            "${startTime.replace(amPmPattern, "")}-$endTime"
        } else {
            "$startTime-$endTime"
        }
    }

    fun mapBookingStatus(status: BookingEntity.Status): WooPosBookingStatus {
        val colorKey = when (status) {
            BookingEntity.Status.Complete -> WooPosBookingStatusColorKey.COMPLETED
            BookingEntity.Status.Paid -> WooPosBookingStatusColorKey.COMPLETED
            BookingEntity.Status.Confirmed -> WooPosBookingStatusColorKey.PROCESSING
            BookingEntity.Status.PendingConfirmation -> WooPosBookingStatusColorKey.ON_HOLD
            BookingEntity.Status.Unpaid -> WooPosBookingStatusColorKey.ON_HOLD
            BookingEntity.Status.Cancelled -> WooPosBookingStatusColorKey.FAILED
            BookingEntity.Status.InCart -> WooPosBookingStatusColorKey.OTHER
            is BookingEntity.Status.Unknown -> WooPosBookingStatusColorKey.OTHER
        }
        return WooPosBookingStatus(
            text = status.key.replaceFirstChar { it.uppercase() }.replace("-", " "),
            colorKey = colorKey
        )
    }
}
