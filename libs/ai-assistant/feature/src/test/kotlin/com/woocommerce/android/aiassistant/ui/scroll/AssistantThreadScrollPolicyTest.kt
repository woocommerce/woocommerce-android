package com.woocommerce.android.aiassistant.ui.scroll

import com.woocommerce.android.aiassistant.ui.AssistantUiMessage
import com.woocommerce.android.aiassistant.ui.AssistantUiSegment
import com.woocommerce.android.aiassistant.ui.AssistantUiState
import com.woocommerce.android.aiassistant.ui.AssistantUiStatus
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantThreadScrollPolicyTest {
    @Test
    fun `given no previous signal, when content is non-empty, then decision is none`() {
        val current = assistantSignal(renderedItemCount = 2)

        val decision = decideAssistantThreadScroll(
            previous = null,
            current = current,
            autoFollowEnabled = true,
        )

        assertThat(decision).isEqualTo(AssistantThreadScrollDecision.None)
    }

    @Test
    fun `given same signal, when comparing, then decision is none`() {
        val current = assistantSignal(lastMessageTextLength = 12)

        val decision = decideAssistantThreadScroll(
            previous = current,
            current = current,
            autoFollowEnabled = true,
        )

        assertThat(decision).isEqualTo(AssistantThreadScrollDecision.None)
    }

    @Test
    fun `given new user message, when auto follow is disabled, then decision animates to latest`() {
        val previous = userSignal(lastMessageId = "user-1", lastUserMessageId = "user-1")
        val current = userSignal(lastMessageId = "user-2", lastUserMessageId = "user-2")

        val decision = decideAssistantThreadScroll(
            previous = previous,
            current = current,
            autoFollowEnabled = false,
        )

        assertThat(decision).isEqualTo(AssistantThreadScrollDecision.AnimateToLatest)
    }

    @Test
    fun `given new assistant message while following, when comparing, then decision animates to latest`() {
        val previous = userSignal(lastMessageId = "user-1", lastUserMessageId = "user-1")
        val current = assistantSignal(
            lastMessageId = "assistant-1",
            lastUserMessageId = "user-1",
        )

        val decision = decideAssistantThreadScroll(
            previous = previous,
            current = current,
            autoFollowEnabled = true,
        )

        assertThat(decision).isEqualTo(AssistantThreadScrollDecision.AnimateToLatest)
    }

    @Test
    fun `given auto follow disabled, when assistant text grows, then decision is none`() {
        val previous = assistantSignal(lastMessageTextLength = 6)
        val current = assistantSignal(lastMessageTextLength = 24)

        val decision = decideAssistantThreadScroll(
            previous = previous,
            current = current,
            autoFollowEnabled = false,
        )

        assertThat(decision).isEqualTo(AssistantThreadScrollDecision.None)
    }

    @Test
    fun `given auto follow enabled, when assistant text grows, then decision snaps to latest`() {
        val previous = assistantSignal(lastMessageTextLength = 6)
        val current = assistantSignal(lastMessageTextLength = 24)

        val decision = decideAssistantThreadScroll(
            previous = previous,
            current = current,
            autoFollowEnabled = true,
        )

        assertThat(decision).isEqualTo(AssistantThreadScrollDecision.SnapToLatest)
    }

    @Test
    fun `given streaming finishes and cards are revealed, when following, then decision animates to latest`() {
        val previous = assistantSignal(
            lastMessageSegmentCount = 1,
            status = AssistantUiStatus.STREAMING,
            activeAssistantMessageId = "assistant-1",
        )
        val current = assistantSignal(
            lastMessageSegmentCount = 2,
            status = AssistantUiStatus.IDLE,
            activeAssistantMessageId = null,
        )

        val decision = decideAssistantThreadScroll(
            previous = previous,
            current = current,
            autoFollowEnabled = true,
        )

        assertThat(decision).isEqualTo(AssistantThreadScrollDecision.AnimateToLatest)
    }

    @Test
    fun `given confirmation becomes active, when following, then decision animates to latest`() {
        val previous = assistantSignal(activeConfirmationId = null)
        val current = assistantSignal(activeConfirmationId = "confirmation-1")

        val decision = decideAssistantThreadScroll(
            previous = previous,
            current = current,
            autoFollowEnabled = true,
        )

        assertThat(decision).isEqualTo(AssistantThreadScrollDecision.AnimateToLatest)
    }

    @Test
    fun `given confirmation resolves while not following, when comparing, then decision is none`() {
        val previous = assistantSignal(activeConfirmationId = "confirmation-1")
        val current = assistantSignal(activeConfirmationId = null)

        val decision = decideAssistantThreadScroll(
            previous = previous,
            current = current,
            autoFollowEnabled = false,
        )

        assertThat(decision).isEqualTo(AssistantThreadScrollDecision.None)
    }

    @Test
    fun `given cards are visible at idle, when computing signal, then ordered segment count includes cards`() {
        val message = assistantMessage(
            AssistantUiSegment.Text("Here are recent orders."),
            AssistantUiSegment.CardGroup(listOf(orderCard())),
        )
        val state = AssistantUiState(
            messages = listOf(message),
            status = AssistantUiStatus.IDLE,
        )

        val signal = state.messages.toThreadScrollSignal(
            state = state,
            showTypingIndicator = false,
        )

        assertThat(signal.renderedItemCount).isEqualTo(1)
        assertThat(signal.lastMessageSegmentCount).isEqualTo(2)
        assertThat(signal.lastMessageTextLength).isEqualTo("Here are recent orders.".length)
    }

    @Test
    fun `given cards are hidden during streaming, when computing signal, then active assistant fields are populated`() {
        val message = assistantMessage(
            AssistantUiSegment.Text("Loading"),
            AssistantUiSegment.CardGroup(listOf(orderCard())),
        )
        val state = AssistantUiState(
            messages = listOf(message),
            status = AssistantUiStatus.STREAMING,
            activeAssistantMessageId = message.id,
        )

        val signal = state.messages.toThreadScrollSignal(
            state = state,
            showTypingIndicator = true,
        )

        assertThat(signal.renderedItemCount).isEqualTo(2)
        assertThat(signal.lastMessageSegmentCount).isEqualTo(1)
        assertThat(signal.activeAssistantMessageId).isEqualTo(message.id)
        assertThat(signal.showTypingIndicator).isTrue()
    }

    private fun userSignal(
        lastMessageId: String = "user-1",
        lastUserMessageId: String = "user-1",
    ) = assistantSignal(
        lastMessageId = lastMessageId,
        lastMessageRole = AssistantUiMessage.Role.USER,
        lastUserMessageId = lastUserMessageId,
        lastMessageTextLength = 12,
    )

    private fun assistantSignal(
        renderedItemCount: Int = 1,
        lastMessageId: String = "assistant-1",
        lastMessageRole: AssistantUiMessage.Role = AssistantUiMessage.Role.ASSISTANT,
        lastUserMessageId: String? = "user-1",
        lastMessageSegmentCount: Int = 1,
        lastMessageTextLength: Int = 12,
        status: AssistantUiStatus = AssistantUiStatus.IDLE,
        activeAssistantMessageId: String? = null,
        activeConfirmationId: String? = null,
        showTypingIndicator: Boolean = false,
    ) = AssistantThreadScrollSignal(
        renderedItemCount = renderedItemCount,
        messageCount = renderedItemCount,
        lastMessageId = lastMessageId,
        lastMessageRole = lastMessageRole,
        lastUserMessageId = lastUserMessageId,
        lastMessageSegmentCount = lastMessageSegmentCount,
        lastMessageTextLength = lastMessageTextLength,
        activeAssistantMessageId = activeAssistantMessageId,
        activeConfirmationId = activeConfirmationId,
        status = status,
        showTypingIndicator = showTypingIndicator,
    )

    private fun assistantMessage(vararg segments: AssistantUiSegment) = AssistantUiMessage(
        id = "assistant-1",
        role = AssistantUiMessage.Role.ASSISTANT,
        segments = segments.toList(),
    )

    private fun orderCard() = AssistantCard.Order(
        remoteOrderId = 1L,
        number = "#1",
        status = "processing",
        total = "12.34",
        currency = "USD",
        customerName = "Jane Doe",
        date = "2026-05-01T10:00:00Z",
    )
}
