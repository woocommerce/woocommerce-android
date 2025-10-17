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

sealed interface BookingAttendanceStatus {
    data object Booked : BookingAttendanceStatus
    data object CheckedIn : BookingAttendanceStatus
    data object NoShow : BookingAttendanceStatus
    data object Cancelled : BookingAttendanceStatus
    data class Unknown(val key: String) : BookingAttendanceStatus
}

@Composable
fun BookingAttendanceStatus.text(): String {
    return when (this) {
        BookingAttendanceStatus.Booked -> stringResource(R.string.booking_attendance_status_booked)
        BookingAttendanceStatus.CheckedIn -> stringResource(R.string.booking_attendance_status_checked_in)
        BookingAttendanceStatus.Cancelled -> stringResource(R.string.booking_attendance_status_cancelled)
        BookingAttendanceStatus.NoShow -> stringResource(R.string.booking_attendance_status_no_show)
        is BookingAttendanceStatus.Unknown -> key
    }
}

@Composable
fun BookingAttendanceStatus.backgroundColor(): Color {
    return when (this) {
        BookingAttendanceStatus.NoShow -> R.color.tag_bg_booking_yellow
        else -> R.color.tagView_bg
    }.let { colorResource(it) }
}

@Preview
@Composable
private fun AttendanceStatusTagPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.Booked
        )
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttendanceStatusTagDarkPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.CheckedIn
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun AttendanceStatusTagNoShowPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.NoShow,
            modifier = Modifier.padding(10.dp)
        )
    }
}
