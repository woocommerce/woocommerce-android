package com.woocommerce.android.ui.woopos.home.totals.payment.processing

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState
import kotlinx.coroutines.delay

@Composable
fun WooPosPaymentProcessingScreen(
    state: WooPosTotalsViewState.PaymentProcessing,
) {
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
            ProcessingAnimation(
                modifier = Modifier.size(156.dp),
            )
            Spacer(modifier = Modifier.height(48.dp.toAdaptivePadding()))
            Text(
                text = state.title,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Medium,
                color = WooPosTheme.colors.paymentProcessingText,
            )
            Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))
            Text(
                text = state.subtitle,
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                color = WooPosTheme.colors.paymentProcessingText,
            )
        }
    }
}

@Composable
fun ProcessingAnimation(modifier: Modifier = Modifier.fillMaxSize()) {
    val svgs = listOf(
        R.drawable.woopos_reader_tap_card_1,
        R.drawable.woopos_reader_tap_card_2,
        R.drawable.woopos_reader_tap_card_3,
    )

    var currentSvgIndex: Int by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            currentSvgIndex = (currentSvgIndex + 1) % svgs.size
        }
    }

    Box(
        modifier = modifier
            .background(
                color = WooPosTheme.colors.paymentProcessingAnimationBackground,
                shape = RoundedCornerShape(size = 13.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = currentSvgIndex, label = "payment_processing") { index ->
            Image(
                painter = painterResource(id = svgs[index]),
                contentDescription = null,
            )
        }
    }

}

@WooPosPreview
@Composable
fun WooPosPaymentProcessingScreenPreview() {
    WooPosTheme {
        WooPosPaymentProcessingScreen(
            state = WooPosTotalsViewState.PaymentProcessing(
                title = "Processing payment",
                subtitle = "Please wait...",
            )
        )
    }
}
