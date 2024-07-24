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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosConfirmationDialog
import com.woocommerce.android.ui.woopos.common.composeui.isPreviewMode
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreenProductsPreview
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsScreenPreview
import com.woocommerce.android.ui.woopos.home.toolbar.PreviewWooPosFloatingToolbarStatusConnectedWithMenu
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosFloatingToolbar
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsScreen
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsScreenPreview
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
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
) {
    BackHandler {
        onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
    }

    state.exitConfirmationDialog?.let {
        WooPosConfirmationDialog(
            title = stringResource(id = it.title),
            message = stringResource(id = it.message),
            confirmButtonText = stringResource(id = it.confirmButton),
            dismissButtonText = stringResource(id = it.dismissButton),
            onDismiss = { onHomeUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogDismissed) },
            onConfirm = { onNavigationEvent(WooPosNavigationEvent.ExitPosClicked) }
        )
    }

    val current = LocalConfiguration.current
    val screenWidthDp = remember { current.screenWidthDp.dp }
    val cartWidthDp = remember(screenWidthDp) { screenWidthDp * .35f }
    val productsWidthDp = remember(screenWidthDp, cartWidthDp) { screenWidthDp - cartWidthDp }
    val totalsWidthDp = remember(screenWidthDp, cartWidthDp) { screenWidthDp - cartWidthDp }

    val totalsWidthAnimatedDp by animateDpAsState(
        when (state.screenPositionState) {
            is WooPosHomeState.ScreenPositionState.Checkout.Paid -> screenWidthDp
            is WooPosHomeState.ScreenPositionState.Cart,
            WooPosHomeState.ScreenPositionState.Checkout.NotPaid -> totalsWidthDp
        },
        label = "totalsWidthAnimatedDp"
    )

    val cartOverlayIntensityAnimated by animateFloatAsState(
        when (state.screenPositionState) {
            is WooPosHomeState.ScreenPositionState.Cart.Empty -> .6f
            WooPosHomeState.ScreenPositionState.Cart.NotEmpty,
            WooPosHomeState.ScreenPositionState.Checkout.NotPaid,
            WooPosHomeState.ScreenPositionState.Checkout.Paid -> 0f
        },
        label = "cartOverlayAnimated"
    )

    val totalsStartPaddingAnimatedDp by animateDpAsState(
        when (state.screenPositionState) {
            is WooPosHomeState.ScreenPositionState.Cart,
            WooPosHomeState.ScreenPositionState.Checkout.NotPaid -> 0.dp.toAdaptivePadding()

            WooPosHomeState.ScreenPositionState.Checkout.Paid -> 24.dp.toAdaptivePadding()
        },
        label = "totalsStartPaddingAnimatedDp"
    )

    val scrollState = buildScrollStateForNavigationBetweenState(state.screenPositionState)
    WooPosHomeScreen(
        scrollState = scrollState,
        productsWidthDp = productsWidthDp,
        cartWidthDp = cartWidthDp,
        cartOverlayIntensity = cartOverlayIntensityAnimated,
        totalsWidthDp = totalsWidthAnimatedDp,
        totalsStartPaddingDp = totalsStartPaddingAnimatedDp,
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
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState, enabled = false)
                .fillMaxWidth(),
        ) {
            Row(modifier = Modifier.width(productsWidthDp)) {
                WooPosHomeScreenProducts(
                    modifier = Modifier
                        .width(productsWidthDp)
                )
            }
            Row(
                modifier = Modifier
                    .width(cartWidthDp)
                    .background(MaterialTheme.colors.surface)
            ) {
                Box {
                    WooPosHomeScreenCart(
                        modifier = Modifier
                            .width(cartWidthDp)
                    )
                    Box(
                        modifier = Modifier
                            .width(cartWidthDp)
                            .fillMaxHeight()
                            .background(
                                color = MaterialTheme.colors.background.copy(alpha = cartOverlayIntensity),
                            )
                    )
                }
            }
            Row(modifier = Modifier.width(totalsWidthDp)) {
                Spacer(modifier = Modifier.width(totalsStartPaddingDp))
                WooPosHomeScreenTotals(
                    modifier = Modifier
                        .width(totalsWidthDp - 24.dp.toAdaptivePadding() - totalsStartPaddingDp)
                        .padding(vertical = 24.dp.toAdaptivePadding())
                )
                Spacer(modifier = Modifier.width(24.dp.toAdaptivePadding()))
            }
        }

        WooPosHomeScreenToolbar(
            modifier = Modifier
                .padding(24.dp.toAdaptivePadding())
                .align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun WooPosHomeScreenProducts(modifier: Modifier) {
    if (isPreviewMode()) {
        WooPosProductsScreenPreview(modifier)
    } else {
        WooPosProductsScreen(modifier)
    }
}

@Composable
private fun WooPosHomeScreenCart(modifier: Modifier) {
    if (isPreviewMode()) {
        WooPosCartScreenProductsPreview(modifier)
    } else {
        WooPosCartScreen(modifier)
    }
}

@Composable
private fun WooPosHomeScreenTotals(modifier: Modifier) {
    if (isPreviewMode()) {
        WooPosTotalsScreenPreview(modifier)
    } else {
        WooPosTotalsScreen(modifier)
    }
}

@Composable
private fun WooPosHomeScreenToolbar(modifier: Modifier) {
    if (isPreviewMode()) {
        PreviewWooPosFloatingToolbarStatusConnectedWithMenu()
    } else {
        WooPosFloatingToolbar(modifier = modifier)
    }
}

@Composable
private fun buildScrollStateForNavigationBetweenState(state: WooPosHomeState.ScreenPositionState): ScrollState {
    val scrollState = rememberScrollState()
    LaunchedEffect(state) {
        val animationSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 200f)
        when (state) {
            is WooPosHomeState.ScreenPositionState.Cart ->
                scrollState.animateScrollTo(
                    0,
                    animationSpec = animationSpec
                )

            is WooPosHomeState.ScreenPositionState.Checkout -> scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = animationSpec
            )
        }
    }
    return scrollState
}

@Composable
@WooPosPreview
fun WooPosHomeCartScreenPreview() {
    WooPosTheme {
        WooPosHomeScreen(
            state = WooPosHomeState(screenPositionState = WooPosHomeState.ScreenPositionState.Cart.NotEmpty),
            onHomeUIEvent = { },
            onNavigationEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosHomeCartEmptyScreenPreview() {
    WooPosTheme {
        WooPosHomeScreen(
            state = WooPosHomeState(
                screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Empty
            ),
            onHomeUIEvent = { },
            onNavigationEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosHomeCheckoutScreenPreview() {
    WooPosTheme {
        WooPosHomeScreen(
            state = WooPosHomeState(screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.NotPaid),
            onHomeUIEvent = { },
            onNavigationEvent = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosHomeCheckoutPaidScreenPreview() {
    WooPosTheme {
        WooPosHomeScreen(
            state = WooPosHomeState(screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.Paid),
            onHomeUIEvent = { },
            onNavigationEvent = {},
        )
    }
}
