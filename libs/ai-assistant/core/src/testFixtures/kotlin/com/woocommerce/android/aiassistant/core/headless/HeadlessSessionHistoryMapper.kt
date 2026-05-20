package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.history.AssistantSessionHistory
import com.woocommerce.android.aiassistant.core.history.AssistantSessionMessage

internal class HeadlessSessionHistoryMapper {
    fun appendTurn(
        baseHistory: AssistantSessionHistory,
        modelTurnMessages: List<AssistantMessage>,
        error: AssistantError? = null,
    ): AssistantSessionHistory {
        val preserveToolExchanges = error != AssistantError.Cancelled
        val sessionMessages = buildList {
            var index = 0
            while (index < modelTurnMessages.size) {
                when (val message = modelTurnMessages[index]) {
                    is AssistantMessage.User -> {
                        add(AssistantSessionMessage.User(message.content))
                        index += 1
                    }
                    is AssistantMessage.Assistant -> {
                        if (message.toolCalls.isEmpty()) {
                            message.contentAsSessionMessage()?.let(::add)
                            index += 1
                        } else {
                            val toolResults = modelTurnMessages.contiguousToolResultsAfter(index)
                            val exchange = if (preserveToolExchanges) {
                                message.toToolExchangeOrNull(toolResults)
                            } else {
                                null
                            }
                            if (exchange != null) {
                                add(exchange)
                                index += 1 + toolResults.size
                            } else {
                                message.contentAsSessionMessage()?.let(::add)
                                index += 1
                            }
                        }
                    }
                    is AssistantMessage.System,
                    is AssistantMessage.Tool -> index += 1
                }
            }
        }
        return baseHistory.append(sessionMessages)
    }

    private fun AssistantMessage.Assistant.contentAsSessionMessage(): AssistantSessionMessage.Assistant? =
        content
            ?.takeIf { it.isNotBlank() }
            ?.let(AssistantSessionMessage::Assistant)

    private fun AssistantMessage.Assistant.toToolExchangeOrNull(
        toolResults: List<AssistantMessage.Tool>,
    ): AssistantSessionMessage.ToolExchange? {
        val toolCallIds = toolCalls.map { it.id }
        if (
            toolCallIds.distinct().size != toolCallIds.size ||
            toolResults.map(AssistantMessage.Tool::toolCallId) != toolCallIds
        ) {
            return null
        }
        return AssistantSessionMessage.ToolExchange(
            assistantContent = content?.takeIf { it.isNotBlank() },
            toolCalls = toolCalls,
            toolResults = toolResults,
        )
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
}
