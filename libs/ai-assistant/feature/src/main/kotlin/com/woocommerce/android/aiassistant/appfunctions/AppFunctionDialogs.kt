package com.woocommerce.android.aiassistant.appfunctions

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationDiffRow
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreview
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCardState
import com.woocommerce.android.aiassistant.ui.components.AssistantConfirmationCardSegment
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val PREVIEW_PRODUCT_ID = 12L
private const val PREVIEW_STOCK_QUANTITY = 5

@Composable
internal fun AppFunctionsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(), content = content)
}

@Composable
internal fun AppFunctionMessageDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message)
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.ai_assistant_app_functions_close))
                }
            }
        }
    }
}

@Composable
internal fun AppFunctionConfirmationDialog(
    state: AppFunctionConfirmationState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (state !is AppFunctionConfirmationState.Saving) onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.ai_assistant_app_functions_confirmation_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                when (state) {
                    AppFunctionConfirmationState.Loading -> CircularProgressIndicator()
                    is AppFunctionConfirmationState.Pending -> {
                        Text(stringResource(R.string.ai_assistant_app_functions_confirmation_store, state.storeName))
                        AssistantConfirmationCardSegment(
                            confirmation = state.card,
                            onConfirmWrite = onConfirm,
                            onCancelWrite = onDismiss,
                        )
                    }
                    is AppFunctionConfirmationState.Saving -> {
                        Text(stringResource(R.string.ai_assistant_app_functions_confirmation_store, state.storeName))
                        CircularProgressIndicator()
                        Text(stringResource(R.string.ai_assistant_app_functions_confirmation_saving))
                    }
                    is AppFunctionConfirmationState.Done -> {
                        Text(stringResource(R.string.ai_assistant_app_functions_confirmation_store, state.storeName))
                        AssistantConfirmationCardSegment(
                            confirmation = state.card,
                            onConfirmWrite = {},
                            onCancelWrite = {},
                        )
                        Text(stringResource(R.string.ai_assistant_app_functions_confirmation_done))
                        CloseButton(onDismiss)
                    }
                    AppFunctionConfirmationState.CheckInApp -> MessageWithClose(
                        message = stringResource(R.string.ai_assistant_app_functions_confirmation_check_in_app),
                        onDismiss = onDismiss,
                    )
                    is AppFunctionConfirmationState.StoreChanged -> MessageWithClose(
                        message = stringResource(
                            R.string.ai_assistant_app_functions_confirmation_store_changed,
                            state.requestedStoreName,
                        ),
                        onDismiss = onDismiss,
                    )
                    AppFunctionConfirmationState.Unavailable -> MessageWithClose(
                        message = stringResource(R.string.ai_assistant_app_functions_confirmation_unavailable),
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageWithClose(message: String, onDismiss: () -> Unit) {
    Text(message)
    CloseButton(onDismiss)
}

@Composable
private fun CloseButton(onDismiss: () -> Unit) {
    Button(onClick = onDismiss) {
        Text(stringResource(R.string.ai_assistant_app_functions_close))
    }
}

@Preview(showBackground = true)
@Composable
private fun AppFunctionConfirmationPendingPreview() {
    AppFunctionsTheme {
        AppFunctionConfirmationDialog(
            state = AppFunctionConfirmationState.Pending(
                storeName = "Bean Shop",
                store = StoreIdentity(7, "https://bean.example"),
                card = sampleStockConfirmationCard(),
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppFunctionConfirmationStoreChangedPreview() {
    AppFunctionsTheme {
        AppFunctionConfirmationDialog(
            state = AppFunctionConfirmationState.StoreChanged("Bean Shop"),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

private fun sampleStockConfirmationCard() = AssistantConfirmationCard(
    confirmationId = "confirmation-preview",
    toolCall = ToolCall(
        id = "call-preview",
        name = "products_update",
        arguments = buildJsonObject {
            put("id", PREVIEW_PRODUCT_ID)
            put("stock_quantity", PREVIEW_STOCK_QUANTITY)
        },
    ),
    state = AssistantConfirmationCardState.PENDING,
    preview = RenderedConfirmationPreview(
        message = "Update Cotton Tote (product 12)",
        fields = listOf(
            RenderedConfirmationDiffRow("stock_quantity", "Stock quantity", "5", "2"),
            RenderedConfirmationDiffRow("manage_stock", "Stock management", "On", "Off"),
        ),
    ),
)
