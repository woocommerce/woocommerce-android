package com.woocommerce.android.ui.ai.repository

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.ai.mcp.McpToolMapper
import com.woocommerce.android.ui.ai.mcp.WooMcpClient
import com.woocommerce.android.ui.ai.model.AIAssistantEvent
import com.woocommerce.android.ui.ai.model.ChatMessage
import com.woocommerce.android.ui.ai.model.ChatMessage.Role
import com.woocommerce.android.ui.ai.model.ToolCall
import com.woocommerce.android.ui.ai.repository.JetpackAIQueryRestClient.ChatCompletionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.wordpress.android.fluxc.model.JWTToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAIJWTTokenResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIAssistantRepository @Inject constructor(
    private val jetpackAIQueryRestClient: JetpackAIQueryRestClient,
    private val jetpackAIRestClient: JetpackAIRestClient,
    private val selectedSite: SelectedSite,
    private val mcpClient: WooMcpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val tokenMutex = Mutex()
    private var jwtToken: JWTToken? = null

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

            val token = getValidJwtToken()
            if (token == null) {
                emit(AIAssistantEvent.Error("Failed to obtain authentication token"))
                return@flow
            }

            val result = jetpackAIQueryRestClient.sendChatCompletion(
                jwtToken = token.value,
                messages = currentMessages,
                tools = toolDefinitions
            )

            when (result) {
                is ChatCompletionResult.Error -> {
                    if (result.isAuthError) {
                        // Clear cached token so next iteration fetches a fresh one
                        invalidateJwtToken()
                        continue
                    }
                    emit(AIAssistantEvent.Error(result.message))
                    return@flow
                }

                is ChatCompletionResult.Success -> {
                    val shouldContinue = handleSuccess(result, currentMessages) { event ->
                        emit(event)
                    }
                    if (!shouldContinue) return@flow
                }
            }
        }

        emit(AIAssistantEvent.Error("Too many tool call iterations"))
    }

    /**
     * Handles a successful chat completion result.
     * @return true if the tool-call loop should continue, false if a final response was emitted.
     */
    private suspend fun handleSuccess(
        result: ChatCompletionResult.Success,
        currentMessages: MutableList<ChatMessage>,
        emitEvent: suspend (AIAssistantEvent) -> Unit
    ): Boolean {
        return if (result.finishReason == "tool_calls" && !result.toolCalls.isNullOrEmpty()) {
            val toolResults = executeToolCalls(result.toolCalls, emitEvent)

            currentMessages.add(
                ChatMessage(
                    role = Role.ASSISTANT,
                    content = result.content,
                    toolCalls = result.toolCalls
                )
            )
            currentMessages.addAll(toolResults)
            true
        } else {
            emitEvent(
                AIAssistantEvent.FinalResponse(
                    fullText = result.content ?: "",
                    toolCalls = result.toolCalls
                )
            )
            false
        }
    }

    private suspend fun getValidJwtToken(): JWTToken? = tokenMutex.withLock {
        val site = selectedSite.getOrNull() ?: return@withLock null
        val siteId = site.siteId

        // Return cached token if still valid for this site
        jwtToken?.validateExpiryDate()?.let { validToken ->
            if (validToken.getPayloadItem("blog_id")?.toLongOrNull() == siteId) {
                return@withLock validToken
            }
        }

        // Fetch a new JWT token
        when (val response = jetpackAIRestClient.fetchJetpackAIJWTToken(site)) {
            is JetpackAIJWTTokenResponse.Success -> {
                jwtToken = response.token
                response.token
            }
            is JetpackAIJWTTokenResponse.Error -> {
                jwtToken = null
                null
            }
        }
    }

    private suspend fun invalidateJwtToken() = tokenMutex.withLock {
        jwtToken = null
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
            jsonObject.mapValues { (_, value) -> jsonElementToKotlin(value) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun jsonElementToKotlin(element: JsonElement): Any? {
        return when (element) {
            is JsonPrimitive -> when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.booleanOrNull
                element.longOrNull != null -> element.longOrNull
                element.doubleOrNull != null -> element.doubleOrNull
                else -> element.content
            }
            is JsonObject -> element.mapValues { (_, v) -> jsonElementToKotlin(v) }
            is JsonArray -> element.map { jsonElementToKotlin(it) }
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
        invalidateJwtToken()
        mcpClient.disconnect()
    }

    companion object {
        private const val MAX_TOOL_CALL_ITERATIONS = 5
        private const val SYSTEM_PROMPT = """You are a helpful WooCommerce store management assistant. \
You help merchants manage their online store by answering questions and performing actions.

When the user asks about their store, use the available tools to fetch real data. \
Be concise and helpful in your responses. When presenting data, use a clear format.

If a tool call fails, explain the error to the user and suggest what they might do to fix it.

IMPORTANT: When presenting order data, you MUST include a structured JSON block using this exact format:

```json:orders
[{"id": 123, "number": "#123", "status": "processing", "customer_name": "John Doe", "total": "$59.99", "date": "Mar 15, 2025"}]
```

Rules for order JSON blocks:
- The "status" field must be the raw status value (e.g., "processing", "completed", "on-hold", "failed", "pending", "cancelled", "refunded").
- The "number" field should include the "#" prefix.
- The "total" field should include the currency symbol.
- The "date" field should be a human-readable date string.
- You may include text before and after the JSON block to provide context.
- Each JSON block must contain an array of order objects, even for a single order.

IMPORTANT: When presenting product data, you MUST include a structured JSON block using this exact format:

```json:products
[{"id": 456, "name": "Blue T-Shirt", "price": "${'$'}29.99", "status": "publish", "stock_status": "instock", "image_url": "https://example.com/image.jpg"}]
```

Rules for product JSON blocks:
- The "status" field must be the raw status value (e.g., "publish", "draft", "pending", "private").
- The "price" field should include the currency symbol.
- The "stock_status" field must be the raw value (e.g., "instock", "outofstock", "onbackorder").
- The "image_url" field is optional and should be the URL of the first product image if available.
- You may include text before and after the JSON block to provide context.
- Each JSON block must contain an array of product objects, even for a single product."""
    }
}
