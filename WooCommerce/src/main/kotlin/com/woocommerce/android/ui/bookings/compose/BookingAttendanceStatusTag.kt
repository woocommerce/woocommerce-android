package com.woocommerce.android.ui.bookings.compose

import androidx.compose.foundation.BorderStroke
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
                    textColor = state.textColor(),
                    border = state.border(),
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
    data object Attended : BookingAttendanceStatus
    data object Unattended : BookingAttendanceStatus
}

@Composable
private fun BookingAttendanceStatus?.text(): String {
    return when (this) {
        BookingAttendanceStatus.Attended -> stringResource(R.string.booking_attendance_status_attended)
        BookingAttendanceStatus.Unattended -> stringResource(R.string.booking_attendance_status_unattended)
        else -> ""
    }
}

@Composable
private fun BookingAttendanceStatus.backgroundColor(): Color = if (isOutlined()) {
    Color.Transparent
} else {
    colorResource(R.color.tagView_bg)
}

@Composable
private fun BookingAttendanceStatus.textColor(): Color = if (isOutlined()) {
    colorResource(R.color.color_on_surface_high)
} else {
    colorResource(R.color.tagView_text)
}

@Composable
private fun BookingAttendanceStatus.border(): BorderStroke? = if (isOutlined()) {
    BorderStroke(1.dp, colorResource(R.color.tag_border_booking_outlined))
} else {
    null
}

private fun BookingAttendanceStatus.isOutlined(): Boolean = this == BookingAttendanceStatus.Attended

@Preview
@Composable
private fun AttendanceStatusTagAttendedPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.Attended,
            attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
        )
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttendanceStatusTagUnattendedDarkPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.Unattended,
            attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun AttendanceStatusTagCancelledPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusTag(
            state = BookingAttendanceStatus.Attended,
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
            state = BookingAttendanceStatus.Attended,
            attendanceUpdateStatus = AttendanceUpdateStatus.InProgress,
            modifier = Modifier.padding(10.dp)
        )
    }
}
