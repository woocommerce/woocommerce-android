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
fun BookingPaymentStatusTag(
    state: BookingPaymentStatus,
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

enum class BookingPaymentStatus(val key: String) {
    UNPAID("unpaid"),
    PENDING_CONFIRMATION("pending-confirmation"),
    CONFIRMED("confirmed"),
    PAID("paid"),
    CANCELLED("cancelled"),
    COMPLETE("complete");

    companion object {
        fun fromKey(key: String): BookingPaymentStatus {
            return entries.firstOrNull { it.key == key } ?: UNPAID
        }
    }
}

@Composable
private fun BookingPaymentStatus.text(): String {
    return when (this) {
        BookingPaymentStatus.UNPAID -> R.string.booking_payment_status_unpaid
        BookingPaymentStatus.PENDING_CONFIRMATION -> R.string.booking_payment_status_pending_confirmation
        BookingPaymentStatus.CONFIRMED -> R.string.booking_payment_status_confirmed
        BookingPaymentStatus.PAID -> R.string.booking_payment_status_paid
        BookingPaymentStatus.CANCELLED -> R.string.booking_payment_status_cancelled
        BookingPaymentStatus.COMPLETE -> R.string.booking_payment_status_complete
    }.let { stringResource(it) }
}

@Preview
@Composable
private fun PaymentStatusTagPreview() {
    WooThemeWithBackground {
        BookingPaymentStatusTag(
            state = BookingPaymentStatus.PAID
        )
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PaymentStatusTagDarkPreview() {
    WooThemeWithBackground {
        BookingPaymentStatusTag(
            state = BookingPaymentStatus.COMPLETE
        )
    }
}
