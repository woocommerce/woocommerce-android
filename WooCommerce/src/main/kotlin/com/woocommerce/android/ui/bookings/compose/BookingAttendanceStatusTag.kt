package com.woocommerce.android.ui.bookings.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTag
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingAttendanceStatusTag(
    state: BookingAttendanceStatus,
    modifier: Modifier = Modifier,
) {
    WCTag(
        text = state.text(),
        backgroundColor = state.backgroundColor(),
        textColor = colorResource(R.color.tagView_text),
        fontWeight = FontWeight.Normal,
        modifier = modifier
    )
}

enum class BookingAttendanceStatus {
    BOOKED, CHECKED_IN, NO_SHOW, CANCELLED
}

@Composable
fun BookingAttendanceStatus.text(): String {
    return when (this) {
        BookingAttendanceStatus.BOOKED -> R.string.booking_attendance_status_booked
        BookingAttendanceStatus.CHECKED_IN -> R.string.booking_attendance_status_checked_in
        BookingAttendanceStatus.CANCELLED -> R.string.booking_attendance_status_cancelled
        BookingAttendanceStatus.NO_SHOW -> R.string.booking_attendance_status_no_show
    }.let { stringResource(it) }
}

@Composable
fun BookingAttendanceStatus.backgroundColor(): Color {
    return when (this) {
        BookingAttendanceStatus.NO_SHOW -> R.color.tag_bg_booking_yellow
        BookingAttendanceStatus.BOOKED,
        BookingAttendanceStatus.CHECKED_IN,
        BookingAttendanceStatus.CANCELLED -> R.color.tagView_bg
    }.let { colorResource(it) }
}

@Preview
@Composable
private fun AttendanceStatusTagPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.BOOKED
        )
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttendanceStatusTagDarkPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.CHECKED_IN
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun AttendanceStatusTagNoShowPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.NO_SHOW,
            modifier = Modifier.padding(10.dp)
        )
    }
}
