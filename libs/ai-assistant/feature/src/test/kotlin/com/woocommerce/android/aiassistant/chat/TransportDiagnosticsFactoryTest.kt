package com.woocommerce.android.aiassistant.chat

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.BufferedSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.IOException
import java.time.Instant

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

    @Test
    fun `given retry after seconds header, when building diagnostics, then retry delay is captured`() {
        val diagnostics = factory.from(
            response(headers = mapOf("Retry-After" to "3")),
            nowMillis = FIXED_NOW_MS,
        )

        assertThat(diagnostics?.retryAfterMs).isEqualTo(3_000L)
    }

    @Test
    fun `given retry after http date header, when building diagnostics, then retry delay is captured`() {
        val diagnostics = factory.from(
            response(headers = mapOf("Retry-After" to "Sun, 10 May 2026 17:43:30 GMT")),
            nowMillis = FIXED_NOW_MS,
        )

        assertThat(diagnostics?.retryAfterMs).isEqualTo(30_000L)
    }

    @Test
    fun `given retry after exceeds cap, when building diagnostics, then retry delay is capped`() {
        val diagnostics = factory.from(
            response(headers = mapOf("Retry-After" to "600")),
            nowMillis = FIXED_NOW_MS,
        )

        assertThat(diagnostics?.retryAfterMs).isEqualTo(TransportDiagnosticsFactory.MAX_RETRY_AFTER_MS)
    }

    @Test
    fun `given malformed retry after header, when building diagnostics, then retry delay is absent`() {
        val diagnostics = factory.from(
            response(headers = mapOf("Retry-After" to "later")),
            nowMillis = FIXED_NOW_MS,
        )

        assertThat(diagnostics?.retryAfterMs).isNull()
    }

    @Test
    fun `given expired retry after http date header, when building diagnostics, then retry delay is absent`() {
        val diagnostics = factory.from(
            response(headers = mapOf("Retry-After" to "Sun, 10 May 2026 17:42:30 GMT")),
            nowMillis = FIXED_NOW_MS,
        )

        assertThat(diagnostics?.retryAfterMs).isNull()
    }

    @Test
    fun `given sensitive body values, when building diagnostics, then snippet is redacted`() {
        val diagnostics = factory.from(
            response(
                body = """
                    Authorization: Bearer secret-token
                    Cookie: session=secret-cookie
                    Set-Cookie: wp=secret-set-cookie
                    {"token":"Bearer another-secret"}
                """.trimIndent()
            ),
        )

        assertThat(diagnostics?.bodySnippet).contains("[REDACTED]")
        assertThat(diagnostics?.bodySnippet).doesNotContain("secret-token")
        assertThat(diagnostics?.bodySnippet).doesNotContain("secret-cookie")
        assertThat(diagnostics?.bodySnippet).doesNotContain("secret-set-cookie")
        assertThat(diagnostics?.bodySnippet).doesNotContain("another-secret")
    }

    @Test
    fun `given oversized body, when building diagnostics, then snippet is capped`() {
        val diagnostics = factory.from(response(body = "x".repeat(5_000)))

        assertThat(diagnostics?.bodySnippet).hasSize(TransportDiagnosticsFactory.MAX_BODY_SNIPPET_CHARS)
    }

    @Test
    fun `given empty body, when building diagnostics, then snippet is absent`() {
        val diagnostics = factory.from(response(body = ""))

        assertThat(diagnostics?.bodySnippet).isNull()
    }

    @Test
    fun `given unreadable body, when building diagnostics, then other diagnostics are preserved`() {
        val diagnostics = factory.from(
            responseWithBody(
                headers = mapOf("X-Request-Id" to "request-123"),
                body = ThrowingResponseBody,
            ),
        )

        assertThat(diagnostics?.httpStatus).isEqualTo(400)
        assertThat(diagnostics?.requestId).isEqualTo("request-123")
        assertThat(diagnostics?.bodySnippet).isNull()
    }

    private fun response(
        code: Int = 400,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): Response = responseWithBody(
        code = code,
        headers = headers,
        body = body?.toResponseBody(),
    )

    private fun responseWithBody(
        code: Int = 400,
        headers: Map<String, String> = emptyMap(),
        body: ResponseBody? = null,
    ): Response {
        val builder = Response.Builder()
            .request(Request.Builder().url("https://example.com/wpcom/v2/jetpack-ai-query").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Bad Request")
        headers.forEach { (name, value) -> builder.header(name, value) }
        body?.let(builder::body)
        return builder.build()
    }

    private object ThrowingResponseBody : ResponseBody() {
        override fun contentType() = null

        override fun contentLength(): Long = -1L

        override fun source(): BufferedSource {
            throw IOException("closed")
        }
    }

    private companion object {
        val FIXED_NOW_MS: Long = Instant.parse("2026-05-10T17:43:00Z").toEpochMilli()
    }
}
