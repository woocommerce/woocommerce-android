package com.woocommerce.android.aiassistant.core.history

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.SlidingWindowHistoryBudgeter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class ModelRequestHistoryBuilderTest {
    @Test
    fun `given session history, when building, then model history uses distinct assistant message types`() {
        val sessionHistory = AssistantSessionHistory(
            messages = listOf(
                AssistantSessionMessage.User("Earlier question"),
                AssistantSessionMessage.Assistant("Earlier answer"),
            )
        )
        val builder = ModelRequestHistoryBuilder(passThroughBudgeter())

        val result = builder.build(
            systemPrompt = "Fresh prompt",
            sessionHistory = sessionHistory,
            currentUserMessage = "Current question",
        )

        assertThat(result.messages).containsExactly(
            AssistantMessage.System("Fresh prompt"),
            AssistantMessage.User("Earlier question"),
            AssistantMessage.Assistant("Earlier answer"),
            AssistantMessage.User("Current question"),
        )
        assertThat(result.currentUserTurn).isEqualTo(AssistantMessage.User("Current question"))
        assertThat(result.messages.last()).isEqualTo(result.currentUserTurn)
    }

    @Test
    fun `given blank system prompt, when building, then blank system message is preserved`() {
        val builder = ModelRequestHistoryBuilder(passThroughBudgeter())

        val result = builder.build(
            systemPrompt = "",
            sessionHistory = AssistantSessionHistory.Empty,
            currentUserMessage = "Hello",
        )

        assertThat(result.messages).containsExactly(
            AssistantMessage.System(""),
            AssistantMessage.User("Hello"),
        )
    }

    @Test
    fun `given stale prompt concept in old model fixtures, when building, then fresh prompt is first`() {
        val sessionHistory = AssistantSessionHistory(
            messages = listOf(
                AssistantSessionMessage.User("Earlier question"),
                AssistantSessionMessage.Assistant("Earlier answer"),
            )
        )
        val builder = ModelRequestHistoryBuilder(passThroughBudgeter())

        val result = builder.build(
            systemPrompt = "Fresh prompt",
            sessionHistory = sessionHistory,
            currentUserMessage = "Current question",
        )

        assertThat(result.messages.first()).isEqualTo(AssistantMessage.System("Fresh prompt"))
        assertThat(result.messages).doesNotContain(AssistantMessage.System("Stale prompt"))
    }

    @Test
    fun `given long session history, when building, then budgeting is applied before current user turn`() {
        val sessionHistory = AssistantSessionHistory(
            messages = listOf(
                AssistantSessionMessage.User("First"),
                AssistantSessionMessage.Assistant("Second"),
                AssistantSessionMessage.User("Third"),
                AssistantSessionMessage.Assistant("Fourth"),
            )
        )
        val builder = ModelRequestHistoryBuilder(SlidingWindowHistoryBudgeter(windowSize = 2))

        val result = builder.build(
            systemPrompt = "Fresh prompt",
            sessionHistory = sessionHistory,
            currentUserMessage = "Current",
        )

        assertThat(result.messages).containsExactly(
            AssistantMessage.System("Fresh prompt"),
            AssistantMessage.User("Third"),
            AssistantMessage.Assistant("Fourth"),
            AssistantMessage.User("Current"),
        )
        assertThat(result.currentUserTurn).isEqualTo(AssistantMessage.User("Current"))
    }

    private fun passThroughBudgeter() = HistoryBudgeter { systemPrompt, rawTranscript, currentUserTurn ->
        com.woocommerce.android.aiassistant.core.loop.BudgetedHistory(
            messages = listOf(systemPrompt) + rawTranscript + currentUserTurn,
            retainedEntityRefs = emptyList(),
        )
    }
}
