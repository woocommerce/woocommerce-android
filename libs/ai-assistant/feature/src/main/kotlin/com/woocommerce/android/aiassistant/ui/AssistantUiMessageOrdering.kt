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
    val collapsedSegments = displaySegments.collapseToolActivityToLatest()
    val hasAssistantText = collapsedSegments.any { it is AssistantUiSegment.Text && it.text.isNotEmpty() }
    if (isStreaming || !hasAssistantText) return collapsedSegments

    return collapsedSegments.filterNot { it is AssistantUiSegment.CardGroup } +
        collapsedSegments.filterIsInstance<AssistantUiSegment.CardGroup>()
}

private fun AssistantUiState.isStreamingMessage(message: AssistantUiMessage): Boolean =
    status == AssistantUiStatus.STREAMING &&
        message.role == AssistantUiMessage.Role.ASSISTANT &&
        message.id == activeAssistantMessageId

private fun List<AssistantUiSegment>.collapseToolActivityToLatest(): List<AssistantUiSegment> {
    val latestToolActivityIndex = indexOfLast { it is AssistantUiSegment.ToolActivity }
    if (latestToolActivityIndex == -1) return this

    return filterIndexed { index, segment ->
        segment !is AssistantUiSegment.ToolActivity || index == latestToolActivityIndex
    }
}
