package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDefinition
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ChatCompletionPayloadsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        coerceInputValues = true
    }

    @Test
    fun `given a chat request, when converted to payload, then serializable transport fields are preserved`() {
        val payload = ChatCompletionRequestPayload.from(
            request = ChatRequest(
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
            ),
            feature = "woo-ai-assistant",
            model = "gpt-4o-mini",
        )

        val encoded = json.encodeToString(payload)
        val root = json.parseToJsonElement(encoded).jsonObject
        val messages = root.getValue("messages").jsonArray
        val assistantMessage = messages[2].jsonObject
        val toolMessage = messages[3].jsonObject
        val toolDefinition = root.getValue("tools").jsonArray.single().jsonObject

        assertThat(root.getValue("feature").jsonPrimitive.content).isEqualTo("woo-ai-assistant")
        assertThat(root.getValue("stream").jsonPrimitive.boolean).isTrue()
        assertThat(root.getValue("model").jsonPrimitive.content).isEqualTo("gpt-4o-mini")
        assertThat(assistantMessage.getValue("content")).isEqualTo(JsonNull)
        assertThat(
            assistantMessage.getValue("tool_calls").jsonArray.single().jsonObject
                .getValue("function").jsonObject
                .getValue("arguments").jsonPrimitive.content
        ).isEqualTo("{\"query\":\"shirt\"}")
        assertThat(toolMessage.getValue("tool_call_id").jsonPrimitive.content).isEqualTo("call_1")
        assertThat(toolDefinition.getValue("function").jsonObject.getValue("name").jsonPrimitive.content)
            .isEqualTo("lookup_products")
    }

    @Test
    fun `given a stream chunk payload, when decoded, then finish reason and tool-call deltas remain available`() {
        val chunk = this.json.decodeFromString<ChatCompletionStreamChunkPayload>(
            buildJsonObject {
                put(
                    "choices",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put(
                                    "delta",
                                    buildJsonObject {
                                        put("content", "Hi")
                                        put(
                                            "tool_calls",
                                            buildJsonArray {
                                                add(
                                                    buildJsonObject {
                                                        put("index", 0)
                                                        put("id", "call_1")
                                                        put(
                                                            "function",
                                                            buildJsonObject {
                                                                put("name", "show_cards")
                                                                put("arguments", "{\"a\":")
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                        )
                                    }
                                )
                                put("finish_reason", "tool_calls")
                            }
                        )
                    }
                )
            }.toString()
        )

        val choice = chunk.choices.single()
        val toolCall = choice.delta?.toolCalls?.single()

        assertThat(choice.finishReason).isEqualTo("tool_calls")
        assertThat(choice.delta?.content).isEqualTo("Hi")
        assertThat(toolCall?.index).isEqualTo(0)
        assertThat(toolCall?.id).isEqualTo("call_1")
        assertThat(toolCall?.function?.name).isEqualTo("show_cards")
        assertThat(toolCall?.function?.arguments).isEqualTo("{\"a\":")
    }
}
