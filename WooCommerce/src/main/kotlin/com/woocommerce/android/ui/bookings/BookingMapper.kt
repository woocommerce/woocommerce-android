package com.woocommerce.android.ui.bookings

import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.bookings.list.BookingListItem
import com.woocommerce.android.util.CurrencyFormatter
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Duration
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

class BookingMapper @Inject constructor(
    private val currencyFormatter: CurrencyFormatter,
) {
    private val summaryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
        FormatStyle.MEDIUM,
        FormatStyle.SHORT
    ).withZone(ZoneOffset.UTC)

    private val detailsDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
        .withZone(ZoneOffset.UTC)
    private val timeRangeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withZone(ZoneOffset.UTC)

    fun Booking.toBookingSummaryModel(): BookingSummaryModel {
        return BookingSummaryModel(
            date = summaryDateFormatter.format(start),
            // TODO replace mocked values when product and customer data are available
            name = "Women’s Haircut",
            customerName = "Margarita Nikolaevna",
            attendanceStatus = BookingAttendanceStatus.BOOKED,
            status = status.toUiModel()
        )
    }

    fun Booking.toUiModel(): BookingListItem {
        return BookingListItem(
            id = id.value,
            summary = toBookingSummaryModel()
        )
    }

    fun Booking.toAppointmentDetailsModel(): BookingAppointmentDetailsModel {
        val durationMinutes = Duration.between(start, end).toMinutes()
        return BookingAppointmentDetailsModel(
            date = detailsDateFormatter.format(start),
            time = "${timeRangeFormatter.format(start)} - ${timeRangeFormatter.format(end)}",
            // TODO replace mocked values when available from API
            staff = "Marianne Renoir",
            location = "238 Willow Creek Drive, Montgomery AL 36109",
            duration = "$durationMinutes min",
            price = currencyFormatter.formatCurrency(cost, currency)
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
}
