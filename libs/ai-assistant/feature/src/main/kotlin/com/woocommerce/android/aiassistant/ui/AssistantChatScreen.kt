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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.woocommerce.android.aiassistant.ui.components.AssistantComposer
import com.woocommerce.android.aiassistant.ui.components.AssistantConfirmationCardSegment
import com.woocommerce.android.aiassistant.ui.components.AssistantToolActivityPill
import com.woocommerce.android.aiassistant.ui.components.AssistantTypingIndicator
import kotlinx.coroutines.flow.distinctUntilChanged
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
                state = state,
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
            .semantics { contentDescription = statusContentDescription },
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
    state: AssistantUiState,
    onRetry: () -> Unit,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardAction: (AssistantCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleMessages = state.messages.filter { it.hasVisibleContent() }
    val showTypingIndicator = state.shouldShowTypingIndicator
    val listState = rememberLazyListState()
    val bottomPinThresholdPx = with(LocalDensity.current) { BOTTOM_PIN_THRESHOLD_DP.roundToPx() }
    val scrollSignal = visibleMessages.toScrollSignal(showTypingIndicator)
    var previousScrollSignal by remember { mutableStateOf<AssistantThreadScrollSignal?>(null) }
    var wasPinnedBeforeLatestChange by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(listState, bottomPinThresholdPx, scrollSignal.renderedItemCount) {
        snapshotFlow {
            listState.isPinnedToRenderedEnd(
                renderedItemCount = scrollSignal.renderedItemCount,
                bottomPinThresholdPx = bottomPinThresholdPx,
            )
        }
            .distinctUntilChanged()
            .collect { wasPinnedBeforeLatestChange = it }
    }

    LaunchedEffect(scrollSignal) {
        val renderedItemCount = scrollSignal.renderedItemCount
        if (renderedItemCount == 0) {
            previousScrollSignal = scrollSignal
            return@LaunchedEffect
        }

        val previous = previousScrollSignal
        val forceScrollForUserSend = previous != null &&
            previous.lastUserMessageId != scrollSignal.lastUserMessageId
        val isNewAssistantMessage = previous != null &&
            previous.lastMessageId != scrollSignal.lastMessageId &&
            scrollSignal.lastMessageRole == AssistantUiMessage.Role.ASSISTANT

        if (forceScrollForUserSend || wasPinnedBeforeLatestChange) {
            val targetIndex = renderedItemCount - 1
            if (forceScrollForUserSend || isNewAssistantMessage) {
                listState.animateScrollToItem(targetIndex)
            } else {
                listState.scrollToItem(targetIndex)
            }
        }
        previousScrollSignal = scrollSignal
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
    ) {
        items(
            items = visibleMessages,
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
        if (showTypingIndicator) {
            item(key = TYPING_INDICATOR_ITEM_KEY) {
                AssistantTypingIndicator()
            }
        }
    }
}

private const val TYPING_INDICATOR_ITEM_KEY = "assistant-typing-indicator"
private val BOTTOM_PIN_THRESHOLD_DP = 48.dp

private data class AssistantThreadScrollSignal(
    val renderedItemCount: Int,
    val messageCount: Int,
    val lastMessageId: String?,
    val lastMessageRole: AssistantUiMessage.Role?,
    val lastUserMessageId: String?,
    val lastMessageSegmentCount: Int,
    val lastMessageTextLength: Int,
    val showTypingIndicator: Boolean,
)

private fun List<AssistantUiMessage>.toScrollSignal(
    showTypingIndicator: Boolean,
): AssistantThreadScrollSignal {
    val lastMessage = lastOrNull()
    return AssistantThreadScrollSignal(
        renderedItemCount = size + if (showTypingIndicator) 1 else 0,
        messageCount = size,
        lastMessageId = lastMessage?.id,
        lastMessageRole = lastMessage?.role,
        lastUserMessageId = lastOrNull { it.role == AssistantUiMessage.Role.USER }?.id,
        lastMessageSegmentCount = lastMessage?.segments?.size ?: 0,
        lastMessageTextLength = lastMessage?.text?.length ?: 0,
        showTypingIndicator = showTypingIndicator,
    )
}

private fun LazyListState.isPinnedToRenderedEnd(
    renderedItemCount: Int,
    bottomPinThresholdPx: Int,
): Boolean {
    if (renderedItemCount == 0) return true

    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return true
    val targetIndex = renderedItemCount - 1
    if (lastVisibleItem.index != targetIndex) return false

    val distanceFromViewportEnd = layoutInfo.viewportEndOffset - (lastVisibleItem.offset + lastVisibleItem.size)
    return isRenderedTargetPinnedToViewportEnd(
        distanceFromViewportEnd = distanceFromViewportEnd,
        bottomPinThresholdPx = bottomPinThresholdPx,
    )
}

internal fun isRenderedTargetPinnedToViewportEnd(
    distanceFromViewportEnd: Int,
    bottomPinThresholdPx: Int,
): Boolean = distanceFromViewportEnd in 0..bottomPinThresholdPx

private fun AssistantUiMessage.hasVisibleContent(): Boolean =
    role == AssistantUiMessage.Role.USER ||
        error != null ||
        hasVisibleAssistantContent

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
    val rowArrangement = if (isUser) Arrangement.End else Arrangement.Start
    val description = message.contentDescription(isUser = isUser)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        message.segments.forEach { segment ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = rowArrangement,
            ) {
                AssistantMessageSegment(
                    segment = segment,
                    isUser = isUser,
                    onConfirmWrite = onConfirmWrite,
                    onCancelWrite = onCancelWrite,
                    assistantCardRenderer = assistantCardRenderer,
                    onCardAction = onCardAction,
                )
            }
        }
        message.error?.let { error ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = rowArrangement,
            ) {
                AssistantInlineError(error = error, onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun AssistantMessageSegment(
    segment: AssistantUiSegment,
    isUser: Boolean,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardAction: (AssistantCardAction) -> Unit,
) {
    when (segment) {
        is AssistantUiSegment.Text -> {
            if (segment.text.isNotEmpty()) {
                AssistantTextBubble(text = segment.text, isUser = isUser)
            }
        }
        is AssistantUiSegment.CardGroup -> AssistantCardGroupSegment(
            cards = segment.cards,
            assistantCardRenderer = assistantCardRenderer,
            onCardAction = onCardAction,
        )
        is AssistantUiSegment.ToolActivity -> AssistantToolActivityPill(activity = segment.activity)
        is AssistantUiSegment.ConfirmationCard -> AssistantConfirmationCardSegment(
            confirmation = segment.model,
            onConfirmWrite = onConfirmWrite,
            onCancelWrite = onCancelWrite,
        )
    }
}

@Composable
private fun AssistantCardGroupSegment(
    cards: List<AssistantCard>,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardAction: (AssistantCardAction) -> Unit,
) {
    if (assistantCardRenderer == null || cards.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ASSISTANT_CARD_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(cards.groupHeaderRes()),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            cards.forEachIndexed { index, card ->
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
                if (index < cards.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

private val ASSISTANT_CARD_CORNER_RADIUS = 12.dp

private fun List<AssistantCard>.groupHeaderRes(): Int {
    val containsOrders = any { it is AssistantCard.Order }
    val containsProducts = any { it is AssistantCard.Product }
    return when {
        containsOrders && !containsProducts -> R.string.assistant_chat_card_group_orders
        containsProducts && !containsOrders -> R.string.assistant_chat_card_group_products
        else -> R.string.assistant_chat_card_group_generic
    }
}

@Composable
private fun AssistantTextBubble(text: String, isUser: Boolean) {
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

    Box(
        modifier = Modifier
            .then(if (isUser) Modifier.widthIn(max = 360.dp) else Modifier.fillMaxWidth())
            .background(bubbleColor, shape)
            .then(
                if (isUser) {
                    Modifier
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = shape,
                    )
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            modifier = if (isUser) Modifier else Modifier.widthIn(max = 360.dp),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = if (isUser) TextAlign.End else TextAlign.Start,
        )
    }
}

@Composable
private fun AssistantUiMessage.contentDescription(isUser: Boolean): String {
    val messageText = text.ifEmpty { " " }
    val toolActivityLabel = segments
        .filterIsInstance<AssistantUiSegment.ToolActivity>()
        .lastOrNull()
        ?.activity
        ?.let { stringResource(it.labelRes()) }

    return when {
        isUser -> stringResource(R.string.assistant_chat_message_user_content_description, messageText)
        toolActivityLabel != null && text.isEmpty() -> stringResource(
            R.string.assistant_chat_tool_activity_content_description,
            toolActivityLabel,
        )
        else -> stringResource(R.string.assistant_chat_message_assistant_content_description, messageText)
    }
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

@Composable
private fun AssistantStatusPanel(state: AssistantUiState) {
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
@Composable
private fun AssistantChatScreenToolActivityPreview() {
    AssistantChatScreen(
        state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("preview-1", AssistantUiMessage.Role.USER, "Find order 123"),
                AssistantUiMessage(
                    id = "preview-2",
                    role = AssistantUiMessage.Role.ASSISTANT,
                    segments = listOf(
                        AssistantUiSegment.Text(""),
                        AssistantUiSegment.ToolActivity(
                            AssistantToolActivity(
                                toolCallId = "call-preview",
                                toolName = "orders_get",
                            )
                        ),
                    ),
                ),
            ),
            status = AssistantUiStatus.STREAMING,
            activeAssistantMessageId = "preview-2",
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

@Preview(showBackground = true, widthDp = 390, heightDp = 720)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantChatScreenTypingPreview() {
    AssistantChatScreen(
        state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("preview-1", AssistantUiMessage.Role.USER, "Show today's sales"),
                AssistantUiMessage("preview-2", AssistantUiMessage.Role.ASSISTANT, ""),
            ),
            status = AssistantUiStatus.STREAMING,
            activeAssistantMessageId = "preview-2",
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
                        AssistantUiSegment.CardGroup(listOf(sampleOrderCard())),
                        AssistantUiSegment.Text("Here is a matching product."),
                        AssistantUiSegment.CardGroup(listOf(sampleProductCard())),
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
        Column(
            modifier = modifier.padding(14.dp),
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

    @Composable
    override fun ProductCard(
        card: AssistantCard.Product,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        Column(
            modifier = modifier.padding(14.dp),
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
    imageUrl = "https://example.com/socks.png",
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
