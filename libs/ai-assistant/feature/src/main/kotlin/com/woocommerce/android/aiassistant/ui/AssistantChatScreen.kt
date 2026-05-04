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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreview
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreviewField
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardRenderer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun AssistantRoute(
    conversationId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    assistantCardRenderer: AssistantCardRenderer? = null,
    onCardAction: (AssistantCardAction) -> Unit = {},
) {
    val viewModel = hiltViewModel<AssistantViewModel, AssistantViewModel.Factory> { factory ->
        factory.create(conversationId)
    }

    AssistantChatScreen(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier,
        assistantCardRenderer = assistantCardRenderer,
        onCardAction = onCardAction,
    )
}

@Composable
fun AssistantChatScreen(
    viewModel: AssistantViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    assistantCardRenderer: AssistantCardRenderer? = null,
    onCardAction: (AssistantCardAction) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by rememberSaveable { mutableStateOf("") }

    AssistantChatScreen(
        state = state,
        inputText = inputText,
        onInputTextChange = { inputText = it },
        onSendMessage = {
            if (!state.isTurnActive) {
                val message = inputText
                inputText = ""
                viewModel.onSendMessage(message)
            }
        },
        onCancelTurn = viewModel::onCancelTurn,
        onRetry = viewModel::onRetry,
        onConfirmWrite = viewModel::onConfirmWrite,
        onCancelWrite = viewModel::onCancelWrite,
        onBack = onBack,
        modifier = modifier,
        assistantCardRenderer = assistantCardRenderer,
        onCardAction = onCardAction,
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    assistantCardRenderer: AssistantCardRenderer? = null,
    onCardAction: (AssistantCardAction) -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AssistantTopAppBar(
                status = state.status,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            AssistantMessageThread(
                messages = state.messages,
                onRetry = onRetry,
                onConfirmWrite = onConfirmWrite,
                onCancelWrite = onCancelWrite,
                assistantCardRenderer = assistantCardRenderer,
                onCardAction = onCardAction,
                modifier = Modifier.weight(1f),
            )
            AssistantStatusPanel(state = state)
            AssistantComposer(
                inputText = inputText,
                onInputTextChange = onInputTextChange,
                isTurnActive = state.isTurnActive,
                shouldShowStopControl = state.shouldShowStopControl,
                onSendMessage = onSendMessage,
                onCancelTurn = onCancelTurn,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantTopAppBar(
    status: AssistantUiStatus,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.assistant_chat_title),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_assistant_back),
                    contentDescription = stringResource(R.string.assistant_chat_back_content_description),
                )
            }
        },
        actions = {
            AssistantStatusLabel(status = status)
        },
    )
}

