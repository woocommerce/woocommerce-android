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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportIssueType
import com.woocommerce.android.ui.aisupportchat.diagnostics.TestStatus
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun AiSupportChatScreen(viewModel: AiSupportChatViewModel) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    AiSupportChatScreen(
        viewState = viewState,
        onInputChanged = viewModel::onInputChanged,
        onSendClicked = viewModel::onSendClicked,
        onIssueSelected = viewModel::onIssueSelected,
        onRetryDiagnosticsClicked = viewModel::onRetryDiagnosticsClicked,
        onContinueAfterDiagnosticsClicked = viewModel::onContinueAfterDiagnosticsClicked
    )
}

@Composable
fun AiSupportChatScreen(
    viewState: AiSupportChatViewState,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onIssueSelected: (SupportIssueType, String) -> Unit,
    onRetryDiagnosticsClicked: () -> Unit,
    onContinueAfterDiagnosticsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        MessageList(
            messages = viewState.messages,
            isSending = viewState.isSending,
            showDiagnosticActions = viewState.showDiagnosticActions,
            onIssueSelected = onIssueSelected,
            onRetryDiagnosticsClicked = onRetryDiagnosticsClicked,
            onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        if (viewState.showSendError) {
            ErrorBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.major_100))
            )
        }
        InputBar(
            input = viewState.input,
            isSending = viewState.isSending,
            enabled = viewState.hasProceededToChat,
            onInputChanged = onInputChanged,
            onSendClicked = onSendClicked,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MessageList(
    messages: List<AiSupportChatMessage>,
    isSending: Boolean,
    showDiagnosticActions: Boolean,
    onIssueSelected: (SupportIssueType, String) -> Unit,
    onRetryDiagnosticsClicked: () -> Unit,
    onContinueAfterDiagnosticsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val itemCount = messages.size + (if (isSending) 1 else 0)

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
                showDiagnosticActions = showDiagnosticActions,
                onIssueSelected = onIssueSelected,
                onRetryDiagnosticsClicked = onRetryDiagnosticsClicked,
                onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked
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
    showDiagnosticActions: Boolean,
    onIssueSelected: (SupportIssueType, String) -> Unit,
    onRetryDiagnosticsClicked: () -> Unit,
    onContinueAfterDiagnosticsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == AiSupportChatMessageRole.USER
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            MessageContent(
                content = message.content,
                textColor = textColor,
                showDiagnosticActions = showDiagnosticActions,
                onIssueSelected = onIssueSelected,
                onRetryDiagnosticsClicked = onRetryDiagnosticsClicked,
                onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked
            )
        }
    }
}

@Composable
private fun MessageContent(
    content: AiSupportChatMessageContent,
    textColor: Color,
    showDiagnosticActions: Boolean,
    onIssueSelected: (SupportIssueType, String) -> Unit,
    onRetryDiagnosticsClicked: () -> Unit,
    onContinueAfterDiagnosticsClicked: () -> Unit
) {
    when (content) {
        AiSupportChatMessageContent.Greeting -> TextContent(
            text = stringResource(R.string.ai_support_chat_greeting),
            color = textColor
        )
        AiSupportChatMessageContent.IssuePicker -> IssuePickerContent(
            textColor = textColor,
            onIssueSelected = onIssueSelected
        )
        AiSupportChatMessageContent.PostDiagnosticsGreeting -> TextContent(
            text = stringResource(R.string.ai_support_chat_post_diagnostics_greeting),
            color = textColor
        )
        is AiSupportChatMessageContent.Text -> TextContent(
            text = content.text,
            color = textColor
        )
        is AiSupportChatMessageContent.DiagnosticsProgress -> DiagnosticsContent(
            result = content.result,
            textColor = textColor,
            showDiagnosticActions = showDiagnosticActions,
            onRetryDiagnosticsClicked = onRetryDiagnosticsClicked,
            onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked
        )
        is AiSupportChatMessageContent.DiagnosticsFailure -> DiagnosticsContent(
            result = content.result,
            textColor = textColor,
            showDiagnosticActions = showDiagnosticActions,
            onRetryDiagnosticsClicked = onRetryDiagnosticsClicked,
            onContinueAfterDiagnosticsClicked = onContinueAfterDiagnosticsClicked
        )
    }
}

@Composable
private fun TextContent(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.major_100),
            vertical = dimensionResource(R.dimen.minor_100)
        )
    )
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
    onRetryDiagnosticsClicked: () -> Unit,
    onContinueAfterDiagnosticsClicked: () -> Unit
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
                    text = stringResource(R.string.ai_support_chat_diagnostics_failure),
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.minor_100))) {
                if (hasFailure) {
                    WCOutlinedButton(
                        onClick = onRetryDiagnosticsClicked,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.ai_support_chat_diagnostics_retry))
                    }
                }
                WCColoredButton(
                    onClick = onContinueAfterDiagnosticsClicked,
                    text = stringResource(R.string.ai_support_chat_diagnostics_continue),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.semantics(mergeDescendants = true) {
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
                    modifier = Modifier.size(16.dp)
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.alpha(alpha)
    )
}

@Composable
private fun ErrorBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(dimensionResource(R.dimen.minor_100)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.ai_support_chat_send_error),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
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
            onRetryDiagnosticsClicked = {},
            onContinueAfterDiagnosticsClicked = {}
        )
    }
}
