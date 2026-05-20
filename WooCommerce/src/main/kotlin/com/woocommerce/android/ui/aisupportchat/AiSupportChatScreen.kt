package com.woocommerce.android.ui.aisupportchat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticResult
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticStatus
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest
import com.woocommerce.android.ui.aisupportchat.diagnostics.SuggestedFixAction
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportIssueType
import com.woocommerce.android.ui.aisupportchat.diagnostics.TestStatus
import com.woocommerce.android.ui.compose.DialogState
import com.woocommerce.android.ui.compose.Render
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.troubleshooting.FailureType
import com.woocommerce.android.ui.troubleshooting.useCases.StoreAnalyticsCheckUseCase
import com.woocommerce.commons.ui.markdown.MarkdownText

@Composable
fun AiSupportChatScreen(
    viewModel: AiSupportChatViewModel,
    onContactSupportClicked: (HumanSupportContactSource) -> Unit
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    AiSupportChatScreen(
        viewState = viewState,
        onInputChanged = viewModel::onInputChanged,
        onSendClicked = viewModel::onSendClicked,
        onIssueSelected = viewModel::onIssueSelected,
        onContinueAfterDiagnosticsClicked = viewModel::onContinueAfterDiagnosticsClicked,
        onSuggestedFixActionClicked = viewModel::onSuggestedFixActionClicked,
        onContactSupportClicked = { onContactSupportClicked(HumanSupportContactSource.BANNER) },
        onContactSupportFromErrorClicked = { onContactSupportClicked(HumanSupportContactSource.ERROR_DIALOG) },
        onSendErrorDismissed = viewModel::onSendErrorDismissed,
        onRetryLoadHistoryClicked = viewModel::onRetryLoadHistoryClicked,
        onMarkResolvedConfirmed = viewModel::onMarkResolvedConfirmed,
        onMarkResolvedDismissed = viewModel::onMarkResolvedDismissed,
        onFeedbackClicked = viewModel::onFeedbackClicked
    )
}

