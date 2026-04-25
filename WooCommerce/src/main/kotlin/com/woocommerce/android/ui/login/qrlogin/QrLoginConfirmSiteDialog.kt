package com.woocommerce.android.ui.login.qrlogin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun QrLoginConfirmSiteDialog(
    host: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(id = R.string.login_qr_confirm_title)) },
        text = { Text(text = stringResource(id = R.string.login_qr_confirm_body, host)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.login_qr_confirm_connect))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(id = R.string.login_qr_confirm_cancel))
            }
        }
    )
}

@LightDarkThemePreviews
@Preview
@Composable
private fun QrLoginConfirmSiteDialogPreview() {
    WooThemeWithBackground {
        QrLoginConfirmSiteDialog(host = "store.example", onConfirm = {}, onCancel = {})
    }
}
