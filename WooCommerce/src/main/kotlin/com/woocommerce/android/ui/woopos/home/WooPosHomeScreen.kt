package com.woocommerce.android.ui.woopos.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.isPreviewMode
import com.woocommerce.android.ui.woopos.home.toolbar.PreviewWooPosFloatingToolbarStatusConnectedWithMenu
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosFloatingToolbar
import org.wordpress.android.util.ToastUtils

@Composable
fun WooPosHomeScreen(
    isPaymentCompletedViaCash: Boolean,
    viewModel: WooPosHomeViewModel,
) {
    val state = viewModel.state.collectAsState().value
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (isPaymentCompletedViaCash) {
            viewModel.onUIEvent(WooPosHomeUIEvent.OnPaymentCompletedViaCash)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            ToastUtils.showToast(
                context,
                message,
                ToastUtils.Duration.LONG
            )
        }
    }

    WooPosHomeScreen(
        state = state,
        onHomeUIEvent = { viewModel.onUIEvent(it) },
    )
}

@Composable
private fun WooPosHomeScreen(
    state: WooPosHomeState,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
) {
    BackHandler {
        onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
    }

    val current = LocalConfiguration.current
    val screenWidthDp = remember { current.screenWidthDp.dp }
    val cartWidthDp = remember(screenWidthDp) { screenWidthDp * .35f }
    val productsWidthDp = remember(screenWidthDp, cartWidthDp) { screenWidthDp - cartWidthDp }
    val totalsWidthDp = remember(screenWidthDp, cartWidthDp) { screenWidthDp - cartWidthDp }

    val productsWidthAnimatedDp by animateDpAsState(
        when (state.screenPositionState) {
            WooPosHomeState.ScreenPositionState.Products,
            is WooPosHomeState.ScreenPositionState.Cart,
            WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals -> productsWidthDp

            WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals -> productsWidthDp - cartWidthDp
        },
        label = "productsWidthAnimatedDp"
    )

    val totalsWidthAnimatedDp by animateDpAsState(
        when (state.screenPositionState) {
            is WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals -> screenWidthDp
            WooPosHomeState.ScreenPositionState.Products,
            is WooPosHomeState.ScreenPositionState.Cart,
            WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals -> totalsWidthDp
        },
        label = "totalsWidthAnimatedDp"
    )

    val scrollState = buildScrollStateForNavigationBetweenState(state.screenPositionState)
    WooPosHomeScreen(
        state = state,
        scrollState = scrollState,
        productsWidthDp = productsWidthAnimatedDp,
        cartWidthDp = cartWidthDp,
        totalsWidthDp = totalsWidthAnimatedDp,
        onHomeUIEvent = onHomeUIEvent,
    )
}

@Composable
private fun WooPosHomeScreen(
    state: WooPosHomeState,
    scrollState: ScrollState,
    productsWidthDp: Dp,
    cartWidthDp: Dp,
    totalsWidthDp: Dp,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
) {
    Box(
        modifier = Modifier.wooPosHomeRootContainer(state, onHomeUIEvent)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState, enabled = false)
                .fillMaxWidth(),
        ) {
            WooPosHomeProductsPane(
                modifier = Modifier
                    .width(productsWidthDp)
            )
            WooPosHomeCartPane(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .width(cartWidthDp)
            )
            WooPosHomeTotalsPane(
                modifier = Modifier
                    .width(totalsWidthDp),
            )
        }

        if (!isPreviewMode()) {
            WooPosHomeScreenToolbar(
                modifier = Modifier
                    .padding(WooPosSpacing.Large.value)
                    .align(Alignment.BottomStart),
            )

            WooPosHomeDialogs(state.dialogState, onHomeUIEvent)
        }
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
            WooPosHomeState.ScreenPositionState.Products,
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
            state = WooPosHomeState(
                screenPositionState = WooPosHomeState.ScreenPositionState.Cart,
                dialogState = WooPosHomeState.DialogState.Hidden,
            ),
            onHomeUIEvent = { },
        )
    }
}

@Composable
@WooPosPreview
fun WooPosHomeCheckoutScreenPreview() {
    WooPosTheme {
        WooPosHomeScreen(
            state = WooPosHomeState(
                screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals,
                dialogState = WooPosHomeState.DialogState.Hidden,
            ),
            onHomeUIEvent = { },
        )
    }
}

@Composable
@WooPosPreview
fun WooPosHomeCheckoutPaidScreenPreview() {
    WooPosTheme {
        WooPosHomeScreen(
            state = WooPosHomeState(
                screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals,
                dialogState = WooPosHomeState.DialogState.Hidden,
            ),
            onHomeUIEvent = { },
        )
    }
}
