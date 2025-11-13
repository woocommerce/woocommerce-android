package com.woocommerce.android.ui.bookings.filter.attendancestatus

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.filter.BookingFilterListItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.AttendanceStatuses
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.AttendanceStatus

data class BookingAttendanceStatusFilterUiState(
    val selectedStatuses: AttendanceStatuses = AttendanceStatuses.DEFAULT,
    val onStatusSelected: (AttendanceStatus?) -> Unit = {},
) {
    val items: List<BookingFilterListItem> = availableAttendanceStatuses().map { status ->
        BookingFilterListItem(
            title = status.titleRes,
            selected = isSelected(status),
            onClick = { onStatusSelected(status) }
        )
    }

    private fun availableAttendanceStatuses(): List<AttendanceStatus?> = listOf(
        AttendanceStatus.any,
        AttendanceStatus.Booked,
        AttendanceStatus.CheckedIn,
        AttendanceStatus.NoShow,
        // The Cancelled status will be added when the backend implementation is complete (WOOBO0K-574)
        // AttendanceStatus.Cancelled,
    )

    private fun isSelected(status: AttendanceStatus?): Boolean = if (status == AttendanceStatus.any) {
        selectedStatuses.values.isEmpty()
    } else {
        selectedStatuses.values.contains(status)
    }
}

val AttendanceStatus?.titleRes: Int
    @StringRes get() = when (this) {
        AttendanceStatus.Booked -> R.string.booking_attendance_status_booked
        AttendanceStatus.CheckedIn -> R.string.booking_attendance_status_checked_in
        AttendanceStatus.NoShow -> R.string.booking_attendance_status_no_show
        AttendanceStatus.Cancelled -> R.string.booking_attendance_status_cancelled
        else -> R.string.bookings_filter_default
    }

val AttendanceStatus.Companion.any: AttendanceStatus?
    get() = null
