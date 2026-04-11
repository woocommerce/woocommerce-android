package com.woocommerce.android.ui.bookings.compose

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.details.AttendanceUpdateStatus
import com.woocommerce.android.ui.bookings.details.CancelStatus
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingAppointmentDetails(
    model: BookingAppointmentDetailsModel,
    onCancelBooking: () -> Unit,
    onAttendanceToggle: () -> Unit,
    onRescheduleBooking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BookingSectionHeader(R.string.booking_details_section_header)
        Column(modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)) {
            HorizontalDivider(thickness = 0.5.dp)
            AppointmentDetailsRow(
                label = R.string.booking_appointment_label_date,
                value = model.date
            )
            AppointmentDetailsRow(
                label = R.string.booking_appointment_label_time,
                value = model.time
            )
            model.staff?.let {
                AppointmentDetailsRow(
                    label = R.string.booking_appointment_label_team_member
                ) {
                    when (it) {
                        is BookingStaffMemberStatus.Loaded, is BookingStaffMemberStatus.Unavailable -> {
                            Text(
                                text = (it as? BookingStaffMemberStatus.Loaded)?.name ?: "-",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        BookingStaffMemberStatus.Loading -> {
                            SkeletonView(
                                width = 80.dp,
                                height = with(LocalDensity.current) {
                                    MaterialTheme.typography.bodyMedium.fontSize.toDp()
                                },
                            )
                        }
                    }
                }
            }
            AppointmentDetailsRow(
                label = R.string.booking_appointment_label_location
            ) {
                when (model.location) {
                    is BookingLocationStatus.Loaded, is BookingLocationStatus.Unavailable -> {
                        Text(
                            text = (model.location as? BookingLocationStatus.Loaded)?.location ?: "-",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BookingLocationStatus.Loading -> {
                        SkeletonView(
                            width = 80.dp,
                            height = with(LocalDensity.current) {
                                MaterialTheme.typography.bodyMedium.fontSize.toDp()
                            },
                        )
                    }
                }
            }
            AppointmentDetailsRow(
                label = R.string.booking_appointment_label_duration,
                value = model.duration,
                withDivider = model.anyButtonVisible,
            )
            AnimatedVisibility(model.anyButtonVisible) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    AnimatedVisibility(model.rescheduleButtonVisible) {
                        WCOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = onRescheduleBooking,
                            text = stringResource(R.string.booking_details_reschedule_button),
                        )
                    }
                    AnimatedVisibility(model.attendanceButtonVisible) {
                        val text = when (model.attendanceStatus) {
                            BookingAttendanceStatus.Attended ->
                                stringResource(R.string.booking_mark_as_unattended)
                            else -> stringResource(R.string.booking_mark_as_attended)
                        }
                        WCOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = onAttendanceToggle,
                            enabled = model.attendanceButtonEnabled,
                            text = text,
                            loading = model.attendanceInProgressShown,
                        )
                    }
                    AnimatedVisibility(model.cancelButtonVisible) {
                        WCOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = onCancelBooking,
                            enabled = model.cancelButtonEnabled,
                            text = stringResource(R.string.booking_details_cancel_booking_button),
                            loading = model.cancelInProgressShown,
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp)
        }
    }
}

@Composable
private fun AppointmentDetailsRow(
    @StringRes label: Int,
    withDivider: Boolean = true,
    value: @Composable () -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BookingLabel(label)
            Box(Modifier.padding(start = 8.dp)) {
                value()
            }
        }
        if (withDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun AppointmentDetailsRow(
    @StringRes label: Int,
    withDivider: Boolean = true,
    value: String,
) {
    AppointmentDetailsRow(label = label, withDivider = withDivider) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

data class BookingAppointmentDetailsModel(
    val date: String,
    val time: String,
    val staff: BookingStaffMemberStatus?,
    val location: BookingLocationStatus,
    val duration: String,
    val cancelButtonVisible: Boolean,
    val cancelStatus: CancelStatus,
    val rescheduleButtonVisible: Boolean = false,
    val attendanceStatus: BookingAttendanceStatus? = null,
    val isAttendanceStatusEditable: Boolean = false,
    val attendanceUpdateStatus: AttendanceUpdateStatus = AttendanceUpdateStatus.Idle,
) {
    val cancelButtonEnabled: Boolean = cancelButtonVisible && cancelStatus != CancelStatus.InProgress
    val cancelInProgressShown: Boolean = cancelButtonVisible && cancelStatus == CancelStatus.InProgress
    val attendanceButtonVisible: Boolean = isAttendanceStatusEditable
    val attendanceButtonEnabled: Boolean = attendanceUpdateStatus != AttendanceUpdateStatus.InProgress
    val attendanceInProgressShown: Boolean = attendanceUpdateStatus == AttendanceUpdateStatus.InProgress
    val anyButtonVisible: Boolean = rescheduleButtonVisible || attendanceButtonVisible || cancelButtonVisible
}

sealed interface BookingLocationStatus {
    data object Loading : BookingLocationStatus
    data class Loaded(val location: String) : BookingLocationStatus
    data object Unavailable : BookingLocationStatus
}

sealed interface BookingStaffMemberStatus {
    data object Loading : BookingStaffMemberStatus
    data class Loaded(val name: String) : BookingStaffMemberStatus
    data object Unavailable : BookingStaffMemberStatus
}

@LightDarkThemePreviews
@Composable
private fun BookingAppointmentDetailsPreview() {
    WooThemeWithBackground {
        BookingAppointmentDetails(
            model = BookingAppointmentDetailsModel(
                date = "05/07/2025, 11:00 AM",
                time = "11:00 am - 12:00 pm",
                staff = BookingStaffMemberStatus.Loading,
                location = BookingLocationStatus.Loaded("238 Willow Creek Drive, Montgomery AL 36109"),
                duration = "60 min",
                cancelButtonVisible = true,
                cancelStatus = CancelStatus.Idle,
                rescheduleButtonVisible = true,
                attendanceStatus = BookingAttendanceStatus.Unattended,
                isAttendanceStatusEditable = true,
            ),
            onCancelBooking = {},
            onAttendanceToggle = {},
            onRescheduleBooking = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingAppointmentDetailsCancelHiddenPreview() {
    WooThemeWithBackground {
        BookingAppointmentDetails(
            model = BookingAppointmentDetailsModel(
                date = "05/07/2025, 11:00 AM",
                time = "11:00 am - 12:00 pm",
                staff = BookingStaffMemberStatus.Loading,
                location = BookingLocationStatus.Loaded("238 Willow Creek Drive, Montgomery AL 36109"),
                duration = "60 min",
                cancelButtonVisible = false,
                cancelStatus = CancelStatus.Idle,
            ),
            onCancelBooking = {},
            onAttendanceToggle = {},
            onRescheduleBooking = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
