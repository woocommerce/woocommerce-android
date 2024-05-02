package com.woocommerce.android.ui.compose.component

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R

@Composable
fun DiscardChangesDialog(
    discardButton: () -> Unit,
    dismissButton: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        text = {
            Text(text = stringResource(id = R.string.discard_message))
        },
        confirmButton = {
            TextButton(onClick = dismissButton) {
                Text(stringResource(id = R.string.keep_editing).uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = discardButton) {
                Text(stringResource(id = R.string.discard).uppercase())
            }
        },
        neutralButton = {}
    )
}
