package com.woocommerce.android.aiassistant.chat.openai

import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.Diagnostics

/**
 * Endpoint-specific error mapping hook for the shared SSE client.
 *
 * Generic HTTP/network failures are handled by [OpenAiSseChatService]. Mappers only translate backend-specific
 * HTTP bodies or SSE payloads, such as legacy Jetpack AI errors or WPCOM REST envelopes.
 */
internal interface OpenAiSseErrorMapper {
    fun mapHttpError(
        context: OpenAiSseHttpErrorContext,
        diagnosticsFactory: TransportDiagnosticsFactory,
    ): OpenAiSseMappedError? = null

    fun mapStreamPayload(
        context: OpenAiSseStreamErrorContext,
        diagnosticsFactory: TransportDiagnosticsFactory,
    ): OpenAiSseMappedError? = null
}

internal data class OpenAiSseHttpErrorContext(
    val statusCode: Int,
    val isSuccessful: Boolean,
    val isJson: Boolean,
    val body: String,
    val bodyBytes: ByteArray,
    val headers: Map<String, String>,
)

internal data class OpenAiSseStreamErrorContext(
    val payload: String,
    val isFirstPayload: Boolean,
    val httpStatusCode: Int?,
    val headers: Map<String, String>,
)

internal data class OpenAiSseMappedError(
    val kind: ChatStreamError,
    val diagnostics: Diagnostics = Diagnostics(),
    val retryableAuthFailure: Boolean = false,
)
