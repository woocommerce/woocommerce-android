package com.woocommerce.android.ui.woopos.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.modifier.listenForBarcodes

/**
 * Root modifier for the POS home screen (both tablet and phone layouts).
 * Fills the viewport, paints the surface background, and forwards barcode events
 * into the home UI event stream while the user is on a scannable destination.
 */
@Composable
fun Modifier.wooPosHomeRootContainer(
    state: WooPosHomeState,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
): Modifier = this
    .fillMaxSize()
    .background(MaterialTheme.colorScheme.surface)
    .listenForBarcodes(
        onBarcodeEvent = { onHomeUIEvent(WooPosHomeUIEvent.OnBarcodeEvent(it)) },
        enabled = state.isBarcodeListeningEnabled(),
    )

private fun WooPosHomeState.isBarcodeListeningEnabled(): Boolean {
    val onScannableDestination =
        screenPositionState is WooPosHomeState.ScreenPositionState.Products ||
            screenPositionState is WooPosHomeState.ScreenPositionState.Cart ||
            screenPositionState is WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals
    return onScannableDestination && dialogState !is WooPosHomeState.DialogState.ScanningSetupDialog
}
