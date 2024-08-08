package com.woocommerce.android.ui.woopos.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosConfirmationDialog
import com.woocommerce.android.ui.woopos.common.composeui.isPreviewMode
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ProductsInfoDialog
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

    val productsWidthAnimatedDp by animateDpAsState(
        when (state.screenPositionState) {
            WooPosHomeState.ScreenPositionState.Cart.Hidden -> screenWidthDp

            is WooPosHomeState.ScreenPositionState.Cart.Visible.Empty,
            WooPosHomeState.ScreenPositionState.Cart.Visible.NotEmpty,
            WooPosHomeState.ScreenPositionState.Checkout.NotPaid -> productsWidthDp

            WooPosHomeState.ScreenPositionState.Checkout.Paid -> productsWidthDp - cartWidthDp
        },
        label = "productsWidthAnimatedDp"
    )

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
            is WooPosHomeState.ScreenPositionState.Cart.Visible.Empty -> .6f
            WooPosHomeState.ScreenPositionState.Cart.Visible.NotEmpty,
            WooPosHomeState.ScreenPositionState.Checkout.NotPaid,
            WooPosHomeState.ScreenPositionState.Checkout.Paid,
            WooPosHomeState.ScreenPositionState.Cart.Hidden -> 0f
        },
        label = "cartOverlayAnimated"
    )

    val scrollState = buildScrollStateForNavigationBetweenState(state.screenPositionState)
    WooPosHomeScreen(
        state = state,
        scrollState = scrollState,
        productsWidthDp = productsWidthAnimatedDp,
        cartWidthDp = cartWidthDp,
        cartOverlayIntensity = cartOverlayIntensityAnimated,
        totalsWidthDp = totalsWidthAnimatedDp,
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
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
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
            Row(
                modifier = Modifier.width(totalsWidthDp),
                horizontalArrangement = Arrangement.Center
            ) {
                WooPosHomeScreenTotals(
                    modifier = Modifier
                        .width(totalsWidthDp)
                )
            }
        }

        WooPosHomeScreenToolbar(
            modifier = Modifier
                .padding(24.dp.toAdaptivePadding())
                .align(Alignment.BottomStart),
        )

        HandleProductsInfoDialog(state.productsInfoDialog, onHomeUIEvent)
    }
}

@Composable
private fun HandleProductsInfoDialog(
    state: ProductsInfoDialog,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit
) {
    // isDialogVisible is mainly used to restore dialog after process death
    var isDialogVisible by rememberSaveable {
        mutableStateOf(false)
    }
    isDialogVisible = when (state) {
        /**
         * We can't set isDialogVisible to false directly because the dialog's state can be hidden for two different reasons:
         * 1. The user explicitly closed the dialog.
         * 2. After a process death and activity recreation, the dialog's state is reset to Hidden, as its initial state is Hidden.
         *
         * In the second scenario, isDialogVisible will be true if the dialog was visible before the process death,
         * since rememberSaveable retains this value across process death and activity recreation.
         */
        ProductsInfoDialog.Hidden -> isDialogVisible
        is ProductsInfoDialog.Visible -> true
    }
    if (isDialogVisible) {
        val currentState = state as? ProductsInfoDialog.Visible ?: ProductsInfoDialog.Visible(
            header = R.string.woopos_dialog_products_info_heading,
            primaryMessage = R.string.woopos_dialog_products_info_primary_message,
            secondaryMessage = R.string.woopos_dialog_products_info_secondary_message,
            primaryButton = ProductsInfoDialog.Visible.PrimaryButton(
                label = R.string.woopos_dialog_products_info_button_label
            )
        )
        BoxOverlay(state = currentState) {
            // no op
        }
        WooPosProductInfoDialog(
            state = currentState,
            onDismissRequest = {
                isDialogVisible = false
                onHomeUIEvent(WooPosHomeUIEvent.DismissProductsInfoDialog)
            }
        )
    }
}

@Composable
private fun BoxOverlay(
    state: ProductsInfoDialog,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = state is ProductsInfoDialog.Visible,
        enter = fadeIn(initialAlpha = 0.3f),
        exit = fadeOut(targetAlpha = 0.0f)
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onClick()
                }
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                )
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
            state = WooPosHomeState(
                screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Visible.NotEmpty,
                productsInfoDialog = ProductsInfoDialog.Hidden
            ),
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
                screenPositionState = WooPosHomeState.ScreenPositionState.Cart.Visible.Empty,
                productsInfoDialog = ProductsInfoDialog.Hidden
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
            state = WooPosHomeState(
                screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.NotPaid,
                productsInfoDialog = ProductsInfoDialog.Hidden
            ),
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
            state = WooPosHomeState(
                screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.Paid,
                productsInfoDialog = ProductsInfoDialog.Hidden
            ),
            onHomeUIEvent = { },
            onNavigationEvent = {},
        )
    }
}
