package com.woocommerce.android.aiassistant.ui

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
) {
    enum class Role {
        USER,
        ASSISTANT,
    }
}

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
