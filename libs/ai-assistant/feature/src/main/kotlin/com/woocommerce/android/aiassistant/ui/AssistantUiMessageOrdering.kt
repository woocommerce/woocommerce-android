package com.woocommerce.android.aiassistant.ui

internal fun AssistantUiMessage.hasVisibleContent(state: AssistantUiState): Boolean =
    role == AssistantUiMessage.Role.USER ||
        error != null ||
        orderedSegments(state).any { segment ->
            when (segment) {
                is AssistantUiSegment.Text -> segment.text.isNotEmpty()
                is AssistantUiSegment.CardGroup -> segment.cards.isNotEmpty()
                is AssistantUiSegment.ConfirmationCard,
                is AssistantUiSegment.ToolActivity -> true
            }
        }

internal fun AssistantUiMessage.orderedSegments(state: AssistantUiState): List<AssistantUiSegment> {
    if (role != AssistantUiMessage.Role.ASSISTANT) return segments

    val isStreaming = state.isStreamingMessage(this)
    val displaySegments = if (isStreaming) {
        segments.filterNot { it is AssistantUiSegment.CardGroup }
    } else {
        segments
    }
    val hasAssistantText = displaySegments.any { it is AssistantUiSegment.Text && it.text.isNotEmpty() }
    if (isStreaming || !hasAssistantText) return displaySegments

    return displaySegments.filterNot { it is AssistantUiSegment.CardGroup } +
        displaySegments.filterIsInstance<AssistantUiSegment.CardGroup>()
}

private fun AssistantUiState.isStreamingMessage(message: AssistantUiMessage): Boolean =
    status == AssistantUiStatus.STREAMING &&
        message.role == AssistantUiMessage.Role.ASSISTANT &&
        message.id == activeAssistantMessageId
