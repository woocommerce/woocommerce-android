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
fun BookingStatusTag(
    state: BookingStatus,
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

sealed interface BookingStatus {
    data object PayAtLocation : BookingStatus
    data object Unpaid : BookingStatus
    data object PendingConfirmation : BookingStatus
    data object Confirmed : BookingStatus
    data object Paid : BookingStatus
    data object Cancelled : BookingStatus
    data object Complete : BookingStatus
    data class Unknown(val key: String) : BookingStatus
}

@Composable
private fun BookingStatus.text(): String {
    return when (this) {
        BookingStatus.Unpaid -> stringResource(R.string.booking_payment_status_unpaid)
        BookingStatus.PendingConfirmation -> stringResource(R.string.booking_payment_status_pending_confirmation)
        BookingStatus.Confirmed -> stringResource(R.string.booking_payment_status_confirmed)
        BookingStatus.Paid -> stringResource(R.string.booking_payment_status_paid)
        BookingStatus.Cancelled -> stringResource(R.string.booking_payment_status_cancelled)
        BookingStatus.Complete -> stringResource(R.string.booking_payment_status_complete)
        BookingStatus.PayAtLocation -> stringResource(R.string.booking_payment_status_pay_at_location)
        is BookingStatus.Unknown -> key
    }
}

@Preview
@Composable
private fun PaymentStatusTagPreview() {
    WooThemeWithBackground {
        BookingStatusTag(
            state = BookingStatus.Paid
        )
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PaymentStatusTagDarkPreview() {
    WooThemeWithBackground {
        BookingStatusTag(
            state = BookingStatus.Complete
        )
    }
}
