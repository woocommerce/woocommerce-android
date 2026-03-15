package com.woocommerce.android.ui.bookings.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.PaymentStatus
import com.woocommerce.android.ui.compose.component.WCTag
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingPaymentStatusTag(
    paymentStatus: PaymentStatus,
    modifier: Modifier = Modifier,
) {
    WCTag(
        text = paymentStatus.text(),
        backgroundColor = paymentStatus.backgroundColor(),
        textColor = paymentStatus.textColor(),
        border = paymentStatus.border(),
        fontWeight = FontWeight.Normal,
        modifier = modifier
    )
}

@Composable
fun BookingCancelledTag(
    modifier: Modifier = Modifier,
) {
    WCTag(
        text = stringResource(R.string.booking_attendance_status_cancelled),
        backgroundColor = colorResource(R.color.tag_bg_booking_cancelled),
        textColor = colorResource(R.color.tag_text_booking_cancelled),
        fontWeight = FontWeight.Normal,
        modifier = modifier
    )
}

@Composable
private fun PaymentStatus.text(): String = when (this) {
    PaymentStatus.PAID -> stringResource(R.string.booking_payment_status_paid)
    PaymentStatus.UNPAID -> stringResource(R.string.booking_payment_status_unpaid)
    PaymentStatus.FAILED -> stringResource(R.string.booking_payment_status_failed)
    PaymentStatus.REFUNDED -> stringResource(R.string.booking_payment_status_refunded)
    PaymentStatus.PARTIALLY_REFUNDED -> stringResource(R.string.booking_payment_status_partially_refunded)
    PaymentStatus.AUTHORIZED -> stringResource(R.string.booking_payment_status_authorized)
    PaymentStatus.AUTHORIZATION_VOIDED -> stringResource(R.string.booking_payment_status_authorization_voided)
}

@Composable
private fun PaymentStatus.backgroundColor(): Color = when (this) {
    PaymentStatus.UNPAID,
    PaymentStatus.AUTHORIZED -> colorResource(R.color.tag_bg_booking_yellow)

    PaymentStatus.PAID,
    PaymentStatus.REFUNDED,
    PaymentStatus.PARTIALLY_REFUNDED,
    PaymentStatus.AUTHORIZATION_VOIDED -> Color.Transparent

    PaymentStatus.FAILED -> colorResource(R.color.tagView_bg)
}

@Composable
private fun PaymentStatus.textColor(): Color = when (this) {
    PaymentStatus.PAID,
    PaymentStatus.REFUNDED,
    PaymentStatus.PARTIALLY_REFUNDED,
    PaymentStatus.AUTHORIZATION_VOIDED -> colorResource(R.color.color_on_surface_high)

    PaymentStatus.UNPAID,
    PaymentStatus.AUTHORIZED,
    PaymentStatus.FAILED -> colorResource(R.color.tagView_text)
}

@Composable
private fun PaymentStatus.border(): BorderStroke? = when (this) {
    PaymentStatus.PAID,
    PaymentStatus.REFUNDED,
    PaymentStatus.PARTIALLY_REFUNDED,
    PaymentStatus.AUTHORIZATION_VOIDED -> BorderStroke(1.dp, colorResource(R.color.tag_border_booking_outlined))

    PaymentStatus.UNPAID,
    PaymentStatus.AUTHORIZED,
    PaymentStatus.FAILED -> null
}

@LightDarkThemePreviews
@Composable
private fun PaymentStatusTagPaidPreview() {
    WooThemeWithBackground {
        BookingPaymentStatusTag(
            paymentStatus = PaymentStatus.PAID
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun PaymentStatusTagRefundedPreview() {
    WooThemeWithBackground {
        BookingPaymentStatusTag(
            paymentStatus = PaymentStatus.REFUNDED
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun PaymentStatusTagUnpaidPreview() {
    WooThemeWithBackground {
        BookingPaymentStatusTag(
            paymentStatus = PaymentStatus.UNPAID
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun CancelledTagPreview() {
    WooThemeWithBackground {
        BookingCancelledTag()
    }
}
