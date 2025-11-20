package com.woocommerce.android.ui.bookings.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTag
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingStatusTag(
    state: PaymentStatus,
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

sealed interface PaymentStatus {
    data object Unpaid : PaymentStatus
    data object Paid : PaymentStatus
    data object PartiallyRefunded : PaymentStatus
    data object Refunded : PaymentStatus
    data object Failed : PaymentStatus
    data class Unknown(val key: String) : PaymentStatus
}

@Composable
private fun PaymentStatus.text(): String {
    return when (this) {
        PaymentStatus.Unpaid -> stringResource(R.string.booking_payment_status_unpaid)
        PaymentStatus.Paid -> stringResource(R.string.booking_payment_status_paid)
        PaymentStatus.Failed -> stringResource(R.string.booking_payment_status_failed)
        PaymentStatus.PartiallyRefunded -> stringResource(R.string.booking_payment_status_partially_refunded)
        PaymentStatus.Refunded -> stringResource(R.string.booking_payment_status_refunded)
        is PaymentStatus.Unknown -> key
    }
}

@Composable
fun PaymentStatus.backgroundColor(): Color {
    return when (this) {
        PaymentStatus.Unpaid -> R.color.tag_bg_booking_yellow
        else -> R.color.tagView_bg
    }.let { colorResource(it) }
}

@LightDarkThemePreviews
@Composable
private fun PaymentStatusTagPreview() {
    WooThemeWithBackground {
        BookingStatusTag(
            state = PaymentStatus.Paid
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun PaymentStatusTagPayOnSitePreview() {
    WooThemeWithBackground {
        BookingStatusTag(
            state = PaymentStatus.Unpaid
        )
    }
}
