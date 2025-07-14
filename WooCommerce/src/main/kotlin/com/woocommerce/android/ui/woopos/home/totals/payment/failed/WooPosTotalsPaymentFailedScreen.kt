package com.woocommerce.android.ui.woopos.home.totals.payment.failed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsUIEvent
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState

@Composable
fun WooPosPaymentFailedScreen(
    state: WooPosTotalsViewState.PaymentFailed,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    BackHandler {
        onUIEvent(WooPosTotalsUIEvent.OnBackClicked)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 96.dp.toAdaptivePadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        @Suppress("WooPosDesignSystemSpacingUsageRule")
        Spacer(modifier = Modifier.height(96.dp.toAdaptivePadding()))
        Icon(
            modifier = Modifier.size(84.dp),
            painter = painterResource(id = R.drawable.ic_woo_pos_error_x),
            contentDescription = stringResource(id = R.string.woopos_error_icon_content_description),
            tint = WooPosTheme.colors.unspecified,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))
        WooPosText(
            text = state.title,
            style = WooPosTypography.BodyXLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
        WooPosText(
            text = state.subtitle,
            style = WooPosTypography.BodyLarge,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))
        WooPosButton(
            text = state.retryPaymentButtonLabel,
            modifier = Modifier
                .height(80.dp)
                .width(604.dp)
        ) { onUIEvent(WooPosTotalsUIEvent.RetryFailedTransactionClicked) }
        if (state.isReturnToCheckoutButtonVisible) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))
            WooPosOutlinedButton(
                modifier = Modifier
                    .width(604.dp),
                text = stringResource(R.string.woo_pos_payment_failed_go_back_to_checkout),
            ) { onUIEvent(WooPosTotalsUIEvent.GoBackToCheckoutAfterFailedPayment) }
        }
        @Suppress("WooPosDesignSystemSpacingUsageRule")
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
