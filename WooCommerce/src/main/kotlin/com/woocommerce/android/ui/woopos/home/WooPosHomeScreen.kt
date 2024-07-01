package com.woocommerce.android.ui.woopos.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsScreen
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosHomeScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    val viewModel: WooPosHomeViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState().value

    WooPosHomeScreen(
        state,
        onNavigationEvent,
        viewModel::onUIEvent,
    )
}

@Composable
private fun WooPosHomeScreen(
    state: WooPosHomeState,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Boolean,
) {
    BackHandler {
        val result = onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
        if (!result) {
            onNavigationEvent(WooPosNavigationEvent.BackFromHomeClicked)
        }
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val cartWidth = (screenWidthDp / 3)
    val totalsProductsWidth = (screenWidthDp / 3 * 2)
    val halfScreenWidthPx = with(LocalDensity.current) { totalsProductsWidth.roundToPx() }

    val scrollState = rememberScrollState()

    println("WooPosHomeState state - $state")

    LaunchedEffect(state) {
        val animationSpec = spring<Float>(
            dampingRatio = 0.8f,
            stiffness = 200f
        )
        when (state) {
            is WooPosHomeState.Cart -> {
                scrollState.animateScrollTo(
                    0,
                    animationSpec = animationSpec
                )
            }

            WooPosHomeState.Checkout -> scrollState.animateScrollTo(
                halfScreenWidthPx,
                animationSpec = animationSpec
            )
        }
    }

    when (state) {
        is WooPosHomeState.Cart -> {
            when (state) {
                WooPosHomeState.Cart.Empty -> {
                    WooPosHomeScreen(
                        scrollState,
                        totalsProductsWidth + cartWidth.times(.77f),
                        cartWidth,
                    )
                }

                WooPosHomeState.Cart.NotEmpty -> {
                    WooPosHomeScreen(
                        scrollState,
                        totalsProductsWidth,
                        cartWidth,
                    )
                }
            }
        }

        WooPosHomeState.Checkout ->
            WooPosHomeScreen(
                scrollState,
                totalsProductsWidth,
                cartWidth,
            )
    }
}

@Composable
private fun WooPosHomeScreen(
    scrollState: ScrollState,
    totalsProductsWidth: Dp,
    cartWidth: Dp,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(scrollState, enabled = false)
            .fillMaxWidth()
    ) {
        Row(modifier = Modifier.width(totalsProductsWidth)) {
            Spacer(modifier = Modifier.width(40.dp))
            WooPosProductsScreen(
                modifier = Modifier.width(totalsProductsWidth - 56.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Row(modifier = Modifier.width(cartWidth)) {
            Spacer(modifier = Modifier.width(24.dp))
            WooPosCartScreen(Modifier.width(cartWidth - 48.dp))
            Spacer(modifier = Modifier.width(24.dp))
        }
        Row(modifier = Modifier.width(totalsProductsWidth)) {
            WooPosTotalsScreen(modifier = Modifier.width(totalsProductsWidth - 24.dp))
            Spacer(modifier = Modifier.width(24.dp))
        }
    }
}

@Composable
@WooPosPreview
fun WooPosHomeCartScreenPreview() {
    WooPosTheme {
        WooPosHomeScreen(
            state = WooPosHomeState.Cart.NotEmpty,
            onHomeUIEvent = { true },
            onNavigationEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosHomeCartEmptyScreenPreview() {
    WooPosTheme {
        WooPosHomeScreen(
            state = WooPosHomeState.Cart.Empty,
            onHomeUIEvent = { true },
            onNavigationEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosHomeCheckoutScreenPreview() {
    WooPosTheme {
        WooPosHomeScreen(
            state = WooPosHomeState.Checkout,
            onHomeUIEvent = { true },
            onNavigationEvent = {},
        )
    }
}
