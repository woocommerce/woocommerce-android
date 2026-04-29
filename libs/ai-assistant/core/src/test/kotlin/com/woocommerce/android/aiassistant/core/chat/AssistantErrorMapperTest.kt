package com.woocommerce.android.aiassistant.core.chat

import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import kotlinx.coroutines.CancellationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AssistantErrorMapperTest {
    @Test
    fun `given AssistantAuthException, when mapped, then returns Auth`() {
        val throwable: Throwable = AssistantAuthException()

        val error = throwable.toAssistantError()

        assertThat(error).isEqualTo(AssistantError.Auth)
    }

    @Test
    fun `given SocketTimeoutException, when mapped, then returns Timeout`() {
        val throwable: Throwable = SocketTimeoutException("read timed out")

        val error = throwable.toAssistantError()

        assertThat(error).isEqualTo(AssistantError.Timeout)
    }

    @Test
    fun `given SocketTimeoutException as IOException subtype, when mapped, then resolves to Timeout not Network`() {
        // SocketTimeoutException extends IOException — the mapper must check
        // it FIRST, otherwise the IOException branch would swallow it.
        val throwable: IOException = SocketTimeoutException()

        val error = throwable.toAssistantError()

        assertThat(error).isEqualTo(AssistantError.Timeout)
    }

    @Test
    fun `given UnknownHostException, when mapped, then returns Network`() {
        val throwable: Throwable = UnknownHostException("no dns")

        val error = throwable.toAssistantError()

        assertThat(error).isEqualTo(AssistantError.Network)
    }

    @Test
    fun `given ConnectException, when mapped, then returns Network`() {
        val throwable: Throwable = ConnectException("refused")

        val error = throwable.toAssistantError()

        assertThat(error).isEqualTo(AssistantError.Network)
    }

    @Test
    fun `given generic IOException, when mapped, then returns Network`() {
        val throwable: Throwable = IOException("broken pipe")

        val error = throwable.toAssistantError()

        assertThat(error).isEqualTo(AssistantError.Network)
    }

    @Test
    fun `given CancellationException, when mapped, then returns Cancelled`() {
        val throwable: Throwable = CancellationException("user navigated away")

        val error = throwable.toAssistantError()

        assertThat(error).isEqualTo(AssistantError.Cancelled)
    }

    @Test
    fun `given arbitrary RuntimeException, when mapped, then returns Unknown carrying the cause`() {
        val throwable: Throwable = IllegalStateException("oops")

        val error = throwable.toAssistantError()

        assertThat(error).isInstanceOf(AssistantError.Unknown::class.java)
        assertThat((error as AssistantError.Unknown).cause).isSameAs(throwable)
    }

    @Test
    fun `given HTTP 401, when mapped, then returns Auth`() {
        assertThat(assistantErrorFromHttpCode(401)).isEqualTo(AssistantError.Auth)
    }

    @Test
    fun `given HTTP 403, when mapped, then returns Auth`() {
        assertThat(assistantErrorFromHttpCode(403)).isEqualTo(AssistantError.Auth)
    }

    @Test
    fun `given HTTP 408, when mapped, then returns Timeout`() {
        assertThat(assistantErrorFromHttpCode(408)).isEqualTo(AssistantError.Timeout)
    }

    @Test
    fun `given HTTP 429, when mapped, then returns RateLimit`() {
        assertThat(assistantErrorFromHttpCode(429)).isEqualTo(AssistantError.RateLimit)
    }

    @Test
    fun `given HTTP 500, when mapped, then returns UpstreamFailure`() {
        assertThat(assistantErrorFromHttpCode(500)).isEqualTo(AssistantError.UpstreamFailure)
    }

    @Test
    fun `given HTTP 503, when mapped, then returns UpstreamFailure`() {
        assertThat(assistantErrorFromHttpCode(503)).isEqualTo(AssistantError.UpstreamFailure)
    }

    @Test
    fun `given HTTP 599 boundary, when mapped, then returns UpstreamFailure`() {
        assertThat(assistantErrorFromHttpCode(599)).isEqualTo(AssistantError.UpstreamFailure)
    }

    @Test
    fun `given HTTP 400, when mapped, then returns Unknown without cause`() {
        val error = assistantErrorFromHttpCode(400)

        assertThat(error).isInstanceOf(AssistantError.Unknown::class.java)
        assertThat((error as AssistantError.Unknown).cause).isNull()
    }

    @Test
    fun `given HTTP 200 success code, when mapped, then returns Unknown`() {
        // Defensive: callers shouldn't ask, but if they do we must not crash.
        val error = assistantErrorFromHttpCode(200)

        assertThat(error).isInstanceOf(AssistantError.Unknown::class.java)
    }

    @Test
    fun `given ChatStreamError NETWORK, when widened, then returns Network`() {
        assertThat(ChatStreamError.NETWORK.toAssistantError()).isEqualTo(AssistantError.Network)
    }

    @Test
    fun `given ChatStreamError TIMEOUT, when widened, then returns Timeout`() {
        assertThat(ChatStreamError.TIMEOUT.toAssistantError()).isEqualTo(AssistantError.Timeout)
    }

    @Test
    fun `given ChatStreamError AUTH, when widened, then returns Auth`() {
        assertThat(ChatStreamError.AUTH.toAssistantError()).isEqualTo(AssistantError.Auth)
    }

    @Test
    fun `given ChatStreamError RATE_LIMIT, when widened, then returns RateLimit`() {
        assertThat(ChatStreamError.RATE_LIMIT.toAssistantError()).isEqualTo(AssistantError.RateLimit)
    }

    @Test
    fun `given ChatStreamError UPSTREAM_FAILURE, when widened, then returns UpstreamFailure`() {
        assertThat(ChatStreamError.UPSTREAM_FAILURE.toAssistantError()).isEqualTo(AssistantError.UpstreamFailure)
    }

    @Test
    fun `given ChatStreamError INVALID_STREAM, when widened, then returns UpstreamFailure`() {
        // INVALID_STREAM means the upstream sent malformed bytes; UpstreamFailure
        // is the closest loop-level concept and pairs with the same retry rules.
        assertThat(ChatStreamError.INVALID_STREAM.toAssistantError()).isEqualTo(AssistantError.UpstreamFailure)
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
