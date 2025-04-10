package com.woocommerce.android.ui.orders.wooshippinglabels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun SuccessSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            val actionLabel = snackbarData.visuals.actionLabel

            SuccessSnackbar(
                content = snackbarData.visuals.message,
                actionLabel = actionLabel,
                action = { snackbarData.performAction() }
            )
        }
    )
}

@Composable
private fun SuccessSnackbar(
    content: String,
    actionLabel: String?,
    action: () -> Unit
) {
    Surface(
        color = SnackbarDefaults.color,
        shape = SnackbarDefaults.shape,
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = colorResource(R.color.woo_green_20),
            )

            Text(
                text = content,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = SnackbarDefaults.contentColor
            )

            actionLabel?.let { label ->
                TextButton(
                    onClick = action,
                    colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionContentColor)
                ) {
                    Text(text = label)
                }
            }
        }
    }
}

@Preview(name = "Standard snackbar")
@Composable
fun SuccessSnackbarHostPreview() {
    SuccessSnackbar(
        content = "Shipping label created successfully",
        actionLabel = "View",
        action = {}
    )
}

@Preview(name = "Snackbar with long message")
@Composable
fun SuccessSnackbarHostWithLongMessagePreview() {
    SuccessSnackbar(
        content = "Shipping label created successfully in a long long long long long message",
        actionLabel = "View",
        action = {}
    )
}
