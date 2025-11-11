package com.woocommerce.android.ui.bookings.filter.attendancestatus

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.filter.BookingFilterListItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.AttendanceStatus
import org.wordpress.android.fluxc.persistence.entity.BookingEntity

data class BookingAttendanceStatusFilterUiState(
    val selectedStatus: AttendanceStatus? = AttendanceStatus(null),
    val onStatusSelected: (AttendanceStatus) -> Unit = {},
) {
    val items: List<BookingFilterListItem> = availableAttendanceStatuses().map { status ->
        BookingFilterListItem(
            title = status.titleRes,
            selected = status == selectedStatus,
            onClick = { onStatusSelected(status) }
        )
    }

    private fun availableAttendanceStatuses(): List<AttendanceStatus> = listOf(
        AttendanceStatus(null),
        AttendanceStatus(BookingEntity.AttendanceStatus.Booked),
        AttendanceStatus(BookingEntity.AttendanceStatus.CheckedIn),
        AttendanceStatus(BookingEntity.AttendanceStatus.NoShow),
        AttendanceStatus(BookingEntity.AttendanceStatus.Cancelled),
    )
}

val AttendanceStatus.titleRes: Int
    @StringRes get() = when (this.value) {
        BookingEntity.AttendanceStatus.Booked -> R.string.booking_attendance_status_booked
        BookingEntity.AttendanceStatus.CheckedIn -> R.string.booking_attendance_status_checked_in
        BookingEntity.AttendanceStatus.NoShow -> R.string.booking_attendance_status_no_show
        BookingEntity.AttendanceStatus.Cancelled -> R.string.booking_attendance_status_cancelled
        else -> R.string.bookings_filter_default
    }
