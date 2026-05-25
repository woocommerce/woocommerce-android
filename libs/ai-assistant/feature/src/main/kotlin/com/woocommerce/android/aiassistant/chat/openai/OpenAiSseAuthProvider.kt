package com.woocommerce.android.aiassistant.chat.openai

import com.woocommerce.android.aiassistant.auth.WpComOAuthTokenProvider

/**
 * Auth adapter used by the shared SSE client.
 */
internal interface OpenAiSseAuthProvider {
    suspend fun bearerToken(): String
}

internal class WpComOpenAiSseAuthProvider(
    private val tokenProvider: WpComOAuthTokenProvider,
) : OpenAiSseAuthProvider {
    override suspend fun bearerToken(): String = tokenProvider.provide()
}
