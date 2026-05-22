package com.woocommerce.android.aiassistant.chat.jetpackai

import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory
import com.woocommerce.android.aiassistant.chat.assistantJsonForTests
import com.woocommerce.android.aiassistant.chat.openai.OpenAiSseHttpErrorContext
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JetpackAiQueryErrorMapperTest {
    private val mapper = JetpackAiQueryErrorMapper(assistantJsonForTests())
    private val diagnosticsFactory = TransportDiagnosticsFactory()

    @Test
    fun `given logical unauthorized status, when mapped, then auth error is retryable`() {
        val result = mapper.mapHttpError(contextWithLogicalStatus(401), diagnosticsFactory)

        assertThat(result?.kind).isEqualTo(ChatStreamError.AUTH)
        assertThat(result?.retryableAuthFailure).isTrue()
        assertThat(result?.diagnostics?.transport?.httpStatus).isEqualTo(401)
    }

    @Test
    fun `given logical rate limit status, when mapped, then rate limit diagnostics are preserved`() {
        val result = mapper.mapHttpError(
            contextWithLogicalStatus(
                status = 429,
                headers = mapOf(
                    "Retry-After" to "7",
                    "X-Request-Id" to "request-id",
                ),
            ),
            diagnosticsFactory,
        )

        assertThat(result?.kind).isEqualTo(ChatStreamError.RATE_LIMIT)
        assertThat(result?.retryableAuthFailure).isFalse()
        assertThat(result?.diagnostics?.transport?.httpStatus).isEqualTo(429)
        assertThat(result?.diagnostics?.transport?.retryAfterMs).isEqualTo(7_000L)
        assertThat(result?.diagnostics?.transport?.requestId).isEqualTo("request-id")
    }

    @Test
    fun `given logical server error status, when mapped, then upstream failure is returned`() {
        val result = mapper.mapHttpError(contextWithLogicalStatus(503), diagnosticsFactory)

        assertThat(result?.kind).isEqualTo(ChatStreamError.UPSTREAM_FAILURE)
        assertThat(result?.retryableAuthFailure).isFalse()
    }

    @Test
    fun `given body has no logical status, when mapped, then mapper does not own error`() {
        val result = mapper.mapHttpError(contextWithBody("""{"code":"invalid_json_schema"}"""), diagnosticsFactory)

        assertThat(result).isNull()
    }

    private fun contextWithLogicalStatus(
        status: Int,
        headers: Map<String, String> = emptyMap(),
    ): OpenAiSseHttpErrorContext {
        val body = """{"code":"rest_error","data":{"status":"$status"}}"""
        return contextWithBody(body, headers)
    }

    private fun contextWithBody(
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): OpenAiSseHttpErrorContext = OpenAiSseHttpErrorContext(
        statusCode = 200,
        isSuccessful = true,
        isJson = true,
        body = body,
        bodyBytes = body.encodeToByteArray(),
        headers = headers,
    )
}
