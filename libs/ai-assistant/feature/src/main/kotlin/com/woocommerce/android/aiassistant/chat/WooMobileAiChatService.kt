package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.auth.WpComOAuthTokenProvider
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseChatService
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseChatServiceConfig
import com.woocommerce.android.aiassistant.chat.openai.WpComOpenAiSseAuthProvider
import com.woocommerce.android.aiassistant.chat.woomobileai.WooMobileAiWrapperErrorMapper
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

/**
 * Wrapper chat service for the `woo-mobile-ai` endpoint.
 */
internal class WooMobileAiChatService @Inject constructor(
    @AssistantOkHttpClient httpClient: OkHttpClient,
    tokenProvider: WpComOAuthTokenProvider,
    streamParser: ChatStreamParser,
    @AiAssistantJson json: Json,
    @AssistantBaseUrl baseUrl: String,
    transportDiagnosticsFactory: TransportDiagnosticsFactory,
    wrapperErrorMapper: WooMobileAiWrapperErrorMapper,
) : ChatService {
    private val delegate = OpenAiSseChatService(
        httpClient = httpClient,
        streamParser = streamParser,
        json = json,
        baseUrl = baseUrl,
        transportDiagnosticsFactory = transportDiagnosticsFactory,
        config = OpenAiSseChatServiceConfig(
            path = WOO_MOBILE_AI_CHAT_COMPLETIONS_PATH,
            model = WOO_MOBILE_AI_MODEL_ID,
            includeUsage = true,
            authProvider = WpComOpenAiSseAuthProvider(tokenProvider),
            errorMappers = listOf(wrapperErrorMapper),
        ),
    )

    override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> =
        delegate.streamTurn(request)

    private companion object {
        private const val WOO_MOBILE_AI_CHAT_COMPLETIONS_PATH = "/wpcom/v2/woo-mobile-ai/chat/completions"
        private const val WOO_MOBILE_AI_MODEL_ID = "gpt-5.1"
    }
}
