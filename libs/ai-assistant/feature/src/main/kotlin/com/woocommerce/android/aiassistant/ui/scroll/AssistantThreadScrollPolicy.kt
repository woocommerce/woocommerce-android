package com.woocommerce.android.aiassistant.ui.scroll

import com.woocommerce.android.aiassistant.ui.AssistantUiMessage
import com.woocommerce.android.aiassistant.ui.AssistantUiSegment
import com.woocommerce.android.aiassistant.ui.AssistantUiState
import com.woocommerce.android.aiassistant.ui.AssistantUiStatus
import com.woocommerce.android.aiassistant.ui.orderedSegments

internal fun List<AssistantUiMessage>.toThreadScrollSignal(
    state: AssistantUiState,
    showTypingIndicator: Boolean,
): AssistantThreadScrollSignal {
    val lastMessage = lastOrNull()
    val lastMessageSegments = lastMessage?.orderedSegments(state).orEmpty()

    return AssistantThreadScrollSignal(
        renderedItemCount = size + if (showTypingIndicator) 1 else 0,
        messageCount = size,
        lastMessageId = lastMessage?.id,
        lastMessageRole = lastMessage?.role,
        lastUserMessageId = lastOrNull { it.role == AssistantUiMessage.Role.USER }?.id,
        lastMessageSegmentCount = lastMessageSegments.size,
        lastMessageTextLength = lastMessageSegments
            .filterIsInstance<AssistantUiSegment.Text>()
            .sumOf { it.text.length },
        activeAssistantMessageId = state.activeAssistantMessageId,
        activeConfirmationId = state.activeConfirmationId,
        status = state.status,
        showTypingIndicator = showTypingIndicator,
    )
}

internal data class AssistantThreadScrollSignal(
    val renderedItemCount: Int,
    val messageCount: Int,
    val lastMessageId: String?,
    val lastMessageRole: AssistantUiMessage.Role?,
    val lastUserMessageId: String?,
    val lastMessageSegmentCount: Int,
    val lastMessageTextLength: Int,
    val activeAssistantMessageId: String?,
    val activeConfirmationId: String?,
    val status: AssistantUiStatus,
    val showTypingIndicator: Boolean,
)

internal sealed interface AssistantThreadScrollDecision {
    data object None : AssistantThreadScrollDecision
    data object AnimateToLatest : AssistantThreadScrollDecision
    data object SnapToLatest : AssistantThreadScrollDecision
}

internal fun decideAssistantThreadScroll(
    previous: AssistantThreadScrollSignal?,
    current: AssistantThreadScrollSignal,
    autoFollowEnabled: Boolean,
): AssistantThreadScrollDecision {
    val previousSignal = previous.takeUnless { current.renderedItemCount == 0 || it == current }
    if (previousSignal == null) {
        return AssistantThreadScrollDecision.None
    }

    return when {
        previousSignal.hasNewUserMessage(current) -> AssistantThreadScrollDecision.AnimateToLatest
        !autoFollowEnabled -> AssistantThreadScrollDecision.None
        previousSignal.shouldAnimateToLatest(current) -> AssistantThreadScrollDecision.AnimateToLatest
        previousSignal.hasRenderedContentChange(current) -> AssistantThreadScrollDecision.SnapToLatest
        else -> AssistantThreadScrollDecision.None
    }
}

private fun AssistantThreadScrollSignal.hasNewUserMessage(
    current: AssistantThreadScrollSignal,
): Boolean = lastUserMessageId != current.lastUserMessageId

private fun AssistantThreadScrollSignal.shouldAnimateToLatest(
    current: AssistantThreadScrollSignal,
): Boolean = hasNewAssistantMessage(current) ||
    didRevealCardsAfterStreaming(current) ||
    activeConfirmationId != current.activeConfirmationId

private fun AssistantThreadScrollSignal.hasNewAssistantMessage(
    current: AssistantThreadScrollSignal,
): Boolean = lastMessageId != current.lastMessageId &&
    current.lastMessageRole == AssistantUiMessage.Role.ASSISTANT

private fun AssistantThreadScrollSignal.didRevealCardsAfterStreaming(
    current: AssistantThreadScrollSignal,
): Boolean = status == AssistantUiStatus.STREAMING &&
    current.status != AssistantUiStatus.STREAMING &&
    lastMessageId == current.lastMessageId &&
    current.lastMessageSegmentCount > lastMessageSegmentCount

private fun AssistantThreadScrollSignal.hasRenderedContentChange(
    current: AssistantThreadScrollSignal,
): Boolean = renderedItemCount != current.renderedItemCount ||
    messageCount != current.messageCount ||
    lastMessageSegmentCount != current.lastMessageSegmentCount ||
    lastMessageTextLength != current.lastMessageTextLength ||
    showTypingIndicator != current.showTypingIndicator ||
    activeAssistantMessageId != current.activeAssistantMessageId ||
    status != current.status
