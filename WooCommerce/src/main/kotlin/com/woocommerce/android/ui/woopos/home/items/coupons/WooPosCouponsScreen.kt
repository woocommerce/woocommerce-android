package com.woocommerce.android.ui.woopos.home.items.coupons

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme

@Composable
fun WooPosCouponsScreen(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Coupons List",
        color = WooPosTheme.colors.onSurfaceVariantHighest,
        modifier = modifier
    )
}

@WooPosPreview
@Composable
fun WooPosCouponsScreenPreview(modifier: Modifier = Modifier) {
    WooPosCouponsScreen(
        modifier = modifier,
    )
}
