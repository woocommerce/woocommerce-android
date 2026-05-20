package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.chat.jetpackai.JetpackAiQueryErrorMapper
import com.woocommerce.android.aiassistant.chat.jetpackai.JetpackAiQueryRequestBodyMapper
import com.woocommerce.android.aiassistant.chat.openai.JwtOpenAiSseAuthProvider
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseChatService
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseChatServiceConfig
import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.di.AssistantBaseUrl
import com.woocommerce.android.aiassistant.di.AssistantOkHttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Active [ChatService] for the legacy `jetpack-ai-query` SSE endpoint.
 *
 * The endpoint still uses Jetpack AI JWT auth and retries one 401 received before
 * any model output by invalidating the cached JWT through [JwtTokenProvider].
 */
@Singleton
internal class JetpackAiChatService @Inject constructor(
    @AssistantOkHttpClient httpClient: OkHttpClient,
    tokenProvider: JwtTokenProvider,
    streamParser: ChatStreamParser,
    @AiAssistantJson json: Json,
    @AssistantBaseUrl baseUrl: String,
    transportDiagnosticsFactory: TransportDiagnosticsFactory,
) : ChatService {
    private val delegate = OpenAiSseChatService(
        httpClient = httpClient,
        streamParser = streamParser,
        json = json,
        baseUrl = baseUrl,
        transportDiagnosticsFactory = transportDiagnosticsFactory,
        config = OpenAiSseChatServiceConfig(
            path = JETPACK_AI_QUERY_PATH,
            authProvider = JwtOpenAiSseAuthProvider(tokenProvider),
            requestBodyMapper = JetpackAiQueryRequestBodyMapper(AssistantConfig.FEATURE_NAME),
            errorMappers = listOf(JetpackAiQueryErrorMapper(json)),
            retryOnUnauthorizedBeforeOutput = true,
        ),
    )

    override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> =
        delegate.streamTurn(request)

    companion object {
        internal const val DEFAULT_BASE_URL = "https://public-api.wordpress.com"
        private const val JETPACK_AI_QUERY_PATH = "/wpcom/v2/jetpack-ai-query"
    }
}
