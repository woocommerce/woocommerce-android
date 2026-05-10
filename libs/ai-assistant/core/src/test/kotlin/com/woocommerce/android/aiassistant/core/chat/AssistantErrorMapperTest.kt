package com.woocommerce.android.aiassistant.core.chat

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantErrorMapperTest {
    @Test
    fun `given ChatStreamError NETWORK, when widened, then returns Network`() {
        assertThat(ChatStreamError.NETWORK.toAssistantError()).isEqualTo(AssistantError.Network())
    }

    @Test
    fun `given ChatStreamError TIMEOUT, when widened, then returns Timeout`() {
        assertThat(ChatStreamError.TIMEOUT.toAssistantError()).isEqualTo(AssistantError.Timeout())
    }

    @Test
    fun `given ChatStreamError AUTH, when widened, then returns Auth`() {
        assertThat(ChatStreamError.AUTH.toAssistantError()).isEqualTo(AssistantError.Auth())
    }

    @Test
    fun `given ChatStreamError RATE_LIMIT, when widened, then returns RateLimit`() {
        assertThat(ChatStreamError.RATE_LIMIT.toAssistantError()).isEqualTo(AssistantError.RateLimit())
    }

    @Test
    fun `given ChatStreamError BAD_REQUEST, when widened, then returns BadRequest`() {
        assertThat(ChatStreamError.BAD_REQUEST.toAssistantError()).isEqualTo(AssistantError.BadRequest())
    }

    @Test
    fun `given BAD_REQUEST with diagnostics, when widened, then BadRequest carries diagnostics`() {
        val diagnostics = Diagnostics(transport = TransportDiagnostics(httpStatus = 400))

        val error = ChatStreamError.BAD_REQUEST.toAssistantError(diagnostics = diagnostics)

        assertThat(error).isEqualTo(AssistantError.BadRequest(diagnostics))
        assertThat((error as AssistantError.BadRequest).diagnostics.transport?.httpStatus).isEqualTo(400)
    }

    @Test
    fun `given ChatStreamError UPSTREAM_FAILURE, when widened, then returns UpstreamFailure`() {
        assertThat(ChatStreamError.UPSTREAM_FAILURE.toAssistantError()).isEqualTo(AssistantError.UpstreamFailure())
    }

    @Test
    fun `given ChatStreamError INVALID_STREAM, when widened, then returns UpstreamFailure`() {
        // INVALID_STREAM means the upstream sent malformed bytes; UpstreamFailure
        // is the closest loop-level concept and pairs with the same retry rules.
        assertThat(ChatStreamError.INVALID_STREAM.toAssistantError()).isEqualTo(AssistantError.UpstreamFailure())
    }

    @Test
    fun `given ChatStreamError CANCELLED, when widened, then returns Cancelled`() {
        assertThat(ChatStreamError.CANCELLED.toAssistantError()).isEqualTo(AssistantError.Cancelled)
    }

    @Test
    fun `given ChatStreamError UNKNOWN with cause, when widened, then returns Unknown carrying the cause`() {
        val cause = IllegalStateException("boom")

        val error = ChatStreamError.UNKNOWN.toAssistantError(cause)

        assertThat(error).isInstanceOf(AssistantError.Unknown::class.java)
        assertThat((error as AssistantError.Unknown).cause).isSameAs(cause)
    }

    @Test
    fun `given ChatStreamError UNKNOWN with no cause, when widened, then returns Unknown with null cause`() {
        val error = ChatStreamError.UNKNOWN.toAssistantError()

        assertThat(error).isInstanceOf(AssistantError.Unknown::class.java)
        assertThat((error as AssistantError.Unknown).cause).isNull()
    }
}