@Composable
fun AiSupportChatScreen(
    viewState: AiSupportChatViewState,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onIssueSelected: (SupportIssueType, String) -> Unit,
    onContinueAfterDiagnosticsClicked: () -> Unit,
    onSuggestedFixActionClicked: (SuggestedFixAction) -> Unit,
    onContactSupportClicked: () -> Unit,
    onContactSupportFromErrorClicked: () -> Unit,
    onSendErrorDismissed: () -> Unit,
    onRetryLoadHistoryClicked: () -> Unit,
    onMarkResolvedConfirmed: () -> Unit,
    onMarkResolvedDismissed: () -> Unit,
    onFeedbackClicked: (Long, AiSupportChatFeedbackRating) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        MessageList(
            messages = viewState.messages,
            messageRatings = viewState.messageRatings,
            isSending = viewState.isSending,
            isLoadingHistory = viewState.isLoadingHistory,
            showDiagnosticActions = viewState.showDiagnosticActions,
            onIssueSelected = onIssueSelected,
            onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked,
            onSuggestedFixActionClicked = onSuggestedFixActionClicked,
            onFeedbackClicked = onFeedbackClicked,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        when {
            viewState.hasCreatedTicket -> TicketCreatedBanner(modifier = Modifier.fillMaxWidth())
            viewState.isChatResolved -> ChatResolvedBanner(modifier = Modifier.fillMaxWidth())
            viewState.showHumanSupportPrompt -> HumanSupportBanner(
                onContactSupportClicked = onContactSupportClicked,
                modifier = Modifier.fillMaxWidth()
            )
            viewState.showInputBar -> InputBar(
                input = viewState.input,
                isSending = viewState.isSending,
                enabled = viewState.canSendMessages,
                onInputChanged = onInputChanged,
                onSendClicked = onSendClicked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (viewState.showSendError) {
        SendErrorDialog(
            onContactSupportClicked = onContactSupportFromErrorClicked,
            onDismiss = onSendErrorDismissed
        )
    }

    if (viewState.showLoadHistoryError) {
        LoadHistoryErrorDialog(onRetry = onRetryLoadHistoryClicked)
    }

    if (viewState.showMarkResolvedConfirmation) {
        MarkResolvedConfirmationDialog(
            onConfirm = onMarkResolvedConfirmed,
            onDismiss = onMarkResolvedDismissed
        )
    }
}

@Composable
private fun MessageList(
    messages: List<AiSupportChatMessage>,
    messageRatings: Map<Long, AiSupportChatFeedbackRating>,
    isSending: Boolean,
    isLoadingHistory: Boolean,
    showDiagnosticActions: Boolean,
    onIssueSelected: (SupportIssueType, String) -> Unit,
    onContinueAfterDiagnosticsClicked: () -> Unit,
    onSuggestedFixActionClicked: (SuggestedFixAction) -> Unit,
    onFeedbackClicked: (Long, AiSupportChatFeedbackRating) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val itemCount = messages.size + (if (isSending) 1 else 0)

    if (isLoadingHistory) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(messages.size, isSending) {
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.major_100),
            vertical = dimensionResource(R.dimen.major_100)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.minor_100))
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                feedbackRating = message.messageId?.let { messageRatings[it] },
                showDiagnosticActions = showDiagnosticActions,
                onIssueSelected = onIssueSelected,
                onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked,
                onSuggestedFixActionClicked = onSuggestedFixActionClicked,
                onFeedbackClicked = onFeedbackClicked
            )
        }

        if (isSending) {
            item(key = "typing") {
                TypingIndicator()
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: AiSupportChatMessage,
    feedbackRating: AiSupportChatFeedbackRating?,
    showDiagnosticActions: Boolean,
    onIssueSelected: (SupportIssueType, String) -> Unit,
    onContinueAfterDiagnosticsClicked: () -> Unit,
    onSuggestedFixActionClicked: (SuggestedFixAction) -> Unit,
    onFeedbackClicked: (Long, AiSupportChatFeedbackRating) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == AiSupportChatMessageRole.USER
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth * MAX_BUBBLE_WIDTH_FRACTION)
                .align(if (isUser) Alignment.CenterEnd else Alignment.CenterStart)
        ) {
            Surface(
                shape = RoundedCornerShape(MESSAGE_BUBBLE_CORNER_RADIUS),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = if (isUser) 0.dp else 1.dp
            ) {
                MessageContent(
                    content = message.content,
                    textColor = textColor,
                    shouldFormatMarkdown = message.role == AiSupportChatMessageRole.BOT,
                    showDiagnosticActions = showDiagnosticActions,
                    onIssueSelected = onIssueSelected,
                    onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked,
                    onSuggestedFixActionClicked = onSuggestedFixActionClicked
                )
            }
            if (message.canShowFeedback()) {
                if (feedbackRating == null) {
                    MessageFeedbackActions(
                        messageId = requireNotNull(message.messageId),
                        onFeedbackClicked = onFeedbackClicked,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.minor_100))
                    )
                } else {
                    MessageFeedback(
                        rating = feedbackRating,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.minor_100))
                    )
                }
            }
        }
    }
}

private fun AiSupportChatMessage.canShowFeedback(): Boolean =
    role == AiSupportChatMessageRole.BOT &&
        messageId != null &&
        !isResolved &&
        content is AiSupportChatMessageContent.Text

