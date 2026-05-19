package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

    @Test
    fun `given window cuts into tool-call exchange, when building, then no orphaned Tool message starts the window`() {
        val toolCall = ToolCall(id = "call_1", name = "orders_list", arguments = buildJsonObject { })
        val assistantWithToolCall = AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall))
        val toolResult = AssistantMessage.Tool(toolCallId = "call_1", content = """{"orders":[]}""")
        val assistantFinal = AssistantMessage.Assistant(content = "Here are your orders.")
        val transcript = listOf(
            AssistantMessage.User("show orders"),
            assistantWithToolCall,
            toolResult,
            assistantFinal,
        )
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 2)

        val result = budgeter.build(system, transcript, user)

        assertThat(result.messages).doesNotContain(toolResult)
        assertThat(result.messages.filterIsInstance<AssistantMessage.Tool>()).isEmpty()
    }

    @Test
    fun `given window size 2 on 4-message transcript ending with tool then assistant, when building, then orphaned tool is dropped and only assistant response is kept`() {
        // Transcript: [User, Assistant(toolCalls=[c1]), Tool(c1), Assistant("Done")]
        // takeLast(2) alone would give [Tool(c1), Assistant("Done")] — Tool(c1) has no
        // preceding assistant tool-call message, which is invalid for the OpenAI wire format.
        // The budgeter must advance the window start past orphaned Tool messages.
        val toolCall = ToolCall(id = "call_1", name = "orders_list", arguments = buildJsonObject { })
        val assistantWithToolCall = AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall))
        val toolResult = AssistantMessage.Tool(toolCallId = "call_1", content = """{"orders":[]}""")
        val assistantFinal = AssistantMessage.Assistant(content = "Here are your orders.")
        val transcript = listOf(
            AssistantMessage.User("show orders"),
            assistantWithToolCall,
            toolResult,
            assistantFinal,
        )
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 2)

        val result = budgeter.build(system, transcript, user)

        assertThat(result.messages).containsExactly(system, assistantFinal, user)
    }

    @Test
    fun `given complete tool exchange fits window, when building, then exchange is retained whole`() {
        val toolCall = toolCall()
        val assistantWithToolCall = AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall))
        val toolResult = AssistantMessage.Tool(toolCallId = toolCall.id, content = """{"orders":[]}""")
        val assistantFinal = AssistantMessage.Assistant(content = "Here are your orders.")
        val transcript = listOf(assistantWithToolCall, toolResult, assistantFinal)
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 3)

        val result = budgeter.build(system, transcript, user)

        assertThat(result.messages).containsExactly(
            system,
            assistantWithToolCall,
            toolResult,
            assistantFinal,
            user,
        )
    }

    @Test
    fun `given assistant tool call missing a result, when building, then protocol is dropped`() {
        val firstCall = toolCall(id = "call_1")
        val secondCall = toolCall(id = "call_2")
        val assistantWithToolCalls = AssistantMessage.Assistant(
            content = null,
            toolCalls = listOf(firstCall, secondCall),
        )
        val transcript = listOf(
            AssistantMessage.User("show orders"),
            assistantWithToolCalls,
            AssistantMessage.Tool(toolCallId = firstCall.id, content = """{"orders":[]}"""),
            AssistantMessage.Assistant(content = "Done"),
        )
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 10)

        val result = budgeter.build(system, transcript, user)

        assertThat(result.messages).containsExactly(
            system,
            AssistantMessage.User("show orders"),
            AssistantMessage.Assistant(content = "Done"),
            user,
        )
    }

    @Test
    fun `given leading tool result, when building, then orphan tool result is dropped`() {
        val toolResult = AssistantMessage.Tool(toolCallId = "call_1", content = """{"orders":[]}""")
        val assistantFinal = AssistantMessage.Assistant(content = "Done")
        val transcript = listOf(toolResult, assistantFinal)
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 10)

        val result = budgeter.build(system, transcript, user)

        assertThat(result.messages).containsExactly(system, assistantFinal, user)
    }

    @Test
    fun `given multi call exchange, when building, then all results are retained in call order`() {
        val firstCall = toolCall(id = "call_1")
        val secondCall = toolCall(id = "call_2")
        val assistantWithToolCalls = AssistantMessage.Assistant(
            content = null,
            toolCalls = listOf(firstCall, secondCall),
        )
        val firstResult = AssistantMessage.Tool(toolCallId = firstCall.id, content = """{"first":true}""")
        val secondResult = AssistantMessage.Tool(toolCallId = secondCall.id, content = """{"second":true}""")
        val transcript = listOf(assistantWithToolCalls, firstResult, secondResult)
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 3)

        val result = budgeter.build(system, transcript, user)

        assertThat(result.messages).containsExactly(system, assistantWithToolCalls, firstResult, secondResult, user)
    }

    @Test
    fun `given multi call exchange with out of order results, when building, then whole exchange is dropped`() {
        val firstCall = toolCall(id = "call_1")
        val secondCall = toolCall(id = "call_2")
        val assistantWithToolCalls = AssistantMessage.Assistant(
            content = null,
            toolCalls = listOf(firstCall, secondCall),
        )
        val transcript = listOf(
            assistantWithToolCalls,
            AssistantMessage.Tool(toolCallId = secondCall.id, content = """{"second":true}"""),
            AssistantMessage.Tool(toolCallId = firstCall.id, content = """{"first":true}"""),
            AssistantMessage.Assistant(content = "Final"),
        )
        val budgeter = SlidingWindowHistoryBudgeter(windowSize = 10)

        val result = budgeter.build(system, transcript, user)

        assertThat(result.messages).containsExactly(system, AssistantMessage.Assistant("Final"), user)
    }

    @Test
    fun `given negative window size, when constructing, then throws IllegalArgumentException`() {
        assertThatThrownBy { SlidingWindowHistoryBudgeter(windowSize = -1) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun toolCall(
        id: String = "call_1",
    ) = ToolCall(id = id, name = "orders_list", arguments = buildJsonObject { })
}
