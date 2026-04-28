package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SlidingWindowHistoryBudgeterTest {
    private val system = AssistantMessage.System("You are a helpful assistant.")
    private val user = AssistantMessage.User("What can you do?")

    @Test
    fun `given empty transcript, when building, then messages contain only system and user turn`() {
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 5)

        val result = budgeter.build(system, emptyList(), user)

        assertThat(result.messages).containsExactly(system, user)
    }

    @Test
    fun `given transcript shorter than window size, when building, then all transcript messages are included`() {
        val transcript = listOf(
            AssistantMessage.User("hi"),
            AssistantMessage.Assistant("Hello!"),
        )
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 10)

        val result = budgeter.build(system, transcript, user)

        assertThat(result.messages).containsExactly(system, transcript[0], transcript[1], user)
    }

    @Test
    fun `given transcript longer than window size, when building, then only most recent messages are included`() {
        val old1 = AssistantMessage.User("old turn")
        val old2 = AssistantMessage.Assistant("old response")
        val recent1 = AssistantMessage.User("recent turn")
        val recent2 = AssistantMessage.Assistant("recent response")
        val transcript = listOf(old1, old2, recent1, recent2)
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 2)

        val result = budgeter.build(system, transcript, user)

        assertThat(result.messages).containsExactly(system, recent1, recent2, user)
    }

    @Test
    fun `given any transcript, when building, then system prompt is first message`() {
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 5)

        val result = budgeter.build(system, listOf(AssistantMessage.User("hello")), user)

        assertThat(result.messages.first()).isEqualTo(system)
    }

    @Test
    fun `given any transcript, when building, then current user turn is last message`() {
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 5)

        val result = budgeter.build(system, listOf(AssistantMessage.User("hello")), user)

        assertThat(result.messages.last()).isEqualTo(user)
    }

    @Test
    fun `given any transcript, when building, then retainedEntityRefs is empty`() {
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 5)

        val result = budgeter.build(system, listOf(AssistantMessage.User("hello")), user)

        assertThat(result.retainedEntityRefs).isEmpty()
    }
}
