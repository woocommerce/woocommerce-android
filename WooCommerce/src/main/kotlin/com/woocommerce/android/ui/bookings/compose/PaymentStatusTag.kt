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

sealed interface BookingPaymentStatus {
    data object Unpaid : BookingPaymentStatus
    data object PendingConfirmation : BookingPaymentStatus
    data object Confirmed : BookingPaymentStatus
    data object Paid : BookingPaymentStatus
    data object Cancelled : BookingPaymentStatus
    data object Complete : BookingPaymentStatus
    data class Unknown(val key: String) : BookingPaymentStatus
}

@Composable
private fun BookingPaymentStatus.text(): String {
    return when (this) {
        BookingPaymentStatus.Unpaid -> stringResource(R.string.booking_payment_status_unpaid)
        BookingPaymentStatus.PendingConfirmation -> stringResource(R.string.booking_payment_status_pending_confirmation)
        BookingPaymentStatus.Confirmed -> stringResource(R.string.booking_payment_status_confirmed)
        BookingPaymentStatus.Paid -> stringResource(R.string.booking_payment_status_paid)
        BookingPaymentStatus.Cancelled -> stringResource(R.string.booking_payment_status_cancelled)
        BookingPaymentStatus.Complete -> stringResource(R.string.booking_payment_status_complete)
        is BookingPaymentStatus.Unknown -> key
    }
}

@Preview
@Composable
private fun PaymentStatusTagPreview() {
    WooThemeWithBackground {
        BookingPaymentStatusTag(
            state = BookingPaymentStatus.Paid
        )
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PaymentStatusTagDarkPreview() {
    WooThemeWithBackground {
        BookingPaymentStatusTag(
            state = BookingPaymentStatus.Complete
        )
    }
}
