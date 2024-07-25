package com.woocommerce.android.ui.woopos.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.isPreviewMode
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreenProductsPreview
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsScreenPreview
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
    onHomeUIEvent: (WooPosHomeUIEvent) -> Boolean,
) {
    BackHandler {
        val result = onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
        if (!result) {
            onNavigationEvent(WooPosNavigationEvent.BackFromHomeClicked)
        }
    }

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

            is WooPosHomeState.ProductsInfoDialog.Visible, WooPosHomeState.ProductsInfoDialog.Hidden -> screenWidthDp
        },
        label = "totalsWidthAnimatedDp"
    )

    val cartOverlayIntensityAnimated by animateFloatAsState(
        when (state) {
            is WooPosHomeState.Cart.Empty -> .6f
            WooPosHomeState.Cart.NotEmpty,
            WooPosHomeState.Checkout.NotPaid,
            WooPosHomeState.Checkout.Paid,
            is WooPosHomeState.ProductsInfoDialog.Visible, WooPosHomeState.ProductsInfoDialog.Hidden -> 0f
        },
        label = "cartOverlayAnimated"
    )

    val totalsStartPaddingAnimatedDp by animateDpAsState(
        when (state) {
            is WooPosHomeState.Cart,
            WooPosHomeState.Checkout.NotPaid,
            is WooPosHomeState.ProductsInfoDialog.Visible, WooPosHomeState.ProductsInfoDialog.Hidden ->
                0.dp.toAdaptivePadding()

            WooPosHomeState.Checkout.Paid -> 24.dp.toAdaptivePadding()
        },
        label = "totalsStartPaddingAnimatedDp"
    )

    val scrollState = buildScrollStateForNavigationBetweenState(state)
    WooPosHomeScreen(
        state = state,
        scrollState = scrollState,
        productsWidthDp = productsWidthDp,
        cartWidthDp = cartWidthDp,
        cartOverlayIntensity = cartOverlayIntensityAnimated,
        totalsWidthDp = totalsWidthAnimatedDp,
        totalsStartPaddingDp = totalsStartPaddingAnimatedDp,
        onHomeUIEvent,
    )
}

@Composable
private fun WooPosHomeScreen(
    state: WooPosHomeState,
    scrollState: ScrollState,
    productsWidthDp: Dp,
    cartWidthDp: Dp,
    cartOverlayIntensity: Float,
    totalsWidthDp: Dp,
    totalsStartPaddingDp: Dp,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Boolean,
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
//        TestAnimatedVisibility(state, onHomeUIEvent)
        HandleProductsInfoDialog(state, onHomeUIEvent)
    }
}

@Composable
fun TestAnimatedVisibility(
    state: WooPosHomeState,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Boolean
) {
    println(state.hashCode() + onHomeUIEvent.hashCode())
    var isVisible by remember { mutableStateOf(true) }

    Column {
        Button(onClick = {
            isVisible = !isVisible
        }) {
            Text("Toggle Visibility")
        }
        AnimatedVisibility(
            visible = isVisible,
            enter = expandVertically(animationSpec = tween(durationMillis = 5000)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 2000))
        ) {
//            Dialog(
//                onDismissRequest = { isVisible = !isVisible }
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(100.dp)
//                        .background(Color.Red)
//                )
//            }
            ProductInfoDialog(
                state = WooPosHomeState.ProductsInfoDialog.Visible(
                    header = R.string.woopos_dialog_products_info_heading,
                    primaryMessage = R.string.woopos_dialog_products_info_primary_message,
                    secondaryMessage = R.string.woopos_dialog_products_info_secondary_message,
                    secondaryMessageActionLabel = R.string.woopos_dialog_products_info_secondary_message_action_label,
                    primaryButton = WooPosHomeState.ProductsInfoDialog.Visible.PrimaryButton(
                        label = R.string.woopos_dialog_products_info_button_label
                    )
                ),
                onDismissRequest = {
                    isVisible = !isVisible
                }
            ) {
            }
        }
    }
}

@Composable
private fun HandleProductsInfoDialog(
    state: WooPosHomeState,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Boolean
) {
    if (state is WooPosHomeState.ProductsInfoDialog.Visible) {
        ProductInfoDialog(
            state,
            onDismissRequest = {
                onHomeUIEvent(WooPosHomeUIEvent.DismissProductsInfoDialog)
            },
            onCreateOrderClick = {}
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

            is WooPosHomeState.Checkout -> scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = animationSpec
            )

            is WooPosHomeState.ProductsInfoDialog.Visible, WooPosHomeState.ProductsInfoDialog.Hidden -> {
                // no op
            }
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
