package com.woocommerce.android.ui.bookings.filter.attendancestatus

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.filter.BookingFilterListItem
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.AttendanceStatus

data class BookingAttendanceStatusFilterUiState(
    val selectedStatus: AttendanceStatus? = null,
    val onStatusSelected: (AttendanceStatus?) -> Unit = {},
) {
    val items: List<BookingFilterListItem> = availableAttendanceStatuses().map { status ->
        BookingFilterListItem(
            title = UiString.UiStringRes(status.titleRes),
            selected = isSelected(status),
            onClick = { onStatusSelected(status) }
        )
    }

    private fun availableAttendanceStatuses(): List<AttendanceStatus?> = listOf(
        AttendanceStatus.any,
        AttendanceStatus.Attended,
        AttendanceStatus.Unattended,
    )

    private fun isSelected(status: AttendanceStatus?): Boolean = if (status == AttendanceStatus.any) {
        selectedStatus == null
    } else {
        selectedStatus == status
    }
}

val AttendanceStatus?.titleRes: Int
    @StringRes get() = when (this) {
        AttendanceStatus.Attended -> R.string.booking_attendance_status_attended
        AttendanceStatus.Unattended -> R.string.booking_attendance_status_unattended
        else -> R.string.bookings_filter_default
    }

val AttendanceStatus.Companion.any: AttendanceStatus?
    get() = null
