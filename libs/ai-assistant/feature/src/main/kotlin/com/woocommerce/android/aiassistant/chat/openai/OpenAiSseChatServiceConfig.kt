package com.woocommerce.android.aiassistant.chat.openai

internal data class OpenAiSseChatServiceConfig(
    val path: String,
    val model: String,
    val includeUsage: Boolean = false,
    val authProvider: OpenAiSseAuthProvider,
    val requestBodyMapper: OpenAiRequestBodyMapper = IdentityOpenAiRequestBodyMapper,
    val errorMappers: List<OpenAiSseErrorMapper> = emptyList(),
    val retryOnUnauthorizedBeforeOutput: Boolean = false,
)
