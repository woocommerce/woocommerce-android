package com.woocommerce.android.aiassistant.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.R
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
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            AssistantHeader(status = state.status)
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

@Composable
private fun AssistantHeader(status: AssistantUiStatus) {
    val statusLabel = status.toHeaderText()
    val statusContentDescription = stringResource(
        R.string.assistant_chat_status_content_description,
        statusLabel,
    )
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.assistant_chat_title),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Surface(
                modifier = Modifier.semantics {
                    contentDescription = statusContentDescription
                },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
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
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
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
    val messageText = message.text.ifEmpty { " " }
    val messageContentDescription = if (isUser) {
        stringResource(R.string.assistant_chat_message_user_content_description, messageText)
    } else {
        stringResource(R.string.assistant_chat_message_assistant_content_description, messageText)
    }
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AssistantChatTestTags.message(message.id))
            .semantics { contentDescription = messageContentDescription },
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .heightIn(min = 40.dp)
                .background(bubbleColor, shape)
                .then(
                    if (isUser) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = messageText,
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
                Text(stringResource(R.string.assistant_chat_retry))
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
            text = stringResource(R.string.assistant_chat_confirm_tool, confirmation.toolCall.name),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onConfirmWrite) {
                Text(stringResource(R.string.assistant_chat_confirm))
            }
            OutlinedButton(onClick = onCancelWrite) {
                Text(stringResource(R.string.assistant_chat_cancel))
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
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text(stringResource(R.string.assistant_chat_placeholder)) },
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
                    OutlinedButton(
                        onClick = onCancelTurn,
                        modifier = Modifier.heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.assistant_chat_stop))
                    }
                } else {
                    Button(
                        onClick = onSendMessage,
                        enabled = canSend,
                        modifier = Modifier.heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.assistant_chat_send))
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantUiStatus.toHeaderText(): String = when (this) {
    AssistantUiStatus.IDLE -> stringResource(R.string.assistant_chat_status_idle)
    AssistantUiStatus.STREAMING -> stringResource(R.string.assistant_chat_status_streaming)
    AssistantUiStatus.AWAITING_CONFIRMATION -> stringResource(
        R.string.assistant_chat_status_awaiting_confirmation
    )
    AssistantUiStatus.ERROR -> stringResource(R.string.assistant_chat_status_error)
}

@Composable
private fun AssistantUiError?.toDisplayText(): String = when (this) {
    AssistantUiError.NETWORK -> stringResource(R.string.assistant_chat_error_network)
    AssistantUiError.AUTH -> stringResource(R.string.assistant_chat_error_auth)
    AssistantUiError.RATE_LIMIT -> stringResource(R.string.assistant_chat_error_rate_limit)
    AssistantUiError.TIMEOUT -> stringResource(R.string.assistant_chat_error_timeout)
    AssistantUiError.UPSTREAM_FAILURE -> stringResource(R.string.assistant_chat_error_upstream_failure)
    AssistantUiError.TOOL_FAILED -> stringResource(R.string.assistant_chat_error_tool_failed)
    AssistantUiError.INVALID_TOOL_CALL -> stringResource(R.string.assistant_chat_error_invalid_tool_call)
    AssistantUiError.OUTCOME_UNKNOWN -> stringResource(R.string.assistant_chat_error_outcome_unknown)
    AssistantUiError.CANCELLED -> stringResource(R.string.assistant_chat_error_cancelled)
    AssistantUiError.CONFIRMATION_DEFERRED -> stringResource(R.string.assistant_chat_error_confirmation_deferred)
    AssistantUiError.MAX_ITERATIONS -> stringResource(R.string.assistant_chat_error_max_iterations)
    AssistantUiError.UNKNOWN,
    null -> stringResource(R.string.assistant_chat_error_unknown)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 720)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Large Font", showBackground = true, widthDp = 390, heightDp = 720, fontScale = 1.5f)
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
