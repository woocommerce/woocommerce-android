package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage

class SlidingWindowHistoryBudgeter(
    private val windowSize: Int = DEFAULT_WINDOW_SIZE,
) : HistoryBudgeter {

    init {
        require(windowSize >= 0) { "windowSize must be non-negative, was $windowSize" }
    }

    override fun build(
        systemPrompt: AssistantMessage.System,
        rawTranscript: List<AssistantMessage>,
        currentUserTurn: AssistantMessage.User,
    ): BudgetedHistory {
        val window = rawTranscript.takeLast(windowSize)
            .dropWhile { it is AssistantMessage.Tool }
        return BudgetedHistory(
            messages = listOf(systemPrompt) + window + currentUserTurn,
            retainedEntityRefs = emptyList(),
        )
    }

    companion object {
        internal const val DEFAULT_WINDOW_SIZE = 20
    }
}
