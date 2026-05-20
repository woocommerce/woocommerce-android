package com.woocommerce.android.aiassistant.chat.openai

import com.woocommerce.android.aiassistant.chat.assistantJsonForTests
import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDefinition
import com.woocommerce.android.aiassistant.core.history.AssistantSessionHistory
import com.woocommerce.android.aiassistant.core.history.AssistantSessionMessage
import com.woocommerce.android.aiassistant.core.loop.BudgetedHistory
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.runtime.ModelRequestHistoryBuilder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class OpenAiMappingTest {
    private val json = assistantJsonForTests()

    @Test
    fun `given a chat request, when mapped to open ai, then serialized fields match the transport contract`() {
        val payload = ChatRequest(
            messages = listOf(
                AssistantMessage.System("You are helpful"),
                AssistantMessage.User("Find shirts"),
                AssistantMessage.Assistant(
                    content = null,
                    toolCalls = listOf(
                        ToolCall(
                            id = "call_1",
                            name = "lookup_products",
                            arguments = buildJsonObject {
                                put("query", "shirt")
                            },
                        )
                    ),
                ),
                AssistantMessage.Tool(
                    toolCallId = "call_1",
                    content = "[{\"id\":1}]",
                ),
            ),
            tools = listOf(
                ToolDefinition(
                    name = "lookup_products",
                    description = "List products matching a query",
                    parameters = buildJsonObject {
                        put("type", "object")
                    },
                )
            ),
        ).toOpenAi()

        val encoded = json.encodeToString(payload)
        val root = json.parseToJsonElement(encoded).jsonObject
        val messages = root.getValue("messages").jsonArray
        val systemMessage = messages[0].jsonObject
        val userMessage = messages[1].jsonObject
        val assistantMessage = messages[2].jsonObject
        val toolMessage = messages[3].jsonObject
        val toolDefinition = root.getValue("tools").jsonArray.single().jsonObject

        assertThat(root.getValue("feature").jsonPrimitive.content).isEqualTo(AssistantConfig.FEATURE_NAME)
        assertThat(root.getValue("stream").jsonPrimitive.boolean).isTrue()
        assertThat(root.getValue("model").jsonPrimitive.content).isEqualTo(AssistantConfig.MODEL_ID)
        assertThat(systemMessage.getValue("role").jsonPrimitive.content).isEqualTo("system")
        assertThat(userMessage.getValue("role").jsonPrimitive.content).isEqualTo("user")
        assertThat(assistantMessage.getValue("role").jsonPrimitive.content).isEqualTo("assistant")
        assertThat(assistantMessage.getValue("content").jsonPrimitive.content).isEmpty()
        assertThat(
            assistantMessage.getValue("tool_calls").jsonArray.single().jsonObject
                .getValue("function").jsonObject
                .getValue("arguments").jsonPrimitive.content
        ).isEqualTo("{\"query\":\"shirt\"}")
        assertThat(toolMessage.getValue("role").jsonPrimitive.content).isEqualTo("tool")
        assertThat(toolMessage.getValue("tool_call_id").jsonPrimitive.content).isEqualTo("call_1")
        assertThat(toolDefinition.getValue("function").jsonObject.getValue("name").jsonPrimitive.content)
            .isEqualTo("lookup_products")
    }

    @Test
    fun `given a stream chunk, when mapped to assistant events, then content tool calls and finish reason are preserved`() {
        val events = OpenAiStreamChunk(
            choices = listOf(
                OpenAiChoice(
                    delta = OpenAiDelta(
                        content = "Hi",
                        toolCalls = listOf(
                            OpenAiToolCallDelta(
                                index = 0,
                                id = "call_1",
                                function = OpenAiFunctionCallDelta(
                                    name = "show_cards",
                                    arguments = "{\"a\":",
                                ),
                            )
                        ),
                    ),
                    finishReason = "tool_calls",
                )
            )
        ).toEvents()

        assertThat(events).containsExactly(
            AssistantEvent.TextDelta("Hi"),
            AssistantEvent.ToolCallDelta(
                index = 0,
                id = "call_1",
                name = "show_cards",
                argumentsDelta = "{\"a\":",
            ),
            AssistantEvent.Finish(FinishReason.TOOL_CALLS),
        )
    }

    @Test
    fun `given replayed session tool exchange, when mapped to open ai, then assistant content is empty string`() {
        val toolCall = ToolCall(
            id = "call_1",
            name = "lookup_products",
            arguments = buildJsonObject {
                put("query", "shirt")
            },
        )
        val toolResult = AssistantMessage.Tool(toolCallId = toolCall.id, content = "[{\"id\":1}]")
        val sessionHistory = AssistantSessionHistory(
            messages = listOf(
                AssistantSessionMessage.User("Find shirts"),
                AssistantSessionMessage.ToolExchange(
                    assistantContent = null,
                    toolCalls = listOf(toolCall),
                    toolResults = listOf(toolResult),
                ),
            )
        )
        val modelHistory = ModelRequestHistoryBuilder(passThroughBudgeter()).build(
            systemPrompt = "You are helpful",
            sessionHistory = sessionHistory,
            currentUserMessage = "Any blue ones?",
        )

        val payload = ChatRequest(
            messages = modelHistory.messages,
            tools = emptyList(),
        ).toOpenAi()

        val encoded = json.encodeToString(payload)
        val messages = json.parseToJsonElement(encoded).jsonObject.getValue("messages").jsonArray
        val assistantMessage = messages[2].jsonObject
        val toolMessage = messages[3].jsonObject
        assertThat(assistantMessage.getValue("content").jsonPrimitive.content).isEmpty()
        val serializedToolCallId = assistantMessage
            .getValue("tool_calls")
            .jsonArray
            .single()
            .jsonObject
            .getValue("id")
            .jsonPrimitive
            .content
        assertThat(
            serializedToolCallId,
        ).isEqualTo(toolCall.id)
        assertThat(toolMessage.getValue("tool_call_id").jsonPrimitive.content).isEqualTo(toolCall.id)
    }

    private fun passThroughBudgeter() = HistoryBudgeter { systemPrompt, rawTranscript, currentUserTurn ->
        BudgetedHistory(
            messages = listOf(systemPrompt) + rawTranscript + currentUserTurn,
            retainedEntityRefs = emptyList(),
        )
    }
}
