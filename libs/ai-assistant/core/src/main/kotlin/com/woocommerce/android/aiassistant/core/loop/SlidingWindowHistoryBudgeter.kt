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
        val window = rawTranscript
            .toBudgetSpans()
            .takeNewestSpans(windowSize)
            .flatMap(BudgetSpan::messages)
        return BudgetedHistory(
            messages = listOf(systemPrompt) + window + currentUserTurn,
            retainedEntityRefs = emptyList(),
        )
    }

    private fun List<AssistantMessage>.toBudgetSpans(): List<BudgetSpan> = buildList {
        var index = 0
        while (index < this@toBudgetSpans.size) {
            when (val message = this@toBudgetSpans[index]) {
                is AssistantMessage.User -> {
                    add(BudgetSpan.Plain(message))
                    index += 1
                }
                is AssistantMessage.Assistant -> {
                    if (message.toolCalls.isEmpty()) {
                        add(BudgetSpan.Plain(message))
                        index += 1
                    } else {
                        val toolResults = contiguousToolResultsAfter(index)
                        val toolCallIds = message.toolCalls.map { it.id }
                        if (
                            toolCallIds.distinct().size == toolCallIds.size &&
                            toolResults.map(AssistantMessage.Tool::toolCallId) == toolCallIds
                        ) {
                            add(BudgetSpan.ToolExchange(listOf(message) + toolResults))
                        }
                        index += 1 + toolResults.size
                    }
                }
                is AssistantMessage.System,
                is AssistantMessage.Tool -> index += 1
            }
        }
    }

    private fun List<BudgetSpan>.takeNewestSpans(maxMessages: Int): List<BudgetSpan> {
        val retained = mutableListOf<BudgetSpan>()
        var remaining = maxMessages
        for (span in asReversed()) {
            if (span.messages.size > remaining) {
                break
            }
            retained += span
            remaining -= span.messages.size
        }
        return retained.asReversed()
    }

    private fun List<AssistantMessage>.contiguousToolResultsAfter(index: Int): List<AssistantMessage.Tool> {
        val toolResults = mutableListOf<AssistantMessage.Tool>()
        var nextIndex = index + 1
        while (nextIndex < size) {
            val message = this[nextIndex]
            if (message !is AssistantMessage.Tool) {
                break
            }
            toolResults += message
            nextIndex += 1
        }
        return toolResults
    }

    private sealed interface BudgetSpan {
        val messages: List<AssistantMessage>

        data class Plain(
            val message: AssistantMessage,
        ) : BudgetSpan {
            override val messages = listOf(message)
        }

        data class ToolExchange(
            override val messages: List<AssistantMessage>,
        ) : BudgetSpan
    }

    companion object {
        internal const val DEFAULT_WINDOW_SIZE = 20
    }
}
