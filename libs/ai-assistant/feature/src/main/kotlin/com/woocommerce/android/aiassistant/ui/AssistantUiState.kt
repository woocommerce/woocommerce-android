package com.woocommerce.android.aiassistant.ui

import androidx.annotation.StringRes
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.runtime.AssistantPendingConfirmation

data class AssistantUiState(
    val messages: List<AssistantUiMessage> = emptyList(),
    val status: AssistantUiStatus = AssistantUiStatus.IDLE,
    val error: AssistantUiError? = null,
    val canRetry: Boolean = false,
    val pendingConfirmation: AssistantPendingConfirmation? = null,
    val pendingNavigation: AssistantPendingNavigation? = null,
) {
    val isStreaming: Boolean
        get() = status == AssistantUiStatus.STREAMING

    val isTurnActive: Boolean
        get() = status == AssistantUiStatus.STREAMING ||
            status == AssistantUiStatus.AWAITING_CONFIRMATION
}

enum class AssistantUiStatus {
    IDLE,
    STREAMING,
    ERROR,
    AWAITING_CONFIRMATION,
}

data class AssistantUiMessage(
    val id: String,
    val role: Role,
    val text: String,
    val error: AssistantMessageError? = null,
) {
    enum class Role {
        USER,
        ASSISTANT,
    }
}

data class AssistantMessageError(
    val error: AssistantError,
    val canRetry: Boolean,
)

enum class AssistantUiError {
    NETWORK,
    AUTH,
    RATE_LIMIT,
    TIMEOUT,
    UPSTREAM_FAILURE,
    TOOL_FAILED,
    INVALID_TOOL_CALL,
    OUTCOME_UNKNOWN,
    CANCELLED,
    CONFIRMATION_DEFERRED,
    MAX_ITERATIONS,
    UNKNOWN,
}

sealed interface AssistantPendingNavigation

fun AssistantError.toAssistantUiError(): AssistantUiError = when (this) {
    AssistantError.Network -> AssistantUiError.NETWORK
    AssistantError.Auth -> AssistantUiError.AUTH
    AssistantError.RateLimit -> AssistantUiError.RATE_LIMIT
    AssistantError.Timeout -> AssistantUiError.TIMEOUT
    AssistantError.UpstreamFailure -> AssistantUiError.UPSTREAM_FAILURE
    is AssistantError.ToolFailed -> AssistantUiError.TOOL_FAILED
    is AssistantError.InvalidToolCall -> AssistantUiError.INVALID_TOOL_CALL
    is AssistantError.OutcomeUnknown -> AssistantUiError.OUTCOME_UNKNOWN
    AssistantError.Cancelled -> AssistantUiError.CANCELLED
    is AssistantError.Unknown -> AssistantUiError.UNKNOWN
}

internal fun AssistantError.supportsRetryAction(): Boolean = when (this) {
    AssistantError.Network,
    AssistantError.Timeout,
    AssistantError.RateLimit -> true
    else -> false
}

@StringRes
internal fun AssistantError.toMessageRes(): Int = when (this) {
    AssistantError.Network -> R.string.assistant_chat_error_network
    AssistantError.Auth -> R.string.assistant_chat_error_auth
    AssistantError.RateLimit -> R.string.assistant_chat_error_rate_limit
    AssistantError.Timeout -> R.string.assistant_chat_error_timeout
    AssistantError.UpstreamFailure -> R.string.assistant_chat_error_upstream_failure
    is AssistantError.ToolFailed -> R.string.assistant_chat_error_tool_failed
    is AssistantError.InvalidToolCall -> R.string.assistant_chat_error_invalid_tool_call
    is AssistantError.OutcomeUnknown -> R.string.assistant_chat_error_outcome_unknown
    AssistantError.Cancelled -> R.string.assistant_chat_error_cancelled
    is AssistantError.Unknown -> R.string.assistant_chat_error_unknown
}
