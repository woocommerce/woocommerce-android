package com.woocommerce.android.ui.woopos.bookings

import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingDto
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

class WooPosBookingMapper @Inject constructor() {

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    fun toListItem(
        dto: BookingDto,
        selectedId: Long?,
        customerName: String? = null,
        productName: String? = null,
    ): BookingListItem {
        val zone = ZoneId.systemDefault()
        val startInstant = Instant.ofEpochSecond(dto.start)
        val zonedStart = startInstant.atZone(zone)

        return BookingListItem(
            id = dto.id,
            orderId = dto.orderId,
            customerName = customerName ?: "Customer #${dto.customerId}",
            serviceName = productName ?: "Product #${dto.productId}",
            startTime = timeFormatter.format(zonedStart),
            amount = formatCost(dto.cost, dto.currency),
            bookingStatus = mapBookingStatus(dto.status),
            attendanceStatus = dto.attendanceStatus?.let { mapAttendanceStatus(it) },
            isSelected = dto.id == selectedId,
        )
    }

    fun toDetail(
        dto: BookingDto,
        customerName: String? = null,
        productName: String? = null,
    ): BookingDetail {
        val zone = ZoneId.systemDefault()
        val startInstant = Instant.ofEpochSecond(dto.start)
        val endInstant = Instant.ofEpochSecond(dto.end)
        val zonedStart = startInstant.atZone(zone)
        val zonedEnd = endInstant.atZone(zone)

        val status = BookingEntity.Status.fromKey(dto.status)
        val isCancellable = status !is BookingEntity.Status.Cancelled &&
            status !is BookingEntity.Status.InCart &&
            status !is BookingEntity.Status.Complete

        val isPayable = (
            status is BookingEntity.Status.Unpaid ||
                status is BookingEntity.Status.PendingConfirmation ||
                status is BookingEntity.Status.Confirmed
            ) &&
            dto.orderId != 0L

        return BookingDetail(
            id = dto.id,
            orderId = dto.orderId,
            customerName = customerName ?: "Customer #${dto.customerId}",
            serviceName = productName ?: "Product #${dto.productId}",
            startDate = dateFormatter.format(zonedStart),
            startTime = timeFormatter.format(zonedStart),
            endTime = timeFormatter.format(zonedEnd),
            amount = formatCost(dto.cost, dto.currency),
            currency = dto.currency,
            bookingStatus = mapBookingStatus(dto.status),
            attendanceStatus = dto.attendanceStatus?.let { mapAttendanceStatus(it) },
            isCancellable = isCancellable,
            isAttendanceEditable = status !is BookingEntity.Status.Cancelled,
            hasLinkedOrder = dto.orderId != 0L,
            isPayable = isPayable,
            attendanceUpdateInProgress = false,
            cancelInProgress = false,
            paymentUpdateInProgress = false,
        )
    }

    private fun mapBookingStatus(statusKey: String): BookingStatusUi {
        return when (BookingEntity.Status.fromKey(statusKey)) {
            is BookingEntity.Status.Unpaid -> BookingStatusUi.Unpaid
            is BookingEntity.Status.PendingConfirmation -> BookingStatusUi.PendingConfirmation
            is BookingEntity.Status.Confirmed -> BookingStatusUi.Confirmed
            is BookingEntity.Status.Paid -> BookingStatusUi.Paid
            is BookingEntity.Status.Cancelled -> BookingStatusUi.Cancelled
            is BookingEntity.Status.Complete -> BookingStatusUi.Complete
            is BookingEntity.Status.InCart -> BookingStatusUi.InCart
            is BookingEntity.Status.Unknown -> BookingStatusUi.Unknown
        }
    }

    private fun mapAttendanceStatus(statusKey: String): AttendanceStatusUi? {
        return when (BookingEntity.AttendanceStatus.fromKey(statusKey)) {
            is BookingEntity.AttendanceStatus.Booked -> AttendanceStatusUi.Booked
            is BookingEntity.AttendanceStatus.CheckedIn -> AttendanceStatusUi.CheckedIn
            is BookingEntity.AttendanceStatus.NoShow -> AttendanceStatusUi.NoShow
            is BookingEntity.AttendanceStatus.Cancelled -> AttendanceStatusUi.Cancelled
            is BookingEntity.AttendanceStatus.Unknown -> null
        }
    }

    @Suppress("SwallowedException")
    private fun formatCost(cost: String, currencyCode: String): String {
        return try {
            val amount = BigDecimal(cost)
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.currency = Currency.getInstance(currencyCode.uppercase())
            format.format(amount)
        } catch (e: NumberFormatException) {
            "$currencyCode $cost"
        } catch (e: IllegalArgumentException) {
            "$currencyCode $cost"
        }
    }
}