@Composable
private fun AssistantStatusLabel(status: AssistantUiStatus) {
    val statusLabel = status.toHeaderText()
    val statusContentDescription = stringResource(
        R.string.assistant_chat_status_content_description,
        statusLabel,
    )
    Surface(
        modifier = Modifier
            .padding(end = 12.dp)
            .widthIn(max = 156.dp)
            .semantics {
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun AssistantMessageThread(
    messages: List<AssistantUiMessage>,
    onRetry: () -> Unit,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardAction: (AssistantCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, messages.lastOrNull()?.segments) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
    ) {
        items(
            items = messages,
            key = { it.id },
        ) { message ->
            AssistantMessageBubble(
                message = message,
                onRetry = onRetry,
                onConfirmWrite = onConfirmWrite,
                onCancelWrite = onCancelWrite,
                assistantCardRenderer = assistantCardRenderer,
                onCardAction = onCardAction,
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(
    message: AssistantUiMessage,
    onRetry: () -> Unit,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardAction: (AssistantCardAction) -> Unit,
) {
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
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistantMessageSegments(
                    message = message,
                    textColor = textColor,
                    isUser = isUser,
                    onConfirmWrite = onConfirmWrite,
                    onCancelWrite = onCancelWrite,
                    assistantCardRenderer = assistantCardRenderer,
                    onCardAction = onCardAction,
                )
                message.error?.let { error ->
                    AssistantInlineError(
                        error = error,
                        onRetry = onRetry,
                    )
                }
                if (message.shouldShowEmptyPlaceholder()) {
                    Text(
                        text = " ",
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantMessageSegments(
    message: AssistantUiMessage,
    textColor: Color,
    isUser: Boolean,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardAction: (AssistantCardAction) -> Unit,
) {
    message.segments.forEach { segment ->
        when (segment) {
            is AssistantUiSegment.ConfirmationCard -> AssistantConfirmationCardSegment(
                confirmation = segment.model,
                onConfirmWrite = onConfirmWrite,
                onCancelWrite = onCancelWrite,
            )
            is AssistantUiSegment.Card -> AssistantCardSegment(
                card = segment.card,
                assistantCardRenderer = assistantCardRenderer,
                onCardAction = onCardAction,
            )
            is AssistantUiSegment.Text -> AssistantMessageTextSegment(
                text = segment.text,
                textColor = textColor,
                isUser = isUser,
            )
        }
    }
}

@Composable
private fun AssistantCardSegment(
    card: AssistantCard,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardAction: (AssistantCardAction) -> Unit,
) {
    if (assistantCardRenderer == null) return

    when (card) {
        is AssistantCard.Order -> assistantCardRenderer.OrderCard(
            card = card,
            onAction = onCardAction,
            modifier = Modifier.fillMaxWidth(),
        )
        is AssistantCard.Product -> assistantCardRenderer.ProductCard(
            card = card,
            onAction = onCardAction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AssistantMessageTextSegment(
    text: String,
    textColor: Color,
    isUser: Boolean,
) {
    if (text.isEmpty()) return

    Text(
        text = text,
        color = textColor,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = if (isUser) TextAlign.End else TextAlign.Start,
    )
}

@Composable
private fun AssistantInlineError(
    error: AssistantMessageError,
    onRetry: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(error.error.toMessageRes()),
            color = MaterialTheme.colorScheme.assistantInlineErrorTextColor(),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (error.canRetry) {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.assistant_chat_retry))
            }
        }
    }
}

private fun AssistantUiMessage.shouldShowEmptyPlaceholder(): Boolean =
    segments.all { it is AssistantUiSegment.Text && it.text.isEmpty() } && error == null

@Composable
private fun AssistantStatusPanel(
    state: AssistantUiState,
) {
    when (state.status) {
        AssistantUiStatus.AWAITING_CONFIRMATION -> Unit
        AssistantUiStatus.ERROR -> {
            if (state.shouldShowFallbackError) {
                AssistantFallbackErrorPanel(error = state.error)
            }
        }
        AssistantUiStatus.IDLE,
        AssistantUiStatus.STREAMING -> Unit
    }
}

@Composable
private fun AssistantFallbackErrorPanel(error: AssistantUiError?) {
    if (error == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(error.toMessageRes()),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AssistantConfirmationCardSegment(
    confirmation: AssistantConfirmationCard,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
) {
    val colors = confirmation.state.confirmationCardColors()
    val shape = RoundedCornerShape(12.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = shape,
        color = colors.container,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(confirmation.state.iconRes()),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.accent,
                )
                Text(
                    text = stringResource(confirmation.state.eyebrowRes()),
                    color = colors.accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = confirmation.preview?.summary
                    ?: stringResource(R.string.assistant_chat_confirm_tool, confirmation.toolCall.name),
                color = colors.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            confirmation.preview?.rows?.forEach { row ->
                ConfirmationDiffRow(
                    row = row,
                    colors = colors,
                )
            }
            if (confirmation.state == AssistantConfirmationCardState.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancelWrite,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.title),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.assistant_chat_cancel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Button(
                        onClick = onConfirmWrite,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.assistant_chat_confirm),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationDiffRow(
    row: RenderedConfirmationPreviewField,
    colors: AssistantConfirmationCardColors,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .border(1.dp, colors.border.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = row.label,
            color = colors.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        row.beforeValue?.let { beforeValue ->
            ConfirmationDiffLine(
                prefix = stringResource(R.string.assistant_confirmation_now),
                value = beforeValue,
                colors = colors,
                strikethrough = true,
            )
        }
        ConfirmationDiffLine(
            prefix = stringResource(R.string.assistant_confirmation_after),
            value = row.afterValue,
            colors = colors,
        )
    }
}

@Composable
private fun ConfirmationDiffLine(
    prefix: String,
    value: String,
    colors: AssistantConfirmationCardColors,
    strikethrough: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = prefix,
            modifier = Modifier.widthIn(min = 40.dp),
            color = colors.label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = if (strikethrough) colors.label else colors.value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (strikethrough) FontWeight.Normal else FontWeight.SemiBold,
            textDecoration = if (strikethrough) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
private fun AssistantConfirmationCardState.confirmationCardColors(): AssistantConfirmationCardColors {
    val colorScheme = MaterialTheme.colorScheme

    return when (this) {
        AssistantConfirmationCardState.PENDING -> AssistantConfirmationCardColors(
            container = colorScheme.surfaceContainerHigh,
            border = colorScheme.primary.copy(alpha = 0.28f),
            accent = colorScheme.primary,
            title = colorScheme.onSurface,
            label = colorScheme.onSurfaceVariant,
            value = colorScheme.onSurface,
        )
        AssistantConfirmationCardState.CONFIRMED -> AssistantConfirmationCardColors(
            container = colorScheme.surfaceContainerHigh,
            border = colorScheme.outlineVariant,
            accent = colorScheme.primary,
            title = colorScheme.onSurface,
            label = colorScheme.onSurfaceVariant,
            value = colorScheme.onSurface,
        )
        AssistantConfirmationCardState.CANCELLED -> AssistantConfirmationCardColors(
            container = colorScheme.surfaceContainerLow,
            border = colorScheme.outlineVariant,
            accent = colorScheme.onSurfaceVariant,
            title = colorScheme.onSurfaceVariant,
            label = colorScheme.onSurfaceVariant,
            value = colorScheme.onSurfaceVariant,
        )
    }
}

private data class AssistantConfirmationCardColors(
    val container: Color,
    val border: Color,
    val accent: Color,
    val title: Color,
    val label: Color,
    val value: Color,
)

@Composable
private fun AssistantComposer(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isTurnActive: Boolean,
    shouldShowStopControl: Boolean,
    onSendMessage: () -> Unit,
    onCancelTurn: () -> Unit,
) {
    val canSend = inputText.isNotBlank() && !isTurnActive
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
                    modifier = Modifier.weight(1f),
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
                if (shouldShowStopControl) {
                    Button(
                        onClick = onCancelTurn,
                        modifier = Modifier.heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
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
                AssistantUiMessage(
                    id = "preview-3",
                    role = AssistantUiMessage.Role.ASSISTANT,
                    segments = listOf(
                        AssistantUiSegment.Text("Here is order #3479."),
                        AssistantUiSegment.Card(sampleOrderCard()),
                        AssistantUiSegment.Text("Here is a matching product."),
                        AssistantUiSegment.Card(sampleProductCard()),
                    ),
                ),
            )
        ),
        inputText = "",
        onInputTextChange = {},
        onSendMessage = {},
        onCancelTurn = {},
        onRetry = {},
        onConfirmWrite = {},
        onCancelWrite = {},
        onBack = {},
        assistantCardRenderer = PreviewAssistantCardRenderer,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 720)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Large Font", showBackground = true, widthDp = 390, heightDp = 720, fontScale = 1.5f)
@Composable
private fun AssistantChatScreenConfirmationPreview() {
    AssistantChatScreen(
        state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("preview-1", AssistantUiMessage.Role.USER, "Mark order 3479 as completed"),
                AssistantUiMessage(
                    id = "preview-2",
                    role = AssistantUiMessage.Role.ASSISTANT,
                    segments = listOf(
                        AssistantUiSegment.Text("I'll mark order #3479 as completed."),
                        AssistantUiSegment.ConfirmationCard(
                            AssistantConfirmationCard(
                                confirmationId = "confirmation-preview",
                                toolCall = ToolCall(
                                    id = "call-preview",
                                    name = "orders_update",
                                    arguments = buildJsonObject { put("id", 3479) },
                                ),
                                state = AssistantConfirmationCardState.PENDING,
                                preview = sampleConfirmationPreview(),
                            )
                        ),
                    ),
                ),
            ),
            status = AssistantUiStatus.AWAITING_CONFIRMATION,
            activeConfirmationId = "confirmation-preview",
        ),
        inputText = "",
        onInputTextChange = {},
        onSendMessage = {},
        onCancelTurn = {},
        onRetry = {},
        onConfirmWrite = {},
        onCancelWrite = {},
        onBack = {},
    )
}

private object PreviewAssistantCardRenderer : AssistantCardRenderer {
    @Composable
    override fun OrderCard(
        card: AssistantCard.Order,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = card.number,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = listOf(card.customerName, card.status, card.unformattedTotal)
                        .filter { it.isNotBlank() }
                        .joinToString(" - "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    @Composable
    override fun ProductCard(
        card: AssistantCard.Product,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = card.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = listOf(card.stockStatus, card.price)
                        .filter { it.isNotBlank() }
                        .joinToString(" - "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun sampleOrderCard() = AssistantCard.Order(
    remoteOrderId = 3479L,
    number = "#3479",
    status = "processing",
    total = "42.00",
    currency = "USD",
    customerName = "Jane Doe",
    date = "2026-05-01T10:00:00Z",
)

private fun sampleProductCard() = AssistantCard.Product(
    remoteProductId = 456L,
    name = "Woo socks",
    sku = "woo-socks",
    price = "9.99",
    stockStatus = "instock",
    status = "publish",
    imageUrl = "",
)

private val AssistantCard.Order.unformattedTotal: String
    get() = total.takeIf { it.isNotBlank() }
        ?.let { listOf(it, currency).filter { value -> value.isNotBlank() }.joinToString(" ") }
        .orEmpty()

private fun sampleConfirmationPreview() = RenderedConfirmationPreview(
    message = "Update order #3479",
    fields = listOf(
        RenderedConfirmationPreviewField(
            name = "status",
            label = "Status",
            beforeValue = "Processing",
            value = "Completed",
        )
    ),
)
