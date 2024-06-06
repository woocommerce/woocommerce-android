package com.woocommerce.android.ui.woopos.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsScreen

@Composable
fun WooPosHomeScreen() {
    val viewModel: WooPosHomeViewModel = hiltViewModel()
    WooPosHomeScreen(
        viewModel.state.collectAsState().value,
        viewModel::onUIEvent
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
            state.exitConfirmationDialog?.let { dialog ->
                PosExitConfirmationDialog(dialog, onHomeUIEvent)
            }
            WooPosHomeScreen(scrollState, totalsProductsWidth, cartWidth)
        }

        WooPosHomeState.Checkout -> WooPosHomeScreen(scrollState, totalsProductsWidth, cartWidth)
    }
}

@Composable
private fun WooPosHomeScreen(
    scrollState: ScrollState,
    totalsProductsWidth: Dp,
    cartWidth: Dp
) {
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
private fun PosExitConfirmationDialog(
    dialog: WooPosExitConfirmationDialog,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            onHomeUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogDismissed)
        },
        title = { Text(text = stringResource(id = dialog.title)) },
        text = { Text(text = stringResource(id = dialog.message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onHomeUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogDismissed)
                }
            ) {
                Text(text = stringResource(id = dialog.positiveButton))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onHomeUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogConfirmed)
                }
            ) {
                Text(text = stringResource(id = dialog.negativeButton))
            }
        },
    )
}

@Composable
@WooPosPreview
fun WooPosHomeCartScreenPreview() {
    WooPosHomeScreen(state = WooPosHomeState.Cart(), onHomeUIEvent = {})
}

@Composable
@WooPosPreview
fun WooPosHomeCartWithExitPOSConfirmationScreenPreview() {
    WooPosHomeScreen(
        state = WooPosHomeState.Cart(
            exitConfirmationDialog = WooPosExitConfirmationDialog(
                title = R.string.woopos_exit_confirmation_title,
                message = R.string.woopos_exit_confirmation_message,
                positiveButton = R.string.woopos_exit_confirmation_positive_button,
                negativeButton = R.string.woopos_exit_confirmation_negative_button,
            )
        ),
        onHomeUIEvent = {}
    )
}

@Composable
@WooPosPreview
fun WooPosHomeCheckoutScreenPreview() {
    WooPosHomeScreen(state = WooPosHomeState.Checkout, onHomeUIEvent = {})
}