@Composable
private fun MessageFeedbackActions(
    messageId: Long,
    onFeedbackClicked: (Long, AiSupportChatFeedbackRating) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onFeedbackClicked(messageId, AiSupportChatFeedbackRating.UP) },
            modifier = Modifier.size(FEEDBACK_BUTTON_SIZE)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_thumb_up),
                contentDescription = stringResource(R.string.ai_feedback_form_positive_button),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = { onFeedbackClicked(messageId, AiSupportChatFeedbackRating.DOWN) },
            modifier = Modifier.size(FEEDBACK_BUTTON_SIZE)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_thumb_down),
                contentDescription = stringResource(R.string.ai_feedback_form_negative_button),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageFeedback(
    rating: AiSupportChatFeedbackRating,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (rating) {
            AiSupportChatFeedbackRating.UP -> R.drawable.ic_thumb_up_filled_24dp
            AiSupportChatFeedbackRating.DOWN -> R.drawable.ic_thumb_down_filled_24dp
        }
        val text = when (rating) {
            AiSupportChatFeedbackRating.UP -> stringResource(R.string.ai_support_chat_feedback_helpful)
            AiSupportChatFeedbackRating.DOWN -> stringResource(R.string.ai_support_chat_feedback_not_helpful)
        }
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(RATED_FEEDBACK_ICON_SIZE)
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.minor_50)))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun MessageContent(
    content: AiSupportChatMessageContent,
    textColor: Color,
    shouldFormatMarkdown: Boolean,
    showDiagnosticActions: Boolean,
    onIssueSelected: (SupportIssueType, String) -> Unit,
    onContinueAfterDiagnosticsClicked: () -> Unit,
    onSuggestedFixActionClicked: (SuggestedFixAction) -> Unit
) {
    when (content) {
        AiSupportChatMessageContent.Greeting -> TextContent(
            text = stringResource(R.string.ai_support_chat_greeting),
            color = textColor,
            shouldFormatMarkdown = false
        )
        AiSupportChatMessageContent.IssuePicker -> IssuePickerContent(
            textColor = textColor,
            onIssueSelected = onIssueSelected
        )
        AiSupportChatMessageContent.PostDiagnosticsGreeting -> TextContent(
            text = stringResource(R.string.ai_support_chat_post_diagnostics_greeting),
            color = textColor,
            shouldFormatMarkdown = false
        )
        AiSupportChatMessageContent.ResolvedPrompt -> TextContent(
            text = stringResource(R.string.ai_support_chat_resolved_prompt),
            color = textColor,
            shouldFormatMarkdown = false
        )
        is AiSupportChatMessageContent.Text -> TextContent(
            text = content.text,
            color = textColor,
            shouldFormatMarkdown = shouldFormatMarkdown
        )
        is AiSupportChatMessageContent.DiagnosticsProgress -> DiagnosticsContent(
            result = content.result,
            textColor = textColor,
            showDiagnosticActions = showDiagnosticActions,
            onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked,
            onSuggestedFixActionClicked = onSuggestedFixActionClicked
        )
        is AiSupportChatMessageContent.DiagnosticsFailure -> DiagnosticsContent(
            result = content.result,
            textColor = textColor,
            showDiagnosticActions = showDiagnosticActions,
            onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked,
            onSuggestedFixActionClicked = onSuggestedFixActionClicked
        )
    }
}

