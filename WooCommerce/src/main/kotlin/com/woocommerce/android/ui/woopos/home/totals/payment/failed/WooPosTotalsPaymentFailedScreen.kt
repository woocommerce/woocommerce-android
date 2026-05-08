package com.woocommerce.android.ui.woopos.home.totals.payment.failed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.adaptiveContentWidth
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
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
    val isPhone = currentWooPosBreakpoint() == WooPosBreakpoint.Phone
    val horizontalPadding = if (isPhone) WooPosSpacing.Large.value else WooPosSpacing.XLarge.value
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = horizontalPadding,
                vertical = WooPosSpacing.Huge.value,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(WooPosSpacing.Huge.value))
        Icon(
            modifier = Modifier.size(WooPosComponentSize.Small.value),
            imageVector = WooPosIcons.ErrorX,
            contentDescription = stringResource(id = R.string.woopos_error_icon_content_description),
            tint = WooPosTheme.colors.unspecified,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        WooPosText(
            text = state.title,
            style = WooPosTypography.BodyXLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        WooPosText(
            text = state.subtitle,
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        WooPosButton(
            text = state.retryPaymentButtonLabel,
            modifier = Modifier
                .height(WooPosComponentSize.Small.value)
                .adaptiveContentWidth()
        ) { onUIEvent(WooPosTotalsUIEvent.RetryFailedTransactionClicked) }
        if (state.isReturnToCheckoutButtonVisible) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
            WooPosOutlinedButton(
                modifier = Modifier
                    .adaptiveContentWidth(),
                text = stringResource(R.string.woo_pos_payment_failed_go_back_to_checkout),
            ) { onUIEvent(WooPosTotalsUIEvent.GoBackToCheckoutAfterFailedPayment) }
        }
        Spacer(modifier = Modifier.height(WooPosSpacing.Huge.value))
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
