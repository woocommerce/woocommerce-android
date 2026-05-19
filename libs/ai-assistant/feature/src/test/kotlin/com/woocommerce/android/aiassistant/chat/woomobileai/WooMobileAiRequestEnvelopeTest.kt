package com.woocommerce.android.aiassistant.chat.woomobileai

import com.woocommerce.android.aiassistant.chat.assistantJsonForTests
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDefinition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooMobileAiRequestEnvelopeTest {
    private val json = assistantJsonForTests()
    private val builder = WooMobileAiRequestBuilder()

    @Test
    fun `given a chat request, when built, then wrapper top level contract is serialized`() {
        val envelope = builder.build(chatRequestWithMessagesAndTools())

        val encoded = json.encodeToString(envelope)
        val root = json.parseToJsonElement(encoded).jsonObject

        assertThat(root.getValue("model").jsonPrimitive.content).isEqualTo("gpt-5.1")
        assertThat(root.getValue("stream").jsonPrimitive.boolean).isTrue()
        assertThat(root.getValue("stream_options").jsonObject.getValue("include_usage").jsonPrimitive.boolean).isTrue()
        assertThat(root).doesNotContainKey("feature")
        assertThat(root).doesNotContainKey("tool_choice")
    }

    @Test
    fun `given messages and tools, when built, then existing open ai helper shapes are preserved`() {
        val envelope = builder.build(chatRequestWithMessagesAndTools())

        val encoded = json.encodeToString(envelope)
        val root = json.parseToJsonElement(encoded).jsonObject
        val messages = root.getValue("messages").jsonArray
        val assistantMessage = messages[2].jsonObject
        val toolMessage = messages[3].jsonObject
        val toolDefinition = root.getValue("tools").jsonArray.single().jsonObject

        assertThat(messages[0].jsonObject.getValue("role").jsonPrimitive.content).isEqualTo("system")
        assertThat(messages[1].jsonObject.getValue("role").jsonPrimitive.content).isEqualTo("user")
        assertThat(assistantMessage.getValue("role").jsonPrimitive.content).isEqualTo("assistant")
        assertThat(assistantMessage.getValue("content").jsonPrimitive.content).isEmpty()
        assertThat(
            assistantMessage.getValue("tool_calls").jsonArray.single().jsonObject
                .getValue("function").jsonObject
                .getValue("arguments").jsonPrimitive.content
        ).isEqualTo("{\"query\":\"shirt\"}")
        assertThat(toolMessage.getValue("role").jsonPrimitive.content).isEqualTo("tool")
        assertThat(toolMessage.getValue("tool_call_id").jsonPrimitive.content).isEqualTo("call_1")
        assertThat(toolDefinition.getValue("type").jsonPrimitive.content).isEqualTo("function")
        assertThat(toolDefinition.getValue("function").jsonObject.getValue("name").jsonPrimitive.content)
            .isEqualTo("lookup_products")
    }

    private fun chatRequestWithMessagesAndTools(): ChatRequest = ChatRequest(
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
    )
}
