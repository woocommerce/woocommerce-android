package com.woocommerce.android.ui.woopos.home

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun PosExitConfirmationDialog(
    dialog: WooPosExitConfirmationDialog,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            onHomeUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogDismissed)
        },
        title = {
            Text(text = stringResource(id = dialog.title))
        },
        text = {
            Text(text = stringResource(id = dialog.message))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onHomeUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogDismissed)
                }
            ) {
                Text(text = stringResource(id = dialog.positiveButton).uppercase())
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onHomeUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogConfirmed)
                }
            ) {
                Text(
                    text = stringResource(id = dialog.negativeButton).uppercase(),
                )
            }
        },
    )
}

@Composable
@WooPosPreview
fun PreviewPosExitConfirmationDialog() {
    PosExitConfirmationDialog(WooPosExitConfirmationDialog) {}
}
