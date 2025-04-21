package com.woocommerce.android.ui.woopos.home.items.coupons

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun WooPosCouponsScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
) {
    Text(
        text = "Coupons List",
        modifier = modifier,
    )
}
