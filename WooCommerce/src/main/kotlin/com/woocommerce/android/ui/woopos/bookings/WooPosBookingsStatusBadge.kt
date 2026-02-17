package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosBookingsStatusBadge(status: WooPosBookingStatus) {
    val bgColor = when (status.colorKey) {
        WooPosBookingStatusColorKey.COMPLETED -> WooPosTheme.colors.infoLowest
        WooPosBookingStatusColorKey.FAILED -> WooPosTheme.colors.errorLowest
        WooPosBookingStatusColorKey.PROCESSING,
        WooPosBookingStatusColorKey.ON_HOLD -> WooPosTheme.colors.default
    }

    val textColor = when (status.colorKey) {
        WooPosBookingStatusColorKey.COMPLETED -> WooPosTheme.colors.onInfoLowest
        WooPosBookingStatusColorKey.FAILED -> WooPosTheme.colors.onErrorLowest
        WooPosBookingStatusColorKey.PROCESSING,
        WooPosBookingStatusColorKey.ON_HOLD -> WooPosTheme.colors.onDefault
    }

    WooPosText(
        text = status.text,
        style = WooPosTypography.Caption,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            .background(bgColor)
            .padding(
                horizontal = WooPosSpacing.Small.value,
                vertical = WooPosSpacing.XSmall.value
            )
    )
}

@Composable
fun WooPosAttendanceBadge(attendanceState: WooPosBookingsState.AttendanceState) {
    val text = when (attendanceState) {
        WooPosBookingsState.AttendanceState.ATTENDED ->
            stringResource(R.string.woopos_bookings_details_attendance_attended)
        WooPosBookingsState.AttendanceState.UNATTENDED ->
            stringResource(R.string.woopos_bookings_details_attendance_unattended)
    }

    WooPosText(
        text = text,
        style = WooPosTypography.Caption,
        color = WooPosTheme.colors.onDefault,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            .background(WooPosTheme.colors.disabledContainer)
            .padding(
                horizontal = WooPosSpacing.Small.value,
                vertical = WooPosSpacing.XSmall.value
            )
    )
}
