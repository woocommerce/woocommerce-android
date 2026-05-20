package com.woocommerce.android.aiassistant.runtime

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.history.AssistantSessionHistory
import com.woocommerce.android.aiassistant.core.history.AssistantSessionMessage
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
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Update product"),
        )
    }

    @Test
    fun `given outcome unknown with matched pair, when mapping, then exchange is preserved`() {
        val toolCall = toolCall(name = "orders_update")
        val toolResult = AssistantMessage.Tool(
            toolCallId = toolCall.id,
            content = """{"error":"Tool execution failed"}""",
        )

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Cancel order"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                toolResult,
            ),
            error = AssistantError.OutcomeUnknown(toolName = "orders_update"),
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Cancel order"),
            AssistantSessionMessage.ToolExchange(
                assistantContent = null,
                toolCalls = listOf(toolCall),
                toolResults = listOf(toolResult),
            ),
        )
    }

    @Test
    fun `given confirmation cancellation, when mapping, then cancelled tool exchange is preserved`() {
        val toolCall = toolCall(name = "orders_update")
        val toolResult = AssistantMessage.Tool(
            toolCallId = toolCall.id,
            content = """{"error":"Action was cancelled"}""",
        )

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Cancel order"),
                AssistantMessage.Assistant(content = "I can do that.", toolCalls = listOf(toolCall)),
                toolResult,
            ),
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Cancel order"),
            AssistantSessionMessage.ToolExchange(
                assistantContent = "I can do that.",
                toolCalls = listOf(toolCall),
                toolResults = listOf(toolResult),
            ),
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
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Explain analytics"),
            AssistantSessionMessage.Assistant("Partial answer"),
        )
    }

    @Test
    fun `given invalid tool call, when mapping with matched pair, then exchange is preserved`() {
        val toolCall = toolCall()
        val toolResult = AssistantMessage.Tool(
            toolCallId = toolCall.id,
            content = """{"error":"Malformed arguments"}""",
        )

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Run tool"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                toolResult,
            ),
            error = AssistantError.InvalidToolCall(toolName = toolCall.name),
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tool"),
            AssistantSessionMessage.ToolExchange(
                assistantContent = null,
                toolCalls = listOf(toolCall),
                toolResults = listOf(toolResult),
            ),
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
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tools"),
        )
    }

    @Test
    fun `given STOPPED with Cancelled error and matched pair, when mapping, then exchange is stripped`() {
        val toolCall = toolCall()

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Run tool"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                toolResult(toolCall.id),
            ),
            error = AssistantError.Cancelled,
        )

        assertThat(result.messages).containsExactly(AssistantSessionMessage.User("Run tool"))
    }

    @Test
    fun `given Cancelled error with assistant content and matched pair, when mapping, then content is kept as text`() {
        val toolCall = toolCall()

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Run tool"),
                AssistantMessage.Assistant(content = "I can update that.", toolCalls = listOf(toolCall)),
                toolResult(toolCall.id),
            ),
            error = AssistantError.Cancelled,
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tool"),
            AssistantSessionMessage.Assistant("I can update that."),
        )
    }

    @Test
    fun `given MAX_ITERATIONS with matched pair, when mapping, then exchange is preserved`() {
        val toolCall = toolCall()
        val toolResult = toolResult(toolCall.id)

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Run tool"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                toolResult,
            ),
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tool"),
            AssistantSessionMessage.ToolExchange(
                assistantContent = null,
                toolCalls = listOf(toolCall),
                toolResults = listOf(toolResult),
            ),
        )
    }

    @Test
    fun `given failed assistant message has content and tool calls, when mapping with matched pair, then exchange retains assistant content`() {
        val toolCall = toolCall()
        val toolResult = toolResult(toolCall.id)

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Run tool"),
                AssistantMessage.Assistant(content = "I can check that.", toolCalls = listOf(toolCall)),
                toolResult,
            ),
            error = AssistantError.OutcomeUnknown(toolName = toolCall.name),
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tool"),
            AssistantSessionMessage.ToolExchange(
                assistantContent = "I can check that.",
                toolCalls = listOf(toolCall),
                toolResults = listOf(toolResult),
            ),
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
