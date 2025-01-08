package com.woocommerce.android.ui.woopos.home.totals.payment.inprogress

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsUIEvent
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState
import kotlinx.coroutines.delay

@Composable
fun WooPosPaymentInProgressScreen(
    state: WooPosTotalsViewState.PaymentInProgress,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    BackHandler {
        onUIEvent(WooPosTotalsUIEvent.OnBackClicked)
    }
    var enterAnimationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        enterAnimationStarted = true
    }
    Box(
        modifier = Modifier
            .background(color = WooPosTheme.colors.paymentProcessingBackground)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.woopos_card_ilustration))
            LottieAnimation(
                modifier = Modifier.size(256.dp),
                composition = composition,
                iterations = LottieConstants.IterateForever,
                clipToCompositionBounds = false,
                clipSpec = LottieClipSpec.Markers("payment_processing_start", "payment_processing_end")
            )
            val marginBetweenAnimatedIconAndText by animateDpAsState(
                targetValue = if (enterAnimationStarted) 0.dp else 256.dp,
            )
            Spacer(modifier = Modifier.height(marginBetweenAnimatedIconAndText.toAdaptivePadding()))
            AnimatedVisibility(visible = enterAnimationStarted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.title,
                        color = WooPosTheme.colors.paymentProcessingText,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Normal,
                    )
                    Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))
                    Text(
                        text = state.subtitle,
                        color = WooPosTheme.colors.paymentProcessingText,
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))
        }
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
