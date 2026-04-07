package com.woocommerce.android.ui.woopos.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosExitConfirmationDialog
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.modifier.listenForBarcodes
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.DialogState
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ScreenPositionState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsScreen
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosFloatingToolbar
import com.woocommerce.android.ui.woopos.home.totals.WooPosPhoneCheckoutScreen
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewModel
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupDialog

@Composable
fun WooPosPhoneHomeScreen(
    state: WooPosHomeState,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
) {
    // Pre-create the totals VM so it starts collecting parent events immediately.
    // Without this, the CheckoutClicked event is lost on the first checkout attempt
    // because MutableSharedFlow(replay=0) drops events when there are no collectors.
    hiltViewModel<WooPosTotalsViewModel>()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .listenForBarcodes(
                onBarcodeEvent = { result ->
                    onHomeUIEvent(WooPosHomeUIEvent.OnBarcodeEvent(result))
                },
                enabled = (
                    state.screenPositionState is ScreenPositionState.PhoneProductsBrowsing ||
                        state.screenPositionState is ScreenPositionState.Cart ||
                        state.screenPositionState is ScreenPositionState.Checkout.FullScreenTotals
                    ) && state.dialogState !is DialogState.ScanningSetupDialog
            )
    ) {
        when (state.screenPositionState) {
            is ScreenPositionState.PhoneProductsBrowsing -> {
                WooPosItemsScreen(modifier = Modifier.fillMaxSize())

                WooPosFloatingToolbar(
                    modifier = Modifier
                        .padding(WooPosSpacing.Large.value)
                        .align(Alignment.BottomStart),
                    isCompact = true,
                )

                WooPosPhoneCartFab(
                    modifier = Modifier
                        .padding(WooPosSpacing.Large.value)
                        .align(Alignment.BottomEnd),
                    onClick = { onHomeUIEvent(WooPosHomeUIEvent.CartFabClicked) }
                )
            }

            is ScreenPositionState.Cart -> {
                WooPosCartScreen(
                    modifier = Modifier.fillMaxSize(),
                    onPhoneBackClick = { onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked) },
                )
            }

            is ScreenPositionState.Checkout -> {
                WooPosPhoneCheckoutScreen(
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = { onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked) }
                )
            }
        }

        WooPosScanningSetupDialog(
            isVisible = state.dialogState is DialogState.ScanningSetupDialog,
            onDismissRequest = {
                onHomeUIEvent(WooPosHomeUIEvent.DismissScanningSetupDialog)
            }
        )

        WooPosExitConfirmationDialog(
            isVisible = state.dialogState is DialogState.ExitConfirmationDialog,
            title = stringResource(id = DialogState.ExitConfirmationDialog.title),
            message = stringResource(id = DialogState.ExitConfirmationDialog.message),
            dismissButtonText = stringResource(id = DialogState.ExitConfirmationDialog.confirmButton),
            onDismissRequest = { onHomeUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogDismissed) },
            onExit = { onHomeUIEvent(WooPosHomeUIEvent.ExitPosClicked) }
        )
    }
}
