package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreen

@Composable
fun WooPosPhoneCartScreen(
    modifier: Modifier = Modifier,
) {
    WooPosCartScreen(
        modifier = modifier.fillMaxSize()
    )
}
