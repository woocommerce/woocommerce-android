package com.woocommerce.android.ui.bookings.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.details.AttendanceUpdateStatus
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingAttendanceSection(
    status: BookingAttendanceStatus?,
    attendanceUpdateStatus: AttendanceUpdateStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BookingSectionHeader(R.string.booking_attendance_header)
        Column(
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            HorizontalDivider(thickness = 0.5.dp)
            AttendanceRow(
                label = R.string.booking_attendance_label_status,
                attendanceUpdateStatus = attendanceUpdateStatus,
                value = status.text(),
                modifier = Modifier.clickable {
                    if (attendanceUpdateStatus == AttendanceUpdateStatus.Idle) onClick()
                }
            )
            HorizontalDivider(thickness = 0.5.dp)
        }
        Text(
            text = stringResource(R.string.booking_attendance_change_status_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AttendanceRow(
    @StringRes label: Int,
    attendanceUpdateStatus: AttendanceUpdateStatus,
    value: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var skeletonSize by remember { mutableStateOf(DpSize.Zero) }
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BookingDetailsLabel(label)
            when (attendanceUpdateStatus) {
                AttendanceUpdateStatus.InProgress -> {
                    SkeletonView(
                        modifier = Modifier
                            .size(skeletonSize)
                    )
                }

                AttendanceUpdateStatus.Idle -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .onSizeChanged {
                                with(density) {
                                    skeletonSize = DpSize(it.width.toDp(), it.height.toDp())
                                }
                            }
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingAttendanceSectionPreview() {
    WooThemeWithBackground {
        BookingAttendanceSection(
            status = BookingAttendanceStatus.Booked,
            attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingAttendanceSectionInProgressPreview() {
    WooThemeWithBackground {
        BookingAttendanceSection(
            status = BookingAttendanceStatus.Booked,
            attendanceUpdateStatus = AttendanceUpdateStatus.InProgress,
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