@Composable
private fun TextContent(
    text: String,
    color: Color,
    shouldFormatMarkdown: Boolean
) {
    val modifier = Modifier.padding(
        horizontal = dimensionResource(R.dimen.major_100),
        vertical = dimensionResource(R.dimen.minor_100)
    )
    if (shouldFormatMarkdown) {
        MarkdownText(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            linkColor = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    } else {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
    }
}

@Composable
private fun IssuePickerContent(
    textColor: Color,
    onIssueSelected: (SupportIssueType, String) -> Unit
) {
    Column(
        modifier = Modifier.padding(dimensionResource(R.dimen.major_100)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.minor_100))
    ) {
        Text(
            text = stringResource(R.string.ai_support_chat_issue_picker_title),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
        SupportIssueType.selectableEntries.forEach { issueType ->
            val issueLabel = stringResource(issueType.displayLabel)
            WCOutlinedButton(
                onClick = { onIssueSelected(issueType, issueLabel) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = issueLabel)
            }
        }
    }
}

@Composable
private fun DiagnosticsContent(
    result: DiagnosticResult,
    textColor: Color,
    showDiagnosticActions: Boolean,
    onContinueAfterDiagnosticsClicked: () -> Unit,
    onSuggestedFixActionClicked: (SuggestedFixAction) -> Unit
) {
    val hasFailure = result.firstFailure != null

    Column(
        modifier = Modifier.padding(dimensionResource(R.dimen.major_100)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.minor_100))
    ) {
        Text(
            text = if (result.firstFailure == null && result.isComplete) {
                stringResource(R.string.ai_support_chat_diagnostics_success)
            } else {
                stringResource(R.string.ai_support_chat_diagnostics_title)
            },
            color = textColor,
            style = MaterialTheme.typography.titleSmall
        )

        result.statuses.forEach { status ->
            DiagnosticStatusRow(status = status, textColor = textColor)
        }

        if (showDiagnosticActions) {
            if (hasFailure) {
                Text(
                    text = result.failureMessage(),
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            DiagnosticActions(
                suggestedAction = result.suggestedAction,
                onSuggestedFixActionClicked = onSuggestedFixActionClicked,
                onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked
            )
        }
    }
}

@Composable
private fun DiagnosticActions(
    suggestedAction: SuggestedFixAction?,
    onSuggestedFixActionClicked: (SuggestedFixAction) -> Unit,
    onContinueAfterDiagnosticsClicked: () -> Unit
) {
    BoxWithConstraints {
        val spacing = dimensionResource(R.dimen.minor_100)
        val useVerticalActions = maxWidth < MIN_HORIZONTAL_DIAGNOSTIC_ACTIONS_WIDTH

        if (useVerticalActions) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                suggestedAction?.let { action ->
                    SuggestedFixActionButton(
                        action = action,
                        onSuggestedFixActionClicked = onSuggestedFixActionClicked,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ContinueAfterDiagnosticsButton(
                    onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                suggestedAction?.let { action ->
                    SuggestedFixActionButton(
                        action = action,
                        onSuggestedFixActionClicked = onSuggestedFixActionClicked,
                        modifier = Modifier.weight(1f)
                    )
                }
                ContinueAfterDiagnosticsButton(
                    onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SuggestedFixActionButton(
    action: SuggestedFixAction,
    onSuggestedFixActionClicked: (SuggestedFixAction) -> Unit,
    modifier: Modifier = Modifier
) {
    WCOutlinedButton(
        onClick = { onSuggestedFixActionClicked(action) },
        modifier = modifier
    ) {
        Text(text = action.title())
    }
}

@Composable
private fun ContinueAfterDiagnosticsButton(
    onContinueAfterDiagnosticsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WCColoredButton(
        onClick = onContinueAfterDiagnosticsClicked,
        text = stringResource(R.string.ai_support_chat_diagnostics_continue),
        modifier = modifier
    )
}

@Composable
private fun DiagnosticStatusRow(status: DiagnosticStatus, textColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = status.test.title(),
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (status.status is TestStatus.Running) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .padding(end = dimensionResource(R.dimen.minor_100))
                        .size(14.dp)
                )
            }
            Text(
                text = status.status.title(),
                color = textColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DiagnosticTest.title(): String =
    stringResource(
        when (this) {
            DiagnosticTest.INTERNET_CONNECTION -> R.string.orderlist_connectivity_tool_internet_check_title
            DiagnosticTest.WPCOM_SERVERS -> R.string.orderlist_connectivity_tool_wordpress_check_title
            DiagnosticTest.STORE_CONNECTION -> R.string.orderlist_connectivity_tool_store_check_title
            DiagnosticTest.STORE_ORDERS -> R.string.orderlist_connectivity_tool_store_orders_check_title
            DiagnosticTest.STORE_PRODUCTS -> R.string.orderlist_connectivity_tool_store_products_check_title
            DiagnosticTest.ANALYTICS_SETTING -> R.string.ai_support_chat_diagnostics_analytics_setting_title
        }
    )

@Composable
private fun SuggestedFixAction.title(): String =
    stringResource(
        when (this) {
            SuggestedFixAction.EnableAnalytics -> R.string.ai_support_chat_diagnostics_enable_analytics
        }
    )

@Composable
private fun DiagnosticResult.failureMessage(): String {
    val failedTest = firstFailure ?: return ""
    val failedStatus = failedTest.status as? TestStatus.Failed ?: return ""

    return when {
        failedTest.test == DiagnosticTest.INTERNET_CONNECTION ->
            stringResource(R.string.orderlist_connectivity_tool_internet_check_suggestion)
        failedTest.test == DiagnosticTest.WPCOM_SERVERS ->
            stringResource(R.string.ai_support_chat_diagnostics_wpcom_connection_failure)
        failedTest.test == DiagnosticTest.ANALYTICS_SETTING &&
            failedStatus.technicalDetails
                ?.contains(StoreAnalyticsCheckUseCase.PLUGIN_NOT_ACTIVE_ERROR_TYPE) == true ->
            stringResource(R.string.ai_support_chat_diagnostics_analytics_disabled_failure)
        failedTest.test == DiagnosticTest.ANALYTICS_SETTING ->
            stringResource(R.string.ai_support_chat_diagnostics_analytics_check_failure)
        else -> failedStatus.failureMessage()
    }
}

@Composable
private fun TestStatus.Failed.failureMessage(): String =
    stringResource(
        when (failureType) {
            FailureType.TIMEOUT -> R.string.orderlist_connectivity_tool_timeout_error_suggestion
            FailureType.PARSE -> R.string.ai_support_chat_diagnostics_parse_failure
            FailureType.JETPACK -> R.string.ai_support_chat_diagnostics_jetpack_failure
            FailureType.GENERIC, null -> R.string.orderlist_connectivity_tool_generic_error_suggestion
        }
    )

@Composable
private fun TestStatus.title(): String =
    stringResource(
        when (this) {
            TestStatus.Pending -> R.string.ai_support_chat_diagnostics_status_pending
            TestStatus.Running -> R.string.ai_support_chat_diagnostics_status_running
            TestStatus.Passed -> R.string.ai_support_chat_diagnostics_status_passed
            is TestStatus.Failed -> R.string.ai_support_chat_diagnostics_status_failed
        }
    )

@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    val typingDescription = stringResource(R.string.ai_support_chat_typing)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(MESSAGE_BUBBLE_CORNER_RADIUS),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = maxWidth * MAX_BUBBLE_WIDTH_FRACTION)
                .align(Alignment.CenterStart)
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = typingDescription
                }
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.major_100),
                    vertical = dimensionResource(R.dimen.minor_100)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(TYPING_INDICATOR_SIZE)
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.minor_100)))
                AnimatedTypingText()
            }
        }
    }
}

