package com.woocommerce.android.aiassistant.chat.openai

import com.woocommerce.android.aiassistant.auth.WpComOAuthTokenProvider
import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider

/**
 * Auth adapter used by the shared SSE client.
 *
 * Implementations keep token source and invalidation semantics endpoint-specific: the Jetpack AI path can
 * invalidate a JWT before retrying, while the WPCOM bearer path simply provides the current access token.
 */
internal interface OpenAiSseAuthProvider {
    suspend fun bearerToken(): String
    suspend fun invalidate() = Unit
}

internal class JwtOpenAiSseAuthProvider(
    private val tokenProvider: JwtTokenProvider,
) : OpenAiSseAuthProvider {
    override suspend fun bearerToken(): String = tokenProvider.provide()

    override suspend fun invalidate() {
        tokenProvider.invalidate()
    }
}

internal class WpComOpenAiSseAuthProvider(
    private val tokenProvider: WpComOAuthTokenProvider,
) : OpenAiSseAuthProvider {
    override suspend fun bearerToken(): String = tokenProvider.provide()
}
