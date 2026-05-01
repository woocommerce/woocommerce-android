package com.woocommerce.android.aiassistant.ui

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantUiStateTest {
    @Test
    fun `given transient assistant errors, when checking retry action support, then retry is supported`() {
        val retryableErrors = listOf(
            AssistantError.Network,
            AssistantError.Timeout,
            AssistantError.RateLimit,
        )

        retryableErrors.forEach { error ->
            assertThat(error.supportsRetryAction()).isTrue()
        }
    }

    @Test
    fun `given assistant errors, when mapping to message resources, then product copy resources are returned`() {
        assertThat(AssistantError.Network.toMessageRes()).isEqualTo(R.string.assistant_chat_error_network)
        assertThat(AssistantError.Timeout.toMessageRes()).isEqualTo(R.string.assistant_chat_error_timeout)
        assertThat(AssistantError.RateLimit.toMessageRes()).isEqualTo(R.string.assistant_chat_error_rate_limit)
        assertThat(AssistantError.Auth.toMessageRes()).isEqualTo(R.string.assistant_chat_error_auth)
        assertThat(AssistantError.UpstreamFailure.toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_upstream_failure)
        assertThat(AssistantError.ToolFailed(toolName = "orders_update").toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_tool_failed)
        assertThat(AssistantError.InvalidToolCall(toolName = "orders_update").toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_invalid_tool_call)
        assertThat(AssistantError.OutcomeUnknown(toolName = "orders_update").toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_outcome_unknown)
        assertThat(AssistantError.Cancelled.toMessageRes()).isEqualTo(R.string.assistant_chat_error_cancelled)
        assertThat(AssistantError.Unknown().toMessageRes()).isEqualTo(R.string.assistant_chat_error_unknown)
    }

    @Test
    fun `given raw throwable messages, when mapping errors, then raw messages are not used`() {
        val rawCause = IllegalStateException("raw upstream token abc123")

        assertThat(AssistantError.Unknown(cause = rawCause).toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_unknown)
        assertThat(AssistantError.ToolFailed(toolName = "orders_update", cause = rawCause).toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_tool_failed)
    }

    @Test
    fun `given error state with no inline message error, when checking fallback, then fallback is visible`() {
        val state = AssistantUiState(
            status = AssistantUiStatus.ERROR,
            error = AssistantUiError.MAX_ITERATIONS,
            messages = listOf(
                AssistantUiMessage(
                    id = "message-1",
                    role = AssistantUiMessage.Role.USER,
                    text = "Hello",
                ),
                AssistantUiMessage(
                    id = "message-2",
                    role = AssistantUiMessage.Role.ASSISTANT,
                    text = "",
                ),
            ),
        )

        assertThat(state.shouldShowFallbackError).isTrue()
        assertThat(requireNotNull(state.error).toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_max_iterations)
    }

    @Test
    fun `given error state with inline message error, when checking fallback, then fallback is hidden`() {
        val state = AssistantUiState(
            status = AssistantUiStatus.ERROR,
            error = AssistantUiError.NETWORK,
            messages = listOf(
                AssistantUiMessage(
                    id = "message-1",
                    role = AssistantUiMessage.Role.ASSISTANT,
                    text = "",
                    error = AssistantMessageError(
                        error = AssistantError.Network,
                        canRetry = true,
                    ),
                ),
            ),
        )

        assertThat(state.shouldShowFallbackError).isFalse()
    }

    @Test
    fun `given ui errors, when mapping to message resources, then fallback copy resources are returned`() {
        assertThat(AssistantUiError.CONFIRMATION_DEFERRED.toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_confirmation_deferred)
        assertThat(AssistantUiError.MAX_ITERATIONS.toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_max_iterations)
        assertThat(AssistantUiError.CANCELLED.toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_cancelled)
    }

    @Test
    fun `when status is streaming, then turn is active`() {
        val state = AssistantUiState(status = AssistantUiStatus.STREAMING)

        assertThat(state.isTurnActive).isTrue()
    }

    @Test
    fun `when status is awaiting confirmation, then turn is active`() {
        val state = AssistantUiState(status = AssistantUiStatus.AWAITING_CONFIRMATION)

        assertThat(state.isTurnActive).isTrue()
    }

    @Test
    fun `when status is idle, then turn is not active`() {
        val state = AssistantUiState(status = AssistantUiStatus.IDLE)

        assertThat(state.isTurnActive).isFalse()
    }

    @Test
    fun `when status is error, then turn is not active`() {
        val state = AssistantUiState(status = AssistantUiStatus.ERROR)

        assertThat(state.isTurnActive).isFalse()
    }
}
