package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun WooPosConfirmationDialog(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    confirmButtonText: String,
    dismissButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismiss() },
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(text = confirmButtonText.uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(
                    text = dismissButtonText.uppercase(),
                )
            }
        },
    )
}

@Composable
@WooPosPreview
fun PreviewPosExitConfirmationDialog() {
    WooPosConfirmationDialog(
        title = "Title",
        message = "Message",
        confirmButtonText = "Positive",
        dismissButtonText = "Negative",
        onDismiss = {},
        onConfirm = {},
    )
}
