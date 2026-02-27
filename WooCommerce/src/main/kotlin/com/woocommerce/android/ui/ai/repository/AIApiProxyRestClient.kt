package com.woocommerce.android.ui.ai.repository

import com.woocommerce.android.ui.ai.mcp.OpenAIFunctionSchema
import com.woocommerce.android.ui.ai.mcp.OpenAIToolDefinition
import com.woocommerce.android.ui.ai.model.ChatMessage
import com.woocommerce.android.ui.ai.model.FunctionCall
import com.woocommerce.android.ui.ai.model.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AIApiProxyRestClient @Inject constructor(
    @Named("regular") private val okHttpClient: OkHttpClient,
    private val accessToken: AccessToken
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun sendChatCompletion(
        messages: List<ChatMessage>,
        tools: List<OpenAIToolDefinition>,
        model: String = DEFAULT_MODEL
    ): ChatCompletionResult = withContext(Dispatchers.IO) {
        val requestBody = ChatCompletionRequest(
            model = model,
            messages = messages.map { it.toApiMessage() },
            tools = tools.takeIf { it.isNotEmpty() }?.map { it.toApiTool() }
        )

        val jsonBody = json.encodeToString(requestBody)
        val request = Request.Builder()
            .url(ENDPOINT_URL)
            .addHeader("Authorization", "Bearer ${accessToken.get()}")
            .addHeader(AI_FEATURE_HEADER, AI_FEATURE_NAME)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body.use { it.string() }
                return@withContext ChatCompletionResult.Error("HTTP ${response.code}: $errorBody")
            }

            val responseBody = response.body.use { it.string() }

            val chatResponse = json.decodeFromString<ChatCompletionResponse>(responseBody)
            val choice = chatResponse.choices.firstOrNull()
                ?: return@withContext ChatCompletionResult.Error("No choices in response")

            val message = choice.message
            ChatCompletionResult.Success(
                content = message.content,
                toolCalls = message.toolCalls?.map { tc ->
                    ToolCall(
                        id = tc.id,
                        function = FunctionCall(
                            name = tc.function.name,
                            arguments = tc.function.arguments
                        )
                    )
                },
                finishReason = choice.finishReason
            )
        } catch (e: Exception) {
            ChatCompletionResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun ChatMessage.toApiMessage(): ApiMessage {
        return ApiMessage(
            role = role.name.lowercase(),
            content = content,
            toolCalls = toolCalls?.map { tc ->
                ApiToolCall(
                    id = tc.id,
                    function = ApiFunctionCall(
                        name = tc.function.name,
                        arguments = tc.function.arguments
                    )
                )
            },
            toolCallId = toolCallId
        )
    }

    private fun OpenAIToolDefinition.toApiTool(): ApiToolDefinition {
        return ApiToolDefinition(
            function = ApiFunctionSchema(
                name = function.name,
                description = function.description,
                parameters = function.parameters
            )
        )
    }

    sealed class ChatCompletionResult {
        data class Success(
            val content: String?,
            val toolCalls: List<ToolCall>?,
            val finishReason: String?
        ) : ChatCompletionResult()

        data class Error(val message: String) : ChatCompletionResult()
    }

    // region API models
    @Serializable
    private data class ChatCompletionRequest(
        val model: String,
        val messages: List<ApiMessage>,
        val tools: List<ApiToolDefinition>? = null,
        val stream: Boolean = false
    )

    @Serializable
    private data class ApiMessage(
        val role: String,
        val content: String? = null,
        @SerialName("tool_calls")
        val toolCalls: List<ApiToolCall>? = null,
        @SerialName("tool_call_id")
        val toolCallId: String? = null
    )

    @Serializable
    private data class ApiToolCall(
        val id: String,
        val type: String = "function",
        val function: ApiFunctionCall
    )

    @Serializable
    private data class ApiFunctionCall(
        val name: String,
        val arguments: String
    )

    @Serializable
    private data class ApiToolDefinition(
        val type: String = "function",
        val function: ApiFunctionSchema
    )

    @Serializable
    private data class ApiFunctionSchema(
        val name: String,
        val description: String,
        val parameters: JsonObject
    )

    @Serializable
    private data class ChatCompletionResponse(
        val choices: List<ApiChoice>
    )

    @Serializable
    private data class ApiChoice(
        val message: ApiResponseMessage,
        @SerialName("finish_reason")
        val finishReason: String? = null
    )

    @Serializable
    private data class ApiResponseMessage(
        val role: String? = null,
        val content: String? = null,
        @SerialName("tool_calls")
        val toolCalls: List<ApiToolCall>? = null
    )
    // endregion

    companion object {
        private const val ENDPOINT_URL =
            "https://public-api.wordpress.com/wpcom/v2/ai-api-proxy/v1/chat/completions"
        private const val AI_FEATURE_HEADER = "X-WPCOM-AI-Feature"
        private const val AI_FEATURE_NAME = "woo_android_ai_assistant"
        private const val DEFAULT_MODEL = "gpt-4o"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
