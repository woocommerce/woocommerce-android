package com.woocommerce.android.ui.woopos.home.totals.payment.failed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsUIEvent
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState

@Composable
fun WooPosPaymentFailedScreen(
    state: WooPosTotalsViewState.PaymentFailed,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .background(color = WooPosTheme.colors.homeBackground)
            .fillMaxSize()
            .padding(vertical = 96.dp.toAdaptivePadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(96.dp.toAdaptivePadding()))
        Icon(
            modifier = Modifier.size(84.dp),
            painter = painterResource(id = R.drawable.ic_woo_pos_error_x),
            contentDescription = stringResource(id = R.string.woopos_error_icon_content_description),
            tint = Color.Unspecified,
        )
        Spacer(modifier = Modifier.height(40.dp.toAdaptivePadding()))
        Text(
            text = state.title,
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))
        Text(
            text = state.subtitle,
            style = MaterialTheme.typography.h6
        )
        Spacer(modifier = Modifier.height(40.dp.toAdaptivePadding()))
        WooPosButton(
            text = state.retryPaymentButtonLabel,
            modifier = Modifier
                .height(80.dp)
                .width(604.dp)
        ) { onUIEvent(WooPosTotalsUIEvent.RetryFailedTransactionClicked) }
        if (state.isReturnToCheckoutButtonVisible) {
            Spacer(modifier = Modifier.height(24.dp.toAdaptivePadding()))
            WooPosOutlinedButton(
                modifier = Modifier
                    .height(80.dp)
                    .width(604.dp),
                content = {
                    Text(
                        color = MaterialTheme.colors.primary,
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        text = stringResource(R.string.woo_pos_payment_failed_go_back_to_checkout)
                    )
                }
            ) { onUIEvent(WooPosTotalsUIEvent.GoBackToCheckoutAfterFailedPayment) }
        }
        Spacer(modifier = Modifier.height(80.dp.toAdaptivePadding()))
    }
}

@WooPosPreview
@Composable
fun WooPosPaymentFailedScreenPreview() {
    WooPosTheme {
        WooPosPaymentFailedScreen(
            state = WooPosTotalsViewState.PaymentFailed(
                title = "Payment failed",
                subtitle = "Unfortunately, this payment has been declined.",
                retryPaymentButtonLabel = "Try again",
                isReturnToCheckoutButtonVisible = true,
            ),
            onUIEvent = {}
        )
    }
}