@Composable
private fun AnimatedTypingText() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typingAlpha"
    )
    Text(
        text = stringResource(R.string.ai_support_chat_typing),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.alpha(alpha)
    )
}

@Composable
private fun TicketCreatedBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = stringResource(R.string.ai_support_chat_ticket_created_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
    }
}

@Composable
private fun ChatResolvedBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = stringResource(R.string.ai_support_chat_resolved_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
    }
}

@Composable
private fun HumanSupportBanner(
    onContactSupportClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(dimensionResource(R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.minor_100))
    ) {
        Text(
            text = stringResource(R.string.ai_support_chat_human_support_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        WCOutlinedButton(onClick = onContactSupportClicked) {
            Text(text = stringResource(R.string.ai_support_chat_contact_support))
        }
    }
}

@Composable
private fun SendErrorDialog(
    onContactSupportClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    DialogState(
        title = R.string.ai_support_chat_error_title,
        message = R.string.ai_support_chat_send_error,
        positiveButton = DialogState.DialogButton(
            text = R.string.ai_support_chat_contact_support,
            onClick = onContactSupportClicked
        ),
        negativeButton = DialogState.DialogButton(
            text = R.string.ai_support_chat_error_dismiss,
            onClick = onDismiss
        ),
        isCancelable = false,
        onDismiss = onDismiss
    ).Render()
}

@Composable
private fun LoadHistoryErrorDialog(onRetry: () -> Unit) {
    DialogState(
        title = R.string.ai_support_chat_error_title,
        message = R.string.ai_support_chat_load_history_error,
        positiveButton = DialogState.DialogButton(
            text = R.string.retry,
            onClick = onRetry
        ),
        isCancelable = false
    ).Render()
}

@Composable
private fun MarkResolvedConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DialogState(
        title = R.string.ai_support_chat_mark_resolved_confirmation_title,
        message = R.string.ai_support_chat_mark_resolved_confirmation_message,
        positiveButton = DialogState.DialogButton(
            text = R.string.ai_support_chat_mark_resolved,
            onClick = onConfirm
        ),
        negativeButton = DialogState.DialogButton(
            text = R.string.ai_support_chat_mark_resolved_cancel,
            onClick = onDismiss
        ),
        isCancelable = false,
        onDismiss = onDismiss
    ).Render()
}

