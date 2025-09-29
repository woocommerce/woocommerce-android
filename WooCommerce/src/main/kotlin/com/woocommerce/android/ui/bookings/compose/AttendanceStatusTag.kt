package com.woocommerce.android.ui.bookings.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTag
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun AttendanceStatusTag(
    state: AttendanceStatus,
    modifier: Modifier = Modifier,
) {
    WCTag(
        text = state.text(),
        backgroundColor = colorResource(R.color.tagView_bg),
        textColor = colorResource(R.color.tagView_text),
        fontWeight = FontWeight.Normal,
        modifier = modifier
    )
}

enum class AttendanceStatus {
    BOOKED, CHECKED_IN, NO_SHOW, CANCELLED
}

@Composable
fun AttendanceStatus.text(): String {
    return when (this) {
        AttendanceStatus.BOOKED -> R.string.booking_attendance_status_booked
        AttendanceStatus.CHECKED_IN -> R.string.booking_attendance_status_checked_in
        AttendanceStatus.CANCELLED -> R.string.booking_attendance_status_cancelled
        AttendanceStatus.NO_SHOW -> R.string.booking_attendance_status_no_show
    }.let { stringResource(it) }
}

@Preview
@Composable
private fun AttendanceStatusTagPreview() {
    WooThemeWithBackground {
        AttendanceStatusTag(
            state = AttendanceStatus.BOOKED
        )
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttendanceStatusTagDarkPreview() {
    WooThemeWithBackground {
        AttendanceStatusTag(
            state = AttendanceStatus.CHECKED_IN
        )
    }
}
