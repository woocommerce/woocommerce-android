package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsScreen

@Composable
fun WooPosPhoneTotalsScreen(
    modifier: Modifier = Modifier,
) {
    WooPosTotalsScreen(
        modifier = modifier.fillMaxSize(),
        hideCashPaymentButton = true,
    )
}
