package com.woocommerce.android.ui.woopos.home.totals.payment.inprogress

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsUIEvent
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState

@Composable
fun WooPosPaymentInProgressScreen(
    state: WooPosTotalsViewState.PaymentInProgress,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    BackHandler {
        onUIEvent(WooPosTotalsUIEvent.OnBackClicked)
    }
    Box(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.primary)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.woopos_card_ilustration))
            LottieAnimation(
                modifier = Modifier.size(256.dp),
                composition = composition,
                iterations = LottieConstants.IterateForever,
                clipToCompositionBounds = false,
                clipSpec = LottieClipSpec.Markers("payment_processing_start", "payment_processing_end")
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WooPosText(
                    text = state.title,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = WooPosTypography.BodyLarge,
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
                WooPosText(
                    text = state.subtitle,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = WooPosTypography.BodyXLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
    }
}

@WooPosPreview
@Composable
fun WooPosPaymentInProgressScreenPreview() {
    WooPosTheme {
        WooPosPaymentInProgressScreen(
            state = WooPosTotalsViewState.PaymentInProgress(
                title = "Processing payment",
                subtitle = "Please wait...",
            )
        ) {}
    }
}
