package com.woocommerce.android.aiassistant.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreview
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreviewField
import com.woocommerce.android.aiassistant.ui.cards.AiAssistantStatsCard
import com.woocommerce.android.aiassistant.ui.cards.AiAssistantStatsCardState
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardChrome
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardRenderer
import com.woocommerce.android.aiassistant.ui.cards.toAssistantCardGroupMetadata
import com.woocommerce.android.aiassistant.ui.components.AssistantComposer
import com.woocommerce.android.aiassistant.ui.components.AssistantConfirmationCardSegment
import com.woocommerce.android.aiassistant.ui.components.AssistantEmptyState
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        },
        onSendSuggestion = { prompt ->
            if (!state.isTurnActive) {
                inputText = ""
                viewModel.onSendMessage(prompt)
            }
        },
        onCancelTurn = viewModel::onCancelTurn,
        onRetry = viewModel::onRetry,
        onConfirmWrite = viewModel::onConfirmWrite,
        onCancelWrite = viewModel::onCancelWrite,
        onRestartConversation = {
            inputText = ""
            viewModel.onRestartConversation()
        },
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
    onSendSuggestion: (String) -> Unit,
    onCancelTurn: () -> Unit,
    onRetry: () -> Unit,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    onRestartConversation: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    assistantCardRenderer: AssistantCardRenderer? = null,
    onCardAction: (AssistantCardAction) -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = assistantCanvasColor(),
        topBar = {
            AssistantTopAppBar(
                showRestartAction = state.messages.isNotEmpty(),
                onRestartConversation = onRestartConversation,
                onBack = onBack,
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.shouldShowEmptyState) {
                AssistantEmptyState(
                    onSuggestionClick = onSendSuggestion,
                    modifier = Modifier.weight(1f),
                )
            } else {
                AssistantMessageThread(
                    state = state,
                    onRetry = onRetry,
                    onConfirmWrite = onConfirmWrite,
                    onCancelWrite = onCancelWrite,
                    assistantCardRenderer = assistantCardRenderer,
                    onCardAction = onCardAction,
                    modifier = Modifier.weight(1f),
                )
            }
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
    showRestartAction: Boolean,
    onRestartConversation: () -> Unit,
    onBack: () -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_assistant_sparkle),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.assistant_chat_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_assistant_back),
                        contentDescription = stringResource(R.string.assistant_chat_back_content_description),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            actions = {
                if (showRestartAction) {
                    IconButton(onClick = onRestartConversation) {
                        Icon(
                            painter = painterResource(R.drawable.ic_assistant_new_chat),
                            contentDescription = stringResource(
                                R.string.assistant_chat_restart_content_description
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = assistantCanvasColor(),
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            windowInsets = WindowInsets(),
        )
        HorizontalDivider(color = assistantOutlineColor().copy(alpha = 0.6f))
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
    val visibleMessages = state.messages.filter { message ->
        message.hasVisibleContent(state)
    }
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
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
    ) {
        items(
            items = visibleMessages,
            key = { it.id },
        ) { message ->
            AssistantMessageBubble(
                message = message,
                displaySegments = message.orderedSegments(state),
                onRetry = onRetry,
                onConfirmWrite = onConfirmWrite,
                onCancelWrite = onCancelWrite,
                assistantCardRenderer = assistantCardRenderer,
                onCardAction = onCardAction,
                modifier = Modifier.animateItem(),
            )
        }
        if (showTypingIndicator) {
            item(key = TYPING_INDICATOR_ITEM_KEY) {
                AssistantRevealOnFirstComposition(
                    modifier = Modifier.animateItem(),
                ) {
                    AssistantTypingIndicator()
                }
            }
        }
    }
}

private const val TYPING_INDICATOR_ITEM_KEY = "assistant-typing-indicator"
private const val REVEAL_ANIMATION_DURATION_MS = 180
private const val REVEAL_SLIDE_OFFSET_DIVISOR = 3
private val BOTTOM_PIN_THRESHOLD_DP = 48.dp
private val USER_BUBBLE_MAX_WIDTH = 280.dp

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

@Composable
private fun AssistantMessageBubble(
    message: AssistantUiMessage,
    displaySegments: List<AssistantUiSegment>,
    onRetry: () -> Unit,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardAction: (AssistantCardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == AssistantUiMessage.Role.USER
    val rowArrangement = if (isUser) Arrangement.End else Arrangement.Start
    val description = message.contentDescription(isUser = isUser)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        displaySegments.forEach { segment ->
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
        is AssistantUiSegment.ToolActivity -> AssistantRevealOnFirstComposition {
            AssistantToolActivityPill(activity = segment.activity)
        }
        is AssistantUiSegment.ConfirmationCard -> AssistantConfirmationCardSegment(
            confirmation = segment.model,
            onConfirmWrite = onConfirmWrite,
            onCancelWrite = onCancelWrite,
        )
    }
}

@Composable
private fun AssistantRevealOnFirstComposition(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(REVEAL_ANIMATION_DURATION_MS)) +
            slideInVertically(animationSpec = tween(REVEAL_ANIMATION_DURATION_MS)) { fullHeight ->
                fullHeight / REVEAL_SLIDE_OFFSET_DIVISOR
            },
    ) {
        content()
    }
}

@Composable
private fun AssistantCardGroupSegment(
    cards: List<AssistantCard>,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardAction: (AssistantCardAction) -> Unit,
) {
    if (assistantCardRenderer == null || cards.isEmpty()) return
    val metadata = cards.toAssistantCardGroupMetadata()

    AssistantCardChrome(
        title = stringResource(metadata.titleRes),
        leadingIconRes = metadata.iconRes,
        modifier = Modifier.fillMaxWidth(),
    ) {
        cards.forEachIndexed { index, card ->
            assistantCardRenderer.Card(
                card = card,
                onAction = onCardAction,
                modifier = Modifier.fillMaxWidth(),
            )
            if (index < cards.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = assistantOutlineColor().copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun AssistantTextBubble(text: String, isUser: Boolean) {
    if (!isUser) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    Box(
        modifier = Modifier
            .widthIn(max = USER_BUBBLE_MAX_WIDTH)
            .background(assistantUserBubbleColor(), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            color = assistantUserBubbleContentColor(),
            style = MaterialTheme.typography.bodyMedium,
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

@Preview(showBackground = true, widthDp = 390, heightDp = 180)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 180, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantTextBubblePreview() {
    Surface(color = assistantCanvasColor()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                AssistantTextBubble(
                    text = "Show orders that need attention",
                    isUser = true,
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                AssistantTextBubble(
                    text = "I found a few processing orders with recent customer notes.",
                    isUser = false,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 260)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 260, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantCardGroupSegmentPreview() {
    Surface(color = assistantCanvasColor()) {
        Column(modifier = Modifier.padding(16.dp)) {
            AssistantCardGroupSegment(
                cards = listOf(sampleOrderCard(), sampleProductCard(), sampleStatsCard()),
                assistantCardRenderer = PreviewAssistantCardRenderer,
                onCardAction = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 380)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 380, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantStatsCardGroupSegmentPreview() {
    Surface(color = assistantCanvasColor()) {
        Column(modifier = Modifier.padding(16.dp)) {
            AssistantCardGroupSegment(
                cards = listOf(sampleStatsCard()),
                assistantCardRenderer = PreviewAssistantCardRenderer,
                onCardAction = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 380)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 380, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantStatsCardGroupNoTrendPreview() {
    Surface(color = assistantCanvasColor()) {
        Column(modifier = Modifier.padding(16.dp)) {
            AssistantCardGroupSegment(
                cards = listOf(
                    sampleStatsCard(
                        totalSalesChartValues = emptyList(),
                        netSalesChartValues = emptyList(),
                    )
                ),
                assistantCardRenderer = PreviewAssistantCardRenderer,
                onCardAction = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 720)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Large Font", showBackground = true, widthDp = 390, heightDp = 720, fontScale = 1.5f)
@Composable
private fun AssistantChatScreenEmptyStatePreview() {
    AssistantChatScreen(
        state = AssistantUiState(),
        inputText = "",
        onInputTextChange = {},
        onSendMessage = {},
        onSendSuggestion = {},
        onCancelTurn = {},
        onRetry = {},
        onConfirmWrite = {},
        onCancelWrite = {},
        onRestartConversation = {},
        onBack = {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 720)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantChatScreenStreamingMultipleToolActivityPreview() {
    AssistantChatScreen(
        state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("preview-1", AssistantUiMessage.Role.USER, "Show revenue and latest orders"),
                AssistantUiMessage(
                    id = "preview-2",
                    role = AssistantUiMessage.Role.ASSISTANT,
                    segments = listOf(
                        AssistantUiSegment.Text("I checked this week's revenue and am loading your latest orders."),
                        AssistantUiSegment.ToolActivity(
                            AssistantToolActivity(
                                toolCallId = "call-revenue",
                                toolName = "analytics_orders",
                                status = AssistantToolActivity.Status.COMPLETED,
                            )
                        ),
                        AssistantUiSegment.ToolActivity(
                            AssistantToolActivity(
                                toolCallId = "call-customer",
                                toolName = "customers_list",
                                status = AssistantToolActivity.Status.COMPLETED,
                            )
                        ),
                        AssistantUiSegment.ToolActivity(
                            AssistantToolActivity(
                                toolCallId = "call-orders",
                                toolName = "orders_list",
                                status = AssistantToolActivity.Status.RUNNING,
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
        onSendSuggestion = {},
        onCancelTurn = {},
        onRetry = {},
        onConfirmWrite = {},
        onCancelWrite = {},
        onRestartConversation = {},
        onBack = {},
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 720)
@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AssistantChatScreenFinishedMultipleToolActivityPreview() {
    AssistantChatScreen(
        state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("preview-1", AssistantUiMessage.Role.USER, "Show revenue and latest orders"),
                AssistantUiMessage(
                    id = "preview-2",
                    role = AssistantUiMessage.Role.ASSISTANT,
                    segments = listOf(
                        AssistantUiSegment.ToolActivity(
                            AssistantToolActivity(
                                toolCallId = "call-revenue",
                                toolName = "analytics_orders",
                                status = AssistantToolActivity.Status.COMPLETED,
                            )
                        ),
                        AssistantUiSegment.Text("Here are this week's sales and your most recent order."),
                        AssistantUiSegment.ToolActivity(
                            AssistantToolActivity(
                                toolCallId = "call-orders",
                                toolName = "orders_list",
                                status = AssistantToolActivity.Status.COMPLETED,
                            )
                        ),
                    ),
                ),
            ),
            status = AssistantUiStatus.IDLE,
        ),
        inputText = "",
        onInputTextChange = {},
        onSendMessage = {},
        onSendSuggestion = {},
        onCancelTurn = {},
        onRetry = {},
        onConfirmWrite = {},
        onCancelWrite = {},
        onRestartConversation = {},
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
        onSendSuggestion = {},
        onCancelTurn = {},
        onRetry = {},
        onConfirmWrite = {},
        onCancelWrite = {},
        onRestartConversation = {},
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
                        AssistantUiSegment.Text("Here are this week's sales."),
                        AssistantUiSegment.CardGroup(listOf(sampleStatsCard())),
                    ),
                ),
            )
        ),
        inputText = "",
        onInputTextChange = {},
        onSendMessage = {},
        onSendSuggestion = {},
        onCancelTurn = {},
        onRetry = {},
        onConfirmWrite = {},
        onCancelWrite = {},
        onRestartConversation = {},
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
        onSendSuggestion = {},
        onCancelTurn = {},
        onRetry = {},
        onConfirmWrite = {},
        onCancelWrite = {},
        onRestartConversation = {},
        onBack = {},
    )
}

private object PreviewAssistantCardRenderer : AssistantCardRenderer {
    @Composable
    override fun Card(
        card: AssistantCard,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        when (card) {
            is AssistantCard.Order -> PreviewOrderCard(card, modifier)
            is AssistantCard.Product -> PreviewProductCard(card, modifier)
            is AssistantCard.Customer -> PreviewCustomerCard(card, modifier)
            is AssistantCard.Stats -> PreviewStatsCard(card, modifier)
        }
    }

    @Composable
    private fun PreviewOrderCard(
        card: AssistantCard.Order,
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
    private fun PreviewCustomerCard(
        card: AssistantCard.Customer,
        modifier: Modifier,
    ) {
        Column(
            modifier = modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = card.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = card.email,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    @Composable
    private fun PreviewProductCard(
        card: AssistantCard.Product,
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

    @Composable
    private fun PreviewStatsCard(
        card: AssistantCard.Stats,
        modifier: Modifier,
    ) {
        AiAssistantStatsCard(
            state = AiAssistantStatsCardState(
                period = "${card.after} - ${card.before}",
                metrics = card.metrics.map { metric ->
                    AiAssistantStatsCardState.Metric(
                        type = metric.type,
                        value = listOf(metric.value, card.currency)
                            .filter { it.isNotBlank() }
                            .joinToString(" "),
                        chartValues = metric.chartPoints.map { it.value },
                    )
                },
            ),
            onClick = {},
            modifier = modifier,
        )
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

private fun sampleStatsCard(
    totalSalesChartValues: List<AssistantCard.Stats.ChartPoint> = SAMPLE_TOTAL_SALES_CHART_POINTS,
    netSalesChartValues: List<AssistantCard.Stats.ChartPoint> = SAMPLE_NET_SALES_CHART_POINTS,
    totalOrdersChartValues: List<AssistantCard.Stats.ChartPoint> = SAMPLE_TOTAL_ORDERS_CHART_POINTS,
    averageOrderValueChartValues: List<AssistantCard.Stats.ChartPoint> = SAMPLE_AVERAGE_ORDER_VALUE_CHART_POINTS,
) = AssistantCard.Stats(
    id = "analytics_orders:after:2026-05-01:before:2026-05-07:interval:day",
    kind = AssistantCard.Stats.Kind.Orders,
    after = "2026-05-01",
    before = "2026-05-07",
    currency = "USD",
    metrics = listOf(
        AssistantCard.Stats.Metric(
            type = AssistantCard.Stats.MetricType.TotalSales,
            value = "170.35",
            chartPoints = totalSalesChartValues,
        ),
        AssistantCard.Stats.Metric(
            type = AssistantCard.Stats.MetricType.NetSales,
            value = "120.15",
            chartPoints = netSalesChartValues,
        ),
        AssistantCard.Stats.Metric(
            type = AssistantCard.Stats.MetricType.TotalOrders,
            value = "42",
            chartPoints = totalOrdersChartValues,
        ),
        AssistantCard.Stats.Metric(
            type = AssistantCard.Stats.MetricType.AverageOrderValue,
            value = "85.30",
            chartPoints = averageOrderValueChartValues,
        ),
    ),
)

private val SAMPLE_TOTAL_SALES_CHART_POINTS = listOf(
    AssistantCard.Stats.ChartPoint("2026-05-01", 12.0),
    AssistantCard.Stats.ChartPoint("2026-05-02", 18.0),
    AssistantCard.Stats.ChartPoint("2026-05-03", 9.0),
)

private val SAMPLE_NET_SALES_CHART_POINTS = listOf(
    AssistantCard.Stats.ChartPoint("2026-05-01", 8.0),
    AssistantCard.Stats.ChartPoint("2026-05-02", 12.0),
    AssistantCard.Stats.ChartPoint("2026-05-03", 6.0),
)

private val SAMPLE_TOTAL_ORDERS_CHART_POINTS = listOf(
    AssistantCard.Stats.ChartPoint("2026-05-01", 12.0),
    AssistantCard.Stats.ChartPoint("2026-05-02", 16.0),
    AssistantCard.Stats.ChartPoint("2026-05-03", 14.0),
)

private val SAMPLE_AVERAGE_ORDER_VALUE_CHART_POINTS = listOf(
    AssistantCard.Stats.ChartPoint("2026-05-01", 80.10),
    AssistantCard.Stats.ChartPoint("2026-05-02", 82.25),
    AssistantCard.Stats.ChartPoint("2026-05-03", 93.55),
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