@Composable
private fun InputBar(
    input: String,
    isSending: Boolean,
    enabled: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = input.isNotBlank() && !isSending && enabled
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(dimensionResource(R.dimen.major_100)),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChanged,
            modifier = Modifier.weight(1f),
            enabled = !isSending && enabled,
            minLines = 1,
            maxLines = 4,
            placeholder = { Text(stringResource(R.string.ai_support_chat_input_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSendClicked() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.minor_100)))
        WCColoredButton(
            onClick = onSendClicked,
            text = stringResource(R.string.ai_support_chat_send),
            enabled = canSend,
            loading = isSending,
            contentPadding = ButtonDefaults.ContentPadding,
            modifier = Modifier.height(56.dp)
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun AiSupportChatScreenPreview() {
    WooThemeWithBackground {
        AiSupportChatScreen(
            viewState = AiSupportChatViewState(
                input = "Orders are not loading",
                messages = listOf(
                    AiSupportChatMessage(
                        id = "greeting",
                        role = AiSupportChatMessageRole.BOT,
                        content = AiSupportChatMessageContent.Greeting
                    ),
                    AiSupportChatMessage(
                        id = "user-1",
                        role = AiSupportChatMessageRole.USER,
                        content = AiSupportChatMessageContent.Text("I can't see my orders")
                    ),
                    AiSupportChatMessage(
                        id = "bot-2",
                        role = AiSupportChatMessageRole.BOT,
                        content = AiSupportChatMessageContent.Text(
                            "Let's check a few things. First, confirm your store is connected."
                        )
                    )
                ),
                hasProceededToChat = true,
                hasStartedChat = true
            ),
            onInputChanged = {},
            onSendClicked = {},
            onIssueSelected = { _, _ -> },
            onContinueAfterDiagnosticsClicked = {},
            onSuggestedFixActionClicked = {},
            onContactSupportClicked = {},
            onContactSupportFromErrorClicked = {},
            onSendErrorDismissed = {},
            onRetryLoadHistoryClicked = {},
            onMarkResolvedConfirmed = {},
            onMarkResolvedDismissed = {},
            onFeedbackClicked = { _, _ -> }
        )
    }
}

private const val MAX_BUBBLE_WIDTH_FRACTION = 0.88f
private val MIN_HORIZONTAL_DIAGNOSTIC_ACTIONS_WIDTH = 360.dp
private val MESSAGE_BUBBLE_CORNER_RADIUS = 16.dp
private val FEEDBACK_BUTTON_SIZE = 48.dp
private val RATED_FEEDBACK_ICON_SIZE = 18.dp
private val TYPING_INDICATOR_SIZE = 16.dp
