package com.woocommerce.android.ui.bookings.compose

import androidx.annotation.StringRes
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
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingAppointmentDetails(
    model: BookingAppointmentDetailsModel,
    onCancelBooking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BookingSectionHeader(R.string.booking_appointment_details_header)
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
                    label = R.string.booking_appointment_label_staff
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
                label = R.string.booking_appointment_label_location,
                value = model.location
            )
            AppointmentDetailsRow(
                label = R.string.booking_appointment_label_duration,
                value = model.duration
            )
            AppointmentDetailsRow(
                label = R.string.booking_appointment_label_price,
                value = model.price
            )
            WCOutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = onCancelBooking
            ) {
                Text(text = stringResource(R.string.booking_details_cancel_booking_button))
            }
            HorizontalDivider(thickness = 0.5.dp)
        }
    }
}

@Composable
private fun AppointmentDetailsRow(
    @StringRes label: Int,
    value: @Composable () -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BookingDetailsLabel(label)
            Box(Modifier.padding(start = 8.dp)) {
                value()
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun AppointmentDetailsRow(@StringRes label: Int, value: String) {
    AppointmentDetailsRow(label = label) {
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
    val location: String,
    val duration: String,
    val price: String
)

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
                location = "238 Willow Creek Drive, Montgomery AL 36109",
                duration = "60 min",
                price = "$55.00"
            ),
            onCancelBooking = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
