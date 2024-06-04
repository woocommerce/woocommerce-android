package com.woocommerce.android.ui.woopos.cartcheckout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.cartcheckout.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.cartcheckout.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.cartcheckout.totals.WooPosTotalsScreen
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun WooPosCartCheckoutScreen() {
    val viewModel: WooPosCartCheckoutViewModel = hiltViewModel()
    viewModel.state.observeAsState().value?.let { state ->
        WooPosCartCheckoutScreen(state, viewModel::onUIEvent)
    }

}

@Composable
private fun WooPosCartCheckoutScreen(
    state: WooPosCartCheckoutState,
    onCartCheckoutUIEvent: (WooPosCartCheckoutUIEvent) -> Unit
) {
    when (state) {
        WooPosCartCheckoutState.Cart -> {
            Row(Modifier.fillMaxSize()) {
                WooPosCartScreen(onCartCheckoutUIEvent)
                WooPosProductsScreen()
            }
        }

        WooPosCartCheckoutState.Checkout -> {
            Row(Modifier.fillMaxSize()) {
                WooPosCartScreen(onCartCheckoutUIEvent)
                WooPosTotalsScreen()
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartCheckoutCartScreenPreview() {
    WooPosCartCheckoutScreen(
        state = WooPosCartCheckoutState.Cart,
        onCartCheckoutUIEvent = {}
    )
}

@Composable
@WooPosPreview
fun WooPosCartCheckoutCheckoutScreenPreview() {
    WooPosCartCheckoutScreen(
        state = WooPosCartCheckoutState.Checkout,
        onCartCheckoutUIEvent = {}
    )
}
