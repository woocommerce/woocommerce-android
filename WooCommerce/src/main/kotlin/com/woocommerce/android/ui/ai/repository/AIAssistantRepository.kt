package com.woocommerce.android.ui.ai.repository

import com.woocommerce.android.ui.ai.mcp.McpToolMapper
import com.woocommerce.android.ui.ai.mcp.WooMcpClient
import com.woocommerce.android.ui.ai.model.AIAssistantEvent
import com.woocommerce.android.ui.ai.model.ChatMessage
import com.woocommerce.android.ui.ai.model.ChatMessage.Role
import com.woocommerce.android.ui.ai.model.FunctionCall
import com.woocommerce.android.ui.ai.model.ToolCall
import com.woocommerce.android.ui.ai.repository.AIApiProxyRestClient.ChatCompletionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIAssistantRepository @Inject constructor(
    private val aiApiProxyRestClient: AIApiProxyRestClient,
    private val mcpClient: WooMcpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun connectToStore(
        consumerKey: String,
        consumerSecret: String
    ): Result<Int> {
        return mcpClient.connect(consumerKey, consumerSecret)
            .mapCatching {
                val tools = mcpClient.discoverTools().getOrThrow()
                tools.size
            }
    }

    fun sendMessage(
        conversationHistory: List<ChatMessage>,
        userMessage: String
    ): Flow<AIAssistantEvent> = flow {
        val currentMessages = buildMessageList(conversationHistory, userMessage).toMutableList()
        val toolDefinitions = McpToolMapper.toOpenAITools(mcpClient.availableTools)

        var iteration = 0
        while (iteration < MAX_TOOL_CALL_ITERATIONS) {
            iteration++

            val result = aiApiProxyRestClient.sendChatCompletion(
                messages = currentMessages,
                tools = toolDefinitions
            )

            when (result) {
                is ChatCompletionResult.Error -> {
                    emit(AIAssistantEvent.Error(result.message))
                    return@flow
                }

                is ChatCompletionResult.Success -> {
                    if (result.finishReason == "tool_calls" && !result.toolCalls.isNullOrEmpty()) {
                        val toolResults = executeToolCalls(result.toolCalls) { event ->
                            emit(event)
                        }

                        currentMessages.add(
                            ChatMessage(
                                role = Role.ASSISTANT,
                                content = result.content,
                                toolCalls = result.toolCalls
                            )
                        )
                        currentMessages.addAll(toolResults)
                    } else {
                        emit(
                            AIAssistantEvent.FinalResponse(
                                fullText = result.content ?: "",
                                toolCalls = result.toolCalls
                            )
                        )
                        return@flow
                    }
                }
            }
        }

        emit(AIAssistantEvent.Error("Too many tool call iterations"))
    }

    private suspend fun executeToolCalls(
        toolCalls: List<ToolCall>,
        emitEvent: suspend (AIAssistantEvent) -> Unit
    ): List<ChatMessage> {
        return toolCalls.map { toolCall ->
            emitEvent(AIAssistantEvent.ToolCallStarted(toolCall.function.name))

            val arguments = parseArguments(toolCall.function.arguments)
            val toolResult = mcpClient.executeTool(toolCall.function.name, arguments)

            val resultText = toolResult.getOrElse { error ->
                "Error executing tool ${toolCall.function.name}: ${error.message}"
            }

            emitEvent(AIAssistantEvent.ToolCallCompleted(toolCall.function.name, resultText))

            ChatMessage(
                role = Role.TOOL,
                content = resultText,
                toolCallId = toolCall.id
            )
        }
    }

    private fun parseArguments(argumentsJson: String): Map<String, Any?> {
        return try {
            val jsonObject = json.decodeFromString<JsonObject>(argumentsJson)
            jsonObject.mapValues { (_, value) ->
                when (value) {
                    is JsonPrimitive -> when {
                        value.isString -> value.content
                        value.booleanOrNull != null -> value.booleanOrNull
                        value.longOrNull != null -> value.longOrNull
                        value.doubleOrNull != null -> value.doubleOrNull
                        else -> value.content
                    }
                    else -> value.toString()
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun buildMessageList(
        conversationHistory: List<ChatMessage>,
        userMessage: String
    ): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        if (conversationHistory.none { it.role == Role.SYSTEM }) {
            messages.add(
                ChatMessage(
                    role = Role.SYSTEM,
                    content = SYSTEM_PROMPT
                )
            )
        }

        messages.addAll(conversationHistory)

        messages.add(
            ChatMessage(
                role = Role.USER,
                content = userMessage
            )
        )

        return messages
    }

    suspend fun disconnect() {
        mcpClient.disconnect()
    }

    companion object {
        private const val MAX_TOOL_CALL_ITERATIONS = 5
        private const val SYSTEM_PROMPT = """You are a helpful WooCommerce store management assistant. \
You help merchants manage their online store by answering questions and performing actions.

When the user asks about their store, use the available tools to fetch real data. \
Be concise and helpful in your responses. When presenting data, use a clear format.

If a tool call fails, explain the error to the user and suggest what they might do to fix it."""
    }
}
