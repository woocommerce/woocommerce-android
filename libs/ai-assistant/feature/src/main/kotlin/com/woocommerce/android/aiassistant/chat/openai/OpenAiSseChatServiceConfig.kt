package com.woocommerce.android.aiassistant.chat.openai

/**
 * Endpoint-specific configuration for [OpenAiSseChatService].
 *
 * OpenAI-compatible endpoints can use the canonical request body as-is. Legacy or wrapper endpoints can
 * provide adapters for auth, top-level request shape, endpoint-specific error envelopes, and auth retry policy.
 */
internal data class OpenAiSseChatServiceConfig(
    val path: String,
    val includeUsage: Boolean = false,
    val authProvider: OpenAiSseAuthProvider,
    val requestBodyMapper: OpenAiRequestBodyMapper = IdentityOpenAiRequestBodyMapper,
    val errorMappers: List<OpenAiSseErrorMapper> = emptyList(),
    val retryOnUnauthorizedBeforeOutput: Boolean = false,
)
