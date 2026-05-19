package com.woocommerce.android.aiassistant.core.history

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class AssistantSessionHistoryMapperTest {
    private val mapper = AssistantSessionHistoryMapper()

    @Test
    fun `given completed tool turn, when mapping, then matched exchange and final text are appended`() {
        val toolCall = toolCall()
        val toolResult = toolResult(toolCall.id)

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Show orders"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                toolResult,
                AssistantMessage.Assistant(content = "Here are the orders"),
            ),
            outcome = LoopOutcome.COMPLETED,
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Show orders"),
            AssistantSessionMessage.ToolExchange(
                assistantContent = null,
                toolCalls = listOf(toolCall),
                toolResults = listOf(toolResult),
            ),
            AssistantSessionMessage.Assistant("Here are the orders"),
        )
    }

    @Test
    fun `given assistant tool call has content, when completed mapping, then content is stored only in exchange`() {
        val toolCall = toolCall()
        val toolResult = toolResult(toolCall.id)

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Show orders"),
                AssistantMessage.Assistant(content = "I can check that.", toolCalls = listOf(toolCall)),
                toolResult,
                AssistantMessage.Assistant(content = "Here are the orders"),
            ),
            outcome = LoopOutcome.COMPLETED,
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Show orders"),
            AssistantSessionMessage.ToolExchange(
                assistantContent = "I can check that.",
                toolCalls = listOf(toolCall),
                toolResults = listOf(toolResult),
            ),
            AssistantSessionMessage.Assistant("Here are the orders"),
        )
    }

    @Test
    fun `given stream drops during tool call emission, when mapping, then orphaned tool call is not persisted`() {
        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Update product"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall())),
            ),
            outcome = LoopOutcome.COMPLETED,
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Update product"),
        )
    }

    @Test
    fun `given outcome unknown, when mapping, then tool protocol is not reusable session history`() {
        val toolCall = toolCall(name = "orders_update")

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Cancel order"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                AssistantMessage.Tool(toolCallId = toolCall.id, content = """{"error":"Tool execution failed"}"""),
            ),
            outcome = LoopOutcome.FAILED,
            error = AssistantError.OutcomeUnknown(toolName = "orders_update"),
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Cancel order"),
        )
    }

    @Test
    fun `given confirmation cancellation, when mapping, then cancelled tool protocol is stripped`() {
        val toolCall = toolCall(name = "orders_update")

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Cancel order"),
                AssistantMessage.Assistant(content = "I can do that.", toolCalls = listOf(toolCall)),
                AssistantMessage.Tool(toolCallId = toolCall.id, content = """{"error":"Action was cancelled"}"""),
            ),
            outcome = LoopOutcome.STOPPED,
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Cancel order"),
            AssistantSessionMessage.Assistant("I can do that."),
        )
    }

    @Test
    fun `given cancellation with partial text, when mapping, then visible partial assistant text is kept`() {
        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Explain analytics"),
                AssistantMessage.Assistant(content = "Partial answer"),
            ),
            outcome = LoopOutcome.STOPPED,
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Explain analytics"),
            AssistantSessionMessage.Assistant("Partial answer"),
        )
    }

    @Test
    fun `given malformed failed or rejected tool results, when mapping, then no tool messages are persisted`() {
        val toolCall = toolCall()

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Run tool"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                AssistantMessage.Tool(toolCallId = toolCall.id, content = """{"error":"Malformed arguments"}"""),
            ),
            outcome = LoopOutcome.FAILED,
            error = AssistantError.InvalidToolCall(toolName = toolCall.name),
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tool"),
        )
    }

    @Test
    fun `given cancelled turn with streamed assistant text, when appending, then user and assistant text are stored`() {
        val result = mapper.appendCancelledTurn(
            baseHistory = AssistantSessionHistory.Empty,
            userMessage = "Summarize sales",
            assistantText = "Sales are up",
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Summarize sales"),
            AssistantSessionMessage.Assistant("Sales are up"),
        )
    }

    @Test
    fun `given cancelled turn without streamed assistant text, when appending, then only user is stored`() {
        val result = mapper.appendCancelledTurn(
            baseHistory = AssistantSessionHistory.Empty,
            userMessage = "Summarize sales",
            assistantText = "   ",
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Summarize sales"),
        )
    }

    @Test
    fun `given model turn contains system and tool messages, when mapping, then they are never stored`() {
        val toolCall = toolCall()
        val toolResult = toolResult(toolCall.id)

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.System("system prompt"),
                AssistantMessage.User("Run tool"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                toolResult,
                AssistantMessage.Assistant(content = "Done"),
            ),
            outcome = LoopOutcome.COMPLETED,
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tool"),
            AssistantSessionMessage.ToolExchange(
                assistantContent = null,
                toolCalls = listOf(toolCall),
                toolResults = listOf(toolResult),
            ),
            AssistantSessionMessage.Assistant("Done"),
        )
    }

    @Test
    fun `given duplicate tool results, when mapping completed turn, then protocol is stripped`() {
        val toolCall = toolCall()

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Run tool"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                toolResult(toolCall.id, content = """{"ok":true}"""),
                toolResult(toolCall.id, content = """{"ok":true}"""),
            ),
            outcome = LoopOutcome.COMPLETED,
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tool"),
        )
    }

    @Test
    fun `given out of order multi tool results, when mapping completed turn, then protocol is stripped`() {
        val firstCall = toolCall(id = "call_1")
        val secondCall = toolCall(id = "call_2")

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Run tools"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(firstCall, secondCall)),
                toolResult(secondCall.id),
                toolResult(firstCall.id),
            ),
            outcome = LoopOutcome.COMPLETED,
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tools"),
        )
    }

    @Test
    fun `given terminal outcomes, when mapping valid protocol, then no exchange is stored`() {
        val outcomes = listOf(LoopOutcome.FAILED, LoopOutcome.STOPPED, LoopOutcome.MAX_ITERATIONS)
        val toolCall = toolCall()

        outcomes.forEach { outcome ->
            val result = mapper.appendTurn(
                baseHistory = AssistantSessionHistory.Empty,
                modelTurnMessages = listOf(
                    AssistantMessage.User("Run tool"),
                    AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                    toolResult(toolCall.id),
                ),
                outcome = outcome,
            )

            assertThat(result.messages)
                .describedAs("outcome $outcome")
                .containsExactly(AssistantSessionMessage.User("Run tool"))
        }
    }

    @Test
    fun `given failed assistant message has content and tool calls, when mapping, then content is preserved as text`() {
        val toolCall = toolCall()

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Run tool"),
                AssistantMessage.Assistant(content = "I can check that.", toolCalls = listOf(toolCall)),
                toolResult(toolCall.id),
            ),
            outcome = LoopOutcome.FAILED,
            error = AssistantError.OutcomeUnknown(toolName = toolCall.name),
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tool"),
            AssistantSessionMessage.Assistant("I can check that."),
        )
    }

    private fun toolCall(
        id: String = "call_1",
        name: String = "orders_list",
    ) = ToolCall(
        id = id,
        name = name,
        arguments = buildJsonObject { },
    )

    private fun toolResult(
        toolCallId: String,
        content: String = """{"orders":[]}""",
    ) = AssistantMessage.Tool(toolCallId = toolCallId, content = content)
}
