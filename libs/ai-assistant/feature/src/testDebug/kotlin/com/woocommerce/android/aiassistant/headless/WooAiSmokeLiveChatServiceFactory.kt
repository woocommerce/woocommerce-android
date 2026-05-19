package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.chat.ChatStreamParser
import com.woocommerce.android.aiassistant.chat.JetpackAiChatService
import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory
import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider
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
) {
    fun createTokenProvider(
        credentials: WooAiSmokeCredentialConfig,
        redactor: WooAiSmokeRedactor,
    ): JwtTokenProvider = WooAiSmokeDirectJwtTokenProvider(
        httpClient = httpClient,
        json = json,
        siteUrl = credentials.siteUrl,
        username = credentials.username,
        appPassword = credentials.appPassword,
        redactor = redactor,
    )

    fun create(
        credentials: WooAiSmokeCredentialConfig,
        redactor: WooAiSmokeRedactor,
    ): ChatService = JetpackAiChatService(
        httpClient = httpClient,
        tokenProvider = createTokenProvider(credentials, redactor),
        streamParser = streamParser,
        json = json,
        baseUrl = baseUrl,
        transportDiagnosticsFactory = transportDiagnosticsFactory,
    )
}
