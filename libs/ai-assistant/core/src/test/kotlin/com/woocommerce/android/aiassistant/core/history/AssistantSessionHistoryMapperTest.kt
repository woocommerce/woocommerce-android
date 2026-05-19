package com.woocommerce.android.aiassistant.core.history

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class AssistantSessionHistoryMapperTest {
    private val mapper = AssistantSessionHistoryMapper()

    @Test
    fun `given completed tool turn, when mapping, then only session safe text is appended`() {
        val toolCall = toolCall()

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Show orders"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                AssistantMessage.Tool(toolCallId = toolCall.id, content = """{"orders":[]}"""),
                AssistantMessage.Assistant(content = "Here are the orders"),
            ),
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Show orders"),
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
    fun `given unsafe outcome unknown, when mapping, then tool protocol is not reusable session history`() {
        val toolCall = toolCall(name = "orders_update")

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.User("Cancel order"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                AssistantMessage.Tool(toolCallId = toolCall.id, content = """{"error":"Tool execution failed"}"""),
            ),
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

        val result = mapper.appendTurn(
            baseHistory = AssistantSessionHistory.Empty,
            modelTurnMessages = listOf(
                AssistantMessage.System("system prompt"),
                AssistantMessage.User("Run tool"),
                AssistantMessage.Assistant(content = null, toolCalls = listOf(toolCall)),
                AssistantMessage.Tool(toolCallId = toolCall.id, content = """{"ok":true}"""),
                AssistantMessage.Assistant(content = "Done"),
            ),
        )

        assertThat(result.messages).containsExactly(
            AssistantSessionMessage.User("Run tool"),
            AssistantSessionMessage.Assistant("Done"),
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
}
