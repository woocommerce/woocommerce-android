package com.woocommerce.android.aiassistant.chat

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TransportDiagnosticsFactoryTest {
    private val factory = TransportDiagnosticsFactory()

    @Test
    fun `given allowlisted request id header, when building diagnostics, then request id is captured`() {
        val diagnostics = factory.from(
            response(headers = mapOf("X-WP-Request-ID" to "request-123"))
        )

        assertThat(diagnostics?.requestId).isEqualTo("request-123")
    }

    @Test
    fun `given unrelated tracing header, when building diagnostics, then request id is ignored`() {
        val diagnostics = factory.from(
            response(headers = mapOf("X-Amzn-Trace-Id" to "trace-123"))
        )

        assertThat(diagnostics?.requestId).isNull()
    }

    private fun response(
        code: Int = 400,
        headers: Map<String, String> = emptyMap(),
    ): Response {
        val builder = Response.Builder()
            .request(Request.Builder().url("https://example.com/wpcom/v2/jetpack-ai-query").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Bad Request")
        headers.forEach { (name, value) -> builder.header(name, value) }
        return builder.build()
    }
}
