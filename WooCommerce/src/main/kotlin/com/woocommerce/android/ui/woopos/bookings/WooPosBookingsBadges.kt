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
fun WooPosPaymentStatusBadge(paymentStatus: PaymentStatus) {
    val text = when (paymentStatus) {
        PaymentStatus.PAID -> stringResource(R.string.woopos_bookings_payment_status_paid)
        PaymentStatus.UNPAID -> stringResource(R.string.woopos_bookings_payment_status_unpaid)
        PaymentStatus.FAILED -> stringResource(R.string.woopos_bookings_payment_status_failed)
        PaymentStatus.REFUNDED -> stringResource(R.string.woopos_bookings_payment_status_refunded)
        PaymentStatus.PARTIALLY_REFUNDED -> stringResource(R.string.woopos_bookings_payment_status_partially_refunded)
        PaymentStatus.AUTHORIZED -> stringResource(R.string.woopos_bookings_payment_status_authorized)
        PaymentStatus.AUTHORIZATION_VOIDED -> stringResource(
            R.string.woopos_bookings_payment_status_authorization_voided
        )
    }

    val bgColor = when (paymentStatus) {
        PaymentStatus.UNPAID,
        PaymentStatus.FAILED,
        PaymentStatus.AUTHORIZED -> WooPosTheme.colors.errorLowest
        PaymentStatus.PAID,
        PaymentStatus.REFUNDED,
        PaymentStatus.PARTIALLY_REFUNDED,
        PaymentStatus.AUTHORIZATION_VOIDED -> WooPosTheme.colors.disabledContainer
    }

    val textColor = when (paymentStatus) {
        PaymentStatus.UNPAID,
        PaymentStatus.FAILED,
        PaymentStatus.AUTHORIZED -> WooPosTheme.colors.onErrorLowest
        PaymentStatus.PAID,
        PaymentStatus.REFUNDED,
        PaymentStatus.PARTIALLY_REFUNDED,
        PaymentStatus.AUTHORIZATION_VOIDED -> WooPosTheme.colors.onDefault
    }

    WooPosText(
        text = text,
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
fun WooPosCancelledBadge() {
    WooPosText(
        text = stringResource(R.string.woopos_bookings_status_cancelled),
        style = WooPosTypography.Caption,
        color = WooPosTheme.colors.onInfoLowest,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            .background(WooPosTheme.colors.infoLowest)
            .padding(
                horizontal = WooPosSpacing.Small.value,
                vertical = WooPosSpacing.XSmall.value
            )
    )
}

@Composable
fun WooPosGuestBadge() {
    WooPosText(
        text = stringResource(R.string.woopos_bookings_details_customer_guest_badge),
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
