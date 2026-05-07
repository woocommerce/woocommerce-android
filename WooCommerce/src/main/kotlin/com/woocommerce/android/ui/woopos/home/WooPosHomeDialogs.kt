package com.woocommerce.android.ui.woopos.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionDialog
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosExitConfirmationDialog
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupDialog

@Composable
fun WooPosHomeDialogs(
    dialogState: WooPosHomeState.DialogState,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
) {
    WooPosScanningSetupDialog(
        isVisible = dialogState is WooPosHomeState.DialogState.ScanningSetupDialog,
        onDismissRequest = {
            onHomeUIEvent(WooPosHomeUIEvent.DismissScanningSetupDialog)
        }
    )

    WooPosExitConfirmationDialog(
        isVisible = dialogState is WooPosHomeState.DialogState.ExitConfirmationDialog,
        title = stringResource(id = WooPosHomeState.DialogState.ExitConfirmationDialog.title),
        message = stringResource(id = WooPosHomeState.DialogState.ExitConfirmationDialog.message),
        dismissButtonText = stringResource(id = WooPosHomeState.DialogState.ExitConfirmationDialog.confirmButton),
        onDismissRequest = { onHomeUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogDismissed) },
        onExit = { onHomeUIEvent(WooPosHomeUIEvent.ExitPosClicked) }
    )

    if (dialogState is WooPosHomeState.DialogState.CardReaderConnectionDialog) {
        WooPosCardReaderConnectionDialog(
            onDismiss = { onHomeUIEvent(WooPosHomeUIEvent.DismissCardReaderConnectionDialog) },
            onConnectionSuccess = { onHomeUIEvent(WooPosHomeUIEvent.DismissCardReaderConnectionDialog) }
        )
    }
}
