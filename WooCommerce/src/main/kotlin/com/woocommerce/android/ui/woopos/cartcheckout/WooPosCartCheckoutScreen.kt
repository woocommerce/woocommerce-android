package com.woocommerce.android.ui.woopos.cartcheckout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.cartcheckout.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.cartcheckout.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.cartcheckout.totals.WooPosTotalsScreen
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun WooPosCartCheckoutScreen() {
    val viewModel: WooPosCartCheckoutViewModel = hiltViewModel()
    WooPosCartCheckoutScreen(
        viewModel.state.collectAsState().value,
        viewModel::onUIEvent
    )
}

@Composable
private fun WooPosCartCheckoutScreen(
    state: WooPosCartCheckoutState,
    onCartCheckoutUIEvent: (WooPosCartCheckoutUIEvent) -> Unit,
) {
    BackHandler {
        onCartCheckoutUIEvent(WooPosCartCheckoutUIEvent.SystemBackClicked)
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val cartWidth = (screenWidthDp / 3)
    val totalsProductsWidth = (screenWidthDp / 3 * 2)
    val halfScreenWidthPx = with(LocalDensity.current) { totalsProductsWidth.roundToPx() }

    val scrollState = rememberScrollState()

    LaunchedEffect(state) {
        val animationSpec = spring<Float>(
            dampingRatio = 0.8f,
            stiffness = 200f
        )
        when (state) {
            WooPosCartCheckoutState.Cart -> scrollState.animateScrollTo(
                0,
                animationSpec = animationSpec
            )

            WooPosCartCheckoutState.Checkout -> scrollState.animateScrollTo(
                halfScreenWidthPx,
                animationSpec = animationSpec
            )
        }
    }

    Row(
        modifier = Modifier
            .horizontalScroll(scrollState, enabled = false)
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier.width(totalsProductsWidth)) {
            WooPosProductsScreen()
        }
        Box(modifier = Modifier.width(cartWidth)) {
            WooPosCartScreen()
        }
        Box(modifier = Modifier.width(totalsProductsWidth)) {
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
