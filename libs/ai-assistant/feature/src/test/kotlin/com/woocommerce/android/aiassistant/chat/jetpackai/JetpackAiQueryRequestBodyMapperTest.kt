package com.woocommerce.android.aiassistant.chat.jetpackai

import com.woocommerce.android.aiassistant.chat.assistantJsonForTests
import com.woocommerce.android.aiassistant.chat.openai.toOpenAiRequestBody
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDefinition
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JetpackAiQueryRequestBodyMapperTest {
    private val json = assistantJsonForTests()
    private val mapper = JetpackAiQueryRequestBodyMapper(featureName = "ai-assistant")

    @Test
    fun `given canonical open ai body, when mapped, then legacy feature request shape is serialized`() {
        val canonical = chatRequestWithMessagesAndTools()
            .toOpenAiRequestBody(model = "gpt-legacy", includeUsage = true)

        val encoded = mapper.mapToJson(canonical, json)
        val root = json.parseToJsonElement(encoded).jsonObject

        assertThat(root.getValue("feature").jsonPrimitive.content).isEqualTo("ai-assistant")
        assertThat(root.getValue("model").jsonPrimitive.content).isEqualTo("gpt-legacy")
        assertThat(root.getValue("stream").jsonPrimitive.boolean).isTrue()
        assertThat(root).doesNotContainKey("stream_options")
        assertThat(root).doesNotContainKey("tool_choice")
    }

    @Test
    fun `given canonical messages and tools, when mapped, then open ai helper shapes are preserved`() {
        val canonical = chatRequestWithMessagesAndTools()
            .toOpenAiRequestBody(model = "gpt-legacy")

        val encoded = mapper.mapToJson(canonical, json)
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
