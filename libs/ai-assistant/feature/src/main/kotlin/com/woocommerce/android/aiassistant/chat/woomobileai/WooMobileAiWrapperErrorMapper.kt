package com.woocommerce.android.aiassistant.chat.woomobileai

import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseErrorMapper
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseHttpErrorContext
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseMappedError
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseStreamErrorContext
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.Diagnostics
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class WooMobileAiWrapperErrorMapper @Inject constructor(
    @AiAssistantJson private val json: Json,
) : OpenAiSseErrorMapper {
    override fun mapHttpError(
        context: OpenAiSseHttpErrorContext,
        diagnosticsFactory: TransportDiagnosticsFactory,
    ): OpenAiSseMappedError? = mapEnvelope(
        payload = context.body,
        fallbackHttpStatus = context.statusCode,
        headers = context.headers,
        diagnosticsFactory = diagnosticsFactory,
    )

    override fun mapStreamPayload(
        context: OpenAiSseStreamErrorContext,
        diagnosticsFactory: TransportDiagnosticsFactory,
    ): OpenAiSseMappedError? {
        if (!context.isFirstPayload) return null
        return mapEnvelope(
            payload = context.payload,
            fallbackHttpStatus = context.httpStatusCode,
            headers = context.headers,
            diagnosticsFactory = diagnosticsFactory,
        )
    }

    private fun mapEnvelope(
        payload: String,
        fallbackHttpStatus: Int?,
        headers: Map<String, String>,
        diagnosticsFactory: TransportDiagnosticsFactory,
    ): OpenAiSseMappedError? {
        val envelope = runCatching {
            json.decodeFromString<WrapperErrorEnvelope>(payload)
        }.getOrNull() ?: return null

        if (envelope.code == null && envelope.data?.status == null) return null

        val status = envelope.data?.status ?: fallbackHttpStatus
        val kind = envelope.code?.toKnownErrorKind()
            ?: status?.toHttpErrorKind()
            ?: return null

        return OpenAiSseMappedError(
            kind = kind,
            diagnostics = Diagnostics(
                transport = diagnosticsFactory.fromRawHttp(
                    statusCode = status ?: fallbackHttpStatus,
                    headers = headers,
                    bodyBytes = payload.encodeToByteArray(),
                )
            ),
        )
    }

    private fun String.toKnownErrorKind(): ChatStreamError? = when (this) {
        WOO_MOBILE_AI_USER_RATE_LIMIT -> ChatStreamError.RATE_LIMIT
        REST_UNAUTHORIZED, REST_FORBIDDEN -> ChatStreamError.AUTH
        else -> null
    }

    private fun Int.toHttpErrorKind(): ChatStreamError = when (this) {
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> ChatStreamError.AUTH
        HTTP_REQUEST_TIMEOUT -> ChatStreamError.TIMEOUT
        HTTP_TOO_MANY_REQUESTS -> ChatStreamError.RATE_LIMIT
        HTTP_BAD_REQUEST -> ChatStreamError.BAD_REQUEST
        in HTTP_CLIENT_ERROR_RANGE -> ChatStreamError.BAD_REQUEST
        in HTTP_SERVER_ERROR_RANGE -> ChatStreamError.UPSTREAM_FAILURE
        else -> ChatStreamError.UNKNOWN
    }

    private companion object {
        private const val WOO_MOBILE_AI_USER_RATE_LIMIT = "woo_mobile_ai_user_rate_limit"
        private const val REST_UNAUTHORIZED = "rest_unauthorized"
        private const val REST_FORBIDDEN = "rest_forbidden"

        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_BAD_REQUEST = 400
        private const val HTTP_REQUEST_TIMEOUT = 408
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val HTTP_CLIENT_ERROR_RANGE = 400..499
        private val HTTP_SERVER_ERROR_RANGE = 500..599
    }
}
