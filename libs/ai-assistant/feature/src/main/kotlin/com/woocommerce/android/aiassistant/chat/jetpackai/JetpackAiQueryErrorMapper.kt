package com.woocommerce.android.aiassistant.chat.jetpackai

import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseErrorMapper
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseHttpErrorContext
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseMappedError
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.Diagnostics
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class JetpackAiQueryErrorMapper(
    private val json: Json,
) : OpenAiSseErrorMapper {
    override fun mapHttpError(
        context: OpenAiSseHttpErrorContext,
        diagnosticsFactory: TransportDiagnosticsFactory,
    ): OpenAiSseMappedError? {
        val logicalStatus = runCatching {
            json.parseToJsonElement(context.body)
                .jsonObject["data"]
                ?.jsonObject
                ?.get(ERROR_STATUS_FIELD)
                ?.jsonPrimitive
                ?.contentOrNull
                ?.toIntOrNull()
        }.getOrNull() ?: return null

        return OpenAiSseMappedError(
            kind = logicalStatus.toHttpErrorKind(),
            retryableAuthFailure = logicalStatus == HTTP_UNAUTHORIZED,
            diagnostics = Diagnostics(
                transport = diagnosticsFactory.fromRawHttp(
                    statusCode = logicalStatus,
                    headers = context.headers,
                    bodyBytes = context.bodyBytes,
                )
            ),
        )
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
        private const val ERROR_STATUS_FIELD = "status"
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_BAD_REQUEST = 400
        private const val HTTP_REQUEST_TIMEOUT = 408
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val HTTP_CLIENT_ERROR_RANGE = 400..499
        private val HTTP_SERVER_ERROR_RANGE = 500..599
    }
}
