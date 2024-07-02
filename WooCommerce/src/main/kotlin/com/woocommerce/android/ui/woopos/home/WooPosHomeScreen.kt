package com.woocommerce.android.ui.woopos.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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

    println("WooPosHomeScreen: state=$state")

    val current = LocalConfiguration.current
    val screenWidthDp = remember { current.screenWidthDp.dp }
    val cartWidthDp = remember(screenWidthDp) { screenWidthDp * .35f }
    val productsWidthDp = remember(screenWidthDp, cartWidthDp) { screenWidthDp - cartWidthDp }
    val totalsWidthDp = remember(screenWidthDp, cartWidthDp) { screenWidthDp - cartWidthDp }

    val totalsWidthAnimatedDp by animateDpAsState(
        when (state) {
            is WooPosHomeState.Checkout.Paid -> screenWidthDp
            is WooPosHomeState.Cart,
            WooPosHomeState.Checkout.NotPaid -> totalsWidthDp
        },
        label = "totalsWidthAnimatedDp"
    )

    val productsWidthAnimatedDp by animateDpAsState(
        when (state) {
            is WooPosHomeState.Cart.Empty -> productsWidthDp + cartWidthDp.times(.77f)
            is WooPosHomeState.Checkout.Paid -> productsWidthDp - cartWidthDp
            WooPosHomeState.Cart.NotEmpty,
            WooPosHomeState.Checkout.NotPaid -> productsWidthDp
        },
        label = "productsWidthAnimatedDp"
    )

    val cartOverlayIntensityAnimated by animateFloatAsState(
        when (state) {
            is WooPosHomeState.Cart.Empty -> .4f
            WooPosHomeState.Cart.NotEmpty,
            WooPosHomeState.Checkout.NotPaid,
            WooPosHomeState.Checkout.Paid -> 0f
        },
        label = "cartOverlayAnimated"
    )

    val totalsStartPaddingDp = remember(state) {
        when (state) {
            WooPosHomeState.Cart.Empty,
            WooPosHomeState.Cart.NotEmpty,
            WooPosHomeState.Checkout.NotPaid -> 0.dp

            WooPosHomeState.Checkout.Paid -> 24.dp
        }
    }

    val scrollState = buildScrollStateForNavigationBetweenState(state)
    WooPosHomeScreen(
        scrollState = scrollState,
        productsWidthDp = productsWidthAnimatedDp,
        cartWidthDp = cartWidthDp,
        cartOverlayIntensity = cartOverlayIntensityAnimated,
        totalsWidthDp = totalsWidthAnimatedDp,
        totalsStartPaddingDp = totalsStartPaddingDp,
    )
}

@Composable
private fun WooPosHomeScreen(
    scrollState: ScrollState,
    productsWidthDp: Dp,
    cartWidthDp: Dp,
    cartOverlayIntensity: Float,
    totalsWidthDp: Dp,
    totalsStartPaddingDp: Dp,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(scrollState, enabled = false)
            .fillMaxWidth(),
    ) {
        Row(modifier = Modifier.width(productsWidthDp)) {
            Spacer(modifier = Modifier.width(40.dp))
            WooPosProductsScreen(
                modifier = Modifier
                    .width(productsWidthDp - 56.dp)
                    .padding(top = 36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Row(modifier = Modifier.width(cartWidthDp)) {
            Spacer(modifier = Modifier.width(24.dp))
            Box {
                WooPosCartScreen(
                    Modifier
                        .width(cartWidthDp - 48.dp)
                        .padding(vertical = 24.dp)
                )
                Box(
                    modifier = Modifier
                        .width(cartWidthDp - 48.dp)
                        .padding(vertical = 24.dp)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colors.background.copy(alpha = cartOverlayIntensity),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
        }
        Row(modifier = Modifier.width(totalsWidthDp)) {
            Spacer(modifier = Modifier.width(totalsStartPaddingDp))
            WooPosTotalsScreen(
                modifier = Modifier
                    .width(totalsWidthDp - 24.dp - totalsStartPaddingDp)
                    .padding(vertical = 24.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
        }
    }
}

@Composable
private fun buildScrollStateForNavigationBetweenState(state: WooPosHomeState): ScrollState {
    val scrollState = rememberScrollState()
    LaunchedEffect(state) {
        val animationSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 200f)
        when (state) {
            is WooPosHomeState.Cart -> {
                scrollState.animateScrollTo(
                    0,
                    animationSpec = animationSpec
                )
            }

            is WooPosHomeState.Checkout.NotPaid -> scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = animationSpec
            )

            WooPosHomeState.Checkout.Paid -> {
                // avoid animated scrolling to the end of the screen as we extend Payment successful screen
            }
        }
    }
    LaunchedEffect(scrollState.maxValue) {
        when (state) {
            is WooPosHomeState.Cart -> scrollState.scrollTo(0)

            is WooPosHomeState.Checkout -> scrollState.scrollTo(scrollState.maxValue)
        }
    }
    return scrollState
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
            state = WooPosHomeState.Checkout.NotPaid,
            onHomeUIEvent = { true },
            onNavigationEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosHomeCheckoutPaidScreenPreview() {
    WooPosTheme {
        WooPosHomeScreen(
            state = WooPosHomeState.Checkout.Paid,
            onHomeUIEvent = { true },
            onNavigationEvent = {},
        )
    }
}
