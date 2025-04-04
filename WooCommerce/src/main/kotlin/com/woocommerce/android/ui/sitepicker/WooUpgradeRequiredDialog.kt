package com.woocommerce.android.ui.sitepicker

import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooTheme

@Composable
fun WooUpgradeRequiredDialog(
    onUpdateInstructions: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.login_update_required_title),
                style = MaterialTheme.typography.h6,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.login_update_required_desc),
            )
        },
        confirmButton = {
            WCTextButton(
                onClick = { onUpdateInstructions() },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.button_update_instructions).uppercase(),
                    style = MaterialTheme.typography.button,
                )
            }
        },
        dismissButton = {
            WCTextButton(
                onClick = { onDismiss() },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.dismiss),
                    style = MaterialTheme.typography.button,
                )
            }
        }
    )
}

@Preview
@Composable
private fun PreviewWooUpgradeRequiredDialog() {
    WooTheme {
        WooUpgradeRequiredDialog(
            onUpdateInstructions = {},
            onDismiss = {},
        )
    }
}
