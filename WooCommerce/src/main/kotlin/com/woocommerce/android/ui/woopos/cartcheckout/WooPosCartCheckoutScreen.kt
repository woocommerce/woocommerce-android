package com.woocommerce.android.ui.woopos.cartcheckout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.cartcheckout.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun WooPosCartCheckoutScreen(viewModel: WooPosCartCheckoutViewModel) {
    WooPosCartCheckoutScreen(viewModel::onUIEvent)
}

@Composable
private fun WooPosCartCheckoutScreen(onUIEvent: (WooPosCartCheckoutUIEvent) -> Unit) {
    Row(Modifier.fillMaxSize()) {
        WooPosCartScreen(viewModel, onUIEvent)
        WooPosTotalsScreen(viewModel, onUIEvent)
        WooPosCheckoutScreen(viewModel, onUIEvent)

    }
}

@Composable
@WooPosPreview
fun WooPosCartCheckoutScreenPreview() {
    WooPosCartCheckoutScreen({})
}
