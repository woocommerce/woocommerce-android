package com.woocommerce.android.aiassistant.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import com.woocommerce.android.aiassistant.ui.scroll.AssistantScrollController
import com.woocommerce.android.aiassistant.ui.scroll.rememberAssistantScrollController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun AssistantRoute(
    onBack: () -> Unit,
    showEarlyAccessNotice: Boolean,
    onDismissEarlyAccessNotice: () -> Unit,
    onEarlyAccessFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier,
    assistantCardRenderer: AssistantCardRenderer? = null,
    onCardAction: (AssistantCardAction) -> Unit = {},
) {
    val viewModel = hiltViewModel<AssistantViewModel, AssistantViewModel.Factory> { factory ->
        factory.create()
    }

    AssistantChatScreen(
        viewModel = viewModel,
        onBack = onBack,
        showEarlyAccessNotice = showEarlyAccessNotice,
        onDismissEarlyAccessNotice = onDismissEarlyAccessNotice,
        onEarlyAccessFeedbackClick = onEarlyAccessFeedbackClick,
        modifier = modifier,
        assistantCardRenderer = assistantCardRenderer,
        onCardAction = onCardAction,
    )
}

@Composable
fun AssistantChatScreen(
    viewModel: AssistantViewModel,
    onBack: () -> Unit,
    showEarlyAccessNotice: Boolean,
    onDismissEarlyAccessNotice: () -> Unit,
    onEarlyAccessFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier,
    assistantCardRenderer: AssistantCardRenderer? = null,
    onCardAction: (AssistantCardAction) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(viewModel, onCardAction) {
        viewModel.pendingCardNavigation.collect(onCardAction)
    }

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
        showEarlyAccessNotice = showEarlyAccessNotice,
        onDismissEarlyAccessNotice = onDismissEarlyAccessNotice,
        onEarlyAccessFeedbackClick = onEarlyAccessFeedbackClick,
        modifier = modifier,
        assistantCardRenderer = assistantCardRenderer,
        onCardTapped = viewModel::onCardTapped,
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
    showEarlyAccessNotice: Boolean,
    onDismissEarlyAccessNotice: () -> Unit,
    onEarlyAccessFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier,
    assistantCardRenderer: AssistantCardRenderer? = null,
    onCardTapped: (AssistantCard, AssistantCardAction, String) -> Unit = { _, _, _ -> },
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var bottomBarContentHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarContentHeight = with(density) { bottomBarContentHeightPx.toDp() }
    val estimatedBottomBarHeight = FLOATING_COMPOSER_ESTIMATED_HEIGHT * density.fontScale
    val bottomContentPadding = bottomBarContentHeight
        .coerceAtLeast(estimatedBottomBarHeight) + FLOATING_COMPOSER_CONTENT_SPACING
    val visibleMessages = state.messages.filter { message ->
        message.hasVisibleContent(state)
    }
    val showTypingIndicator = state.shouldShowTypingIndicator
    val scrollController = rememberAssistantScrollController(
        state = state,
        visibleMessages = visibleMessages,
        showTypingIndicator = showTypingIndicator,
        bottomContentPadding = bottomContentPadding,
    )
    val submitMessage = {
        scrollController.onUserMessageSubmitted()
        focusManager.clearFocus()
        onSendMessage()
    }
    val submitSuggestion = { prompt: String ->
        scrollController.onUserMessageSubmitted()
        focusManager.clearFocus()
        onSendSuggestion(prompt)
    }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = assistantCanvasColor(),
        topBar = {
            AssistantTopAppBar(
                showRestartAction = state.messages.isNotEmpty(),
                onRestartConversation = onRestartConversation,
                onBack = onBack,
            )
        },
        bottomBar = {
            AssistantFloatingComposerBar(
                state = state,
                inputText = inputText,
                onInputTextChange = onInputTextChange,
                onSendMessage = submitMessage,
                onCancelTurn = onCancelTurn,
                onContentHeightChanged = { bottomBarContentHeightPx = it },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(layoutDirection),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.shouldShowEmptyState) {
                AssistantEmptyState(
                    showEarlyAccessNotice = showEarlyAccessNotice,
                    bottomContentPadding = bottomContentPadding,
                    onFeedbackClick = onEarlyAccessFeedbackClick,
                    onDismissEarlyAccessNotice = onDismissEarlyAccessNotice,
                    onSuggestionClick = submitSuggestion,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                )
            } else {
                AssistantMessageThread(
                    state = state,
                    visibleMessages = visibleMessages,
                    showTypingIndicator = showTypingIndicator,
                    scrollController = scrollController,
                    onRetry = onRetry,
                    onConfirmWrite = onConfirmWrite,
                    onCancelWrite = onCancelWrite,
                    assistantCardRenderer = assistantCardRenderer,
                    onCardTapped = onCardTapped,
                    bottomContentPadding = bottomContentPadding,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                )
            }
            AssistantContentBottomFade(
                bottomBarHeight = bottomBarContentHeight,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
            AssistantJumpToLatestButton(
                visible = scrollController.hasNewerContentBelow.value,
                bottomBarHeight = bottomBarContentHeight,
                onClick = {
                    coroutineScope.launch {
                        scrollController.onJumpToLatestClicked()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun AssistantJumpToLatestButton(
    visible: Boolean,
    bottomBarHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.padding(bottom = bottomBarHeight + 24.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag(ASSISTANT_JUMP_TO_LATEST_TEST_TAG),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_assistant_jump_to_latest),
                    contentDescription = stringResource(
                        R.string.ai_assistant_chat_jump_to_latest_content_description
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun AssistantFloatingComposerBar(
    state: AssistantUiState,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onCancelTurn: () -> Unit,
    onContentHeightChanged: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onContentHeightChanged(it.height) }
            .padding(bottom = FLOATING_COMPOSER_BOTTOM_PADDING),
    ) {
        AssistantStatusPanel(
            state = state,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
        )
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

@Composable
private fun AssistantContentBottomFade(
    bottomBarHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val canvasColor = assistantCanvasColor()
    Box(
        modifier = modifier
            .height(bottomBarHeight + FLOATING_COMPOSER_FADE_EXTRA_HEIGHT)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            FLOATING_COMPOSER_FADE_MIDPOINT_FRACTION to canvasColor.copy(
                                alpha = FLOATING_COMPOSER_FADE_MIDPOINT_ALPHA
                            ),
                            1f to canvasColor,
                        ),
                    )
                )
            },
    )
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
                        text = stringResource(R.string.ai_assistant_chat_title),
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
                        contentDescription = stringResource(R.string.ai_assistant_chat_back_content_description),
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
                                R.string.ai_assistant_chat_restart_content_description
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
    visibleMessages: List<AssistantUiMessage>,
    showTypingIndicator: Boolean,
    scrollController: AssistantScrollController,
    onRetry: () -> Unit,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardTapped: (AssistantCard, AssistantCardAction, String) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = scrollController.listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 18.dp,
            bottom = bottomContentPadding,
        ),
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
                onCardTapped = onCardTapped,
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
private const val FLOATING_COMPOSER_FADE_MIDPOINT_ALPHA = 0.55f
private const val FLOATING_COMPOSER_FADE_MIDPOINT_FRACTION = 0.35f
private val FLOATING_COMPOSER_FADE_EXTRA_HEIGHT = 48.dp
private val FLOATING_COMPOSER_BOTTOM_PADDING = 16.dp
private val FLOATING_COMPOSER_ESTIMATED_HEIGHT = 84.dp
private val FLOATING_COMPOSER_CONTENT_SPACING = 16.dp
private val USER_BUBBLE_MAX_WIDTH = 280.dp
private const val ASSISTANT_JUMP_TO_LATEST_TEST_TAG = "assistant_jump_to_latest"

@Composable
private fun AssistantMessageBubble(
    message: AssistantUiMessage,
    displaySegments: List<AssistantUiSegment>,
    onRetry: () -> Unit,
    onConfirmWrite: () -> Unit,
    onCancelWrite: () -> Unit,
    assistantCardRenderer: AssistantCardRenderer?,
    onCardTapped: (AssistantCard, AssistantCardAction, String) -> Unit,
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
                    onCardTapped = onCardTapped,
                    sourceMessageId = message.id,
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
    onCardTapped: (AssistantCard, AssistantCardAction, String) -> Unit,
    sourceMessageId: String,
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
            onCardTapped = onCardTapped,
            sourceMessageId = sourceMessageId,
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
    onCardTapped: (AssistantCard, AssistantCardAction, String) -> Unit,
    sourceMessageId: String,
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
                onAction = { action -> onCardTapped(card, action, sourceMessageId) },
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
        isUser -> stringResource(R.string.ai_assistant_chat_message_user_content_description, messageText)
        toolActivityLabel != null && text.isEmpty() -> stringResource(
            R.string.ai_assistant_chat_tool_activity_content_description,
            toolActivityLabel,
        )
        else -> stringResource(R.string.ai_assistant_chat_message_assistant_content_description, messageText)
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
                Text(stringResource(R.string.ai_assistant_chat_retry))
            }
        }
    }
}

@Composable
private fun AssistantStatusPanel(
    state: AssistantUiState,
    modifier: Modifier = Modifier,
) {
    when (state.status) {
        AssistantUiStatus.AWAITING_CONFIRMATION -> Unit
        AssistantUiStatus.ERROR -> {
            if (state.shouldShowFallbackError) {
                AssistantFallbackErrorPanel(
                    error = state.error,
                    modifier = modifier,
                )
            }
        }
        AssistantUiStatus.IDLE,
        AssistantUiStatus.STREAMING -> Unit
    }
}

@Composable
private fun AssistantFallbackErrorPanel(
    error: AssistantUiError?,
    modifier: Modifier = Modifier,
) {
    if (error == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 2.dp,
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(error.toMessageRes()),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
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
                onCardTapped = { _, _, _ -> },
                sourceMessageId = "preview-message",
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
                onCardTapped = { _, _, _ -> },
                sourceMessageId = "preview-message",
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
                onCardTapped = { _, _, _ -> },
                sourceMessageId = "preview-message",
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
        showEarlyAccessNotice = true,
        onDismissEarlyAccessNotice = {},
        onEarlyAccessFeedbackClick = {},
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
        showEarlyAccessNotice = false,
        onDismissEarlyAccessNotice = {},
        onEarlyAccessFeedbackClick = {},
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
        showEarlyAccessNotice = false,
        onDismissEarlyAccessNotice = {},
        onEarlyAccessFeedbackClick = {},
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
        showEarlyAccessNotice = false,
        onDismissEarlyAccessNotice = {},
        onEarlyAccessFeedbackClick = {},
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
        showEarlyAccessNotice = false,
        onDismissEarlyAccessNotice = {},
        onEarlyAccessFeedbackClick = {},
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
        showEarlyAccessNotice = false,
        onDismissEarlyAccessNotice = {},
        onEarlyAccessFeedbackClick = {},
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
            is AssistantCard.Variation -> PreviewVariationCard(card, modifier)
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
    private fun PreviewVariationCard(
        card: AssistantCard.Variation,
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
