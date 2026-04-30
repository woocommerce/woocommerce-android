package com.woocommerce.android.aiassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.runtime.AssistantPendingConfirmation
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun AssistantChatScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by rememberSaveable { mutableStateOf("") }

    AssistantChatScreen(
        state = state,
        inputText = inputText,
        onInputTextChange = { inputText = it },
        onSendMessage = {
            val message = inputText
            inputText = ""
            viewModel.onSendMessage(message)
        },
        onCancelTurn = viewModel::onCancelTurn,
        onRetry = viewModel::onRetry,
        onConfirmWrite = viewModel::onConfirmWrite,
        onCancelWrite = viewModel::onCancelWrite,
        modifier = modifier,
    )
}

@Composable
fun AssistantChatScreen(
    state: AssistantUiState,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onCancelTurn: () -> Unit,
    onRetry: () -> Unit,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme {
        Surface(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                AssistantMessageThread(
                    messages = state.messages,
                    modifier = Modifier.weight(1f),
                )
                AssistantStatusPanel(
                    state = state,
                    onRetry = onRetry,
                    onConfirmWrite = onConfirmWrite,
                    onCancelWrite = onCancelWrite,
                )
                AssistantComposer(
                    inputText = inputText,
                    onInputTextChange = onInputTextChange,
                    isStreaming = state.isStreaming,
                    onSendMessage = onSendMessage,
                    onCancelTurn = onCancelTurn,
                )
            }
        }
    }
}

@Composable
private fun AssistantMessageThread(
    messages: List<AssistantUiMessage>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag(AssistantChatTestTags.THREAD),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        items(
            items = messages,
            key = { it.id },
        ) { message ->
            AssistantMessageBubble(message = message)
        }
    }
}

@Composable
private fun AssistantMessageBubble(message: AssistantUiMessage) {
    val isUser = message.role == AssistantUiMessage.Role.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AssistantChatTestTags.message(message.id)),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .heightIn(min = 40.dp)
                .background(bubbleColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = message.text.ifEmpty { " " },
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = if (isUser) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun AssistantStatusPanel(
    state: AssistantUiState,
    onRetry: () -> Unit,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
) {
    when (state.status) {
        AssistantUiStatus.ERROR -> AssistantErrorPanel(state.error, state.canRetry, onRetry)
        AssistantUiStatus.AWAITING_CONFIRMATION -> state.pendingConfirmation?.let {
            AssistantConfirmationPanel(
                confirmation = it,
                onConfirmWrite = onConfirmWrite,
                onCancelWrite = onCancelWrite,
            )
        }
        AssistantUiStatus.IDLE,
        AssistantUiStatus.STREAMING -> Unit
    }
}

@Composable
private fun AssistantErrorPanel(
    error: AssistantUiError?,
    canRetry: Boolean,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = error.toDisplayText(),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (canRetry) {
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun AssistantConfirmationPanel(
    confirmation: AssistantPendingConfirmation,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Confirm ${confirmation.toolCall.name}?",
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onConfirmWrite) {
                Text("Confirm")
            }
            OutlinedButton(onClick = onCancelWrite) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun AssistantComposer(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isStreaming: Boolean,
    onSendMessage: () -> Unit,
    onCancelTurn: () -> Unit,
) {
    val canSend = inputText.isNotBlank() && !isStreaming
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier
                .weight(1f)
                .testTag(AssistantChatTestTags.INPUT),
            minLines = 1,
            maxLines = 4,
            placeholder = { Text("Ask about your store") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (canSend) {
                        onSendMessage()
                    }
                }
            ),
        )
        if (isStreaming) {
            OutlinedButton(onClick = onCancelTurn) {
                Text("Stop")
            }
        } else {
            Button(
                onClick = onSendMessage,
                enabled = canSend,
            ) {
                Text("Send")
            }
        }
    }
}

private fun AssistantUiError?.toDisplayText(): String = when (this) {
    AssistantUiError.NETWORK -> "Network error"
    AssistantUiError.AUTH -> "Authentication error"
    AssistantUiError.RATE_LIMIT -> "Rate limit reached"
    AssistantUiError.TIMEOUT -> "Request timed out"
    AssistantUiError.UPSTREAM_FAILURE -> "Assistant service error"
    AssistantUiError.TOOL_FAILED -> "Tool failed"
    AssistantUiError.INVALID_TOOL_CALL -> "Invalid tool call"
    AssistantUiError.OUTCOME_UNKNOWN -> "Outcome unknown"
    AssistantUiError.CANCELLED -> "Request cancelled"
    AssistantUiError.CONFIRMATION_DEFERRED -> "Confirmation is not available yet"
    AssistantUiError.MAX_ITERATIONS -> "Assistant reached its turn limit"
    AssistantUiError.UNKNOWN,
    null -> "Something went wrong"
}

@Preview(showBackground = true, widthDp = 390, heightDp = 720)
@Composable
private fun AssistantChatScreenPreview() {
    AssistantChatScreen(
        state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("preview-1", AssistantUiMessage.Role.USER, "Show today's sales"),
                AssistantUiMessage("preview-2", AssistantUiMessage.Role.ASSISTANT, "Sales are up 12% today."),
            )
        ),
        inputText = "",
        onInputTextChange = {},
        onSendMessage = {},
        onCancelTurn = {},
        onRetry = {},
        onConfirmWrite = {},
        onCancelWrite = {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 720)
@Composable
private fun AssistantChatScreenConfirmationPreview() {
    AssistantChatScreen(
        state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("preview-1", AssistantUiMessage.Role.USER, "Cancel order 123"),
                AssistantUiMessage("preview-2", AssistantUiMessage.Role.ASSISTANT, "I need confirmation first."),
            ),
            status = AssistantUiStatus.AWAITING_CONFIRMATION,
            pendingConfirmation = AssistantPendingConfirmation(
                id = "confirmation-preview",
                toolCall = ToolCall(
                    id = "call-preview",
                    name = "orders_update",
                    arguments = buildJsonObject { put("id", 123) },
                ),
            ),
        ),
        inputText = "",
        onInputTextChange = {},
        onSendMessage = {},
        onCancelTurn = {},
        onRetry = {},
        onConfirmWrite = {},
        onCancelWrite = {},
    )
}
