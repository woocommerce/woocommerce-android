package com.woocommerce.android.ui.woopos.cartcheckout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
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
    onCartCheckoutUIEvent: (WooPosCartCheckoutUIEvent) -> Unit,
) {
    val halfScreen = (LocalConfiguration.current.screenWidthDp / 2).dp
//    val halfScreenWidthPx = with(LocalDensity.current) { getHalfScreenWidth().toPx() }

    val scrollState = rememberScrollState()

    LaunchedEffect(state) {
        when (state) {
            WooPosCartCheckoutState.Cart -> scrollState.animateScrollTo(0)
            WooPosCartCheckoutState.Checkout -> scrollState.animateScrollTo(10000)
        }
    }

    Row(
        modifier = Modifier
            .scrollable(
                scrollState,
                orientation = Orientation.Horizontal,
                enabled = true
            )
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier.width(halfScreen)) {
            WooPosProductsScreen()
        }
        Box(modifier = Modifier.width(halfScreen)) {
            WooPosCartScreen(onCartCheckoutUIEvent = onCartCheckoutUIEvent)
        }
        Box(modifier = Modifier.width(halfScreen)) {
            WooPosTotalsScreen()
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartCheckoutCartScreenPreview() {
    WooPosCartCheckoutScreen(state = WooPosCartCheckoutState.Cart, onCartCheckoutUIEvent = {})
}

@Composable
@WooPosPreview
fun WooPosCartCheckoutCheckoutScreenPreview() {
    WooPosCartCheckoutScreen(state = WooPosCartCheckoutState.Checkout, onCartCheckoutUIEvent = {})
}
