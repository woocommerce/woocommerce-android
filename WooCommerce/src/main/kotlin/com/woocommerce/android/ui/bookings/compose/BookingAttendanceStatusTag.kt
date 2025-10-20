package com.woocommerce.android.ui.bookings.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.details.AttendanceUpdateStatus
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCTag
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingAttendanceStatusTag(
    state: BookingAttendanceStatus,
    attendanceUpdateStatus: AttendanceUpdateStatus,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var skeletonSize by remember { mutableStateOf(DpSize.Zero) }
    Box(modifier = modifier) {
        when (attendanceUpdateStatus) {
            AttendanceUpdateStatus.InProgress -> {
                SkeletonView(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .sizeIn(
                            minHeight = 20.dp,
                            maxWidth = 80.dp
                        )
                        .size(skeletonSize)
                )
            }

            AttendanceUpdateStatus.Idle -> {
                WCTag(
                    text = state.text(),
                    backgroundColor = state.backgroundColor(),
                    textColor = colorResource(R.color.tagView_text),
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .onSizeChanged {
                            with(density) {
                                skeletonSize = DpSize(it.width.toDp(), it.height.toDp())
                            }
                        }
                )
            }
        }
    }
}

sealed interface BookingAttendanceStatus {
    data object Booked : BookingAttendanceStatus
    data object CheckedIn : BookingAttendanceStatus
    data object NoShow : BookingAttendanceStatus
    data object Cancelled : BookingAttendanceStatus
}

@Composable
fun BookingAttendanceStatus?.text(): String {
    return when (this) {
        BookingAttendanceStatus.Booked -> stringResource(R.string.booking_attendance_status_booked)
        BookingAttendanceStatus.CheckedIn -> stringResource(R.string.booking_attendance_status_checked_in)
        BookingAttendanceStatus.Cancelled -> stringResource(R.string.booking_attendance_status_cancelled)
        BookingAttendanceStatus.NoShow -> stringResource(R.string.booking_attendance_status_no_show)
        else -> ""
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
            state = BookingAttendanceStatus.Booked,
            attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
        )
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttendanceStatusTagDarkPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.CheckedIn,
            attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun AttendanceStatusTagNoShowPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.NoShow,
            attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun AttendanceStatusTagInProgressPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.NoShow,
            attendanceUpdateStatus = AttendanceUpdateStatus.InProgress,
            modifier = Modifier.padding(10.dp)
        )
    }
}
