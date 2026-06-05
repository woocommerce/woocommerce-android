package com.woocommerce.android.aiassistant.chat.openai

/**
 * Endpoint-specific configuration for [OpenAiSseChatService].
 *
 * OpenAI-compatible endpoints can use the canonical request body as-is. Wrapper endpoints can provide adapters
 * for auth, top-level request shape, and endpoint-specific error envelopes.
 */
internal data class OpenAiSseChatServiceConfig(
    val path: String,
    val includeUsage: Boolean = false,
    val authProvider: OpenAiSseAuthProvider,
    val requestBodyMapper: OpenAiRequestBodyMapper = IdentityOpenAiRequestBodyMapper,
    val errorMappers: List<OpenAiSseErrorMapper> = emptyList(),
)
