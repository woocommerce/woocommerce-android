package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.auth.WpComOAuthTokenProvider
import com.woocommerce.android.aiassistant.chat.ChatStreamParser
import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory
import com.woocommerce.android.aiassistant.chat.WooMobileAiChatService
import com.woocommerce.android.aiassistant.chat.woomobileai.WooMobileAiWrapperErrorMapper
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.di.AssistantBaseUrl
import com.woocommerce.android.aiassistant.di.AssistantOkHttpClient
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Inject

internal class WooAiSmokeLiveChatServiceFactory @Inject constructor(
    @AssistantOkHttpClient private val httpClient: OkHttpClient,
    private val streamParser: ChatStreamParser,
    @AiAssistantJson private val json: Json,
    @AssistantBaseUrl private val baseUrl: String,
    private val transportDiagnosticsFactory: TransportDiagnosticsFactory,
    private val tokenProvider: WpComOAuthTokenProvider,
    private val wrapperErrorMapper: WooMobileAiWrapperErrorMapper,
) {
    @Suppress("UNUSED_PARAMETER")
    fun create(
        credentials: WooAiSmokeCredentialConfig,
        redactor: WooAiSmokeRedactor,
    ): ChatService = WooMobileAiChatService(
        httpClient = httpClient,
        tokenProvider = tokenProvider,
        streamParser = streamParser,
        json = json,
        baseUrl = baseUrl,
        transportDiagnosticsFactory = transportDiagnosticsFactory,
        wrapperErrorMapper = wrapperErrorMapper,
    )
}
