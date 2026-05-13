package com.woocommerce.android.aiassistant.telemetry

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.ToolFailureKind
import javax.inject.Inject

internal interface AssistantTelemetry {
    fun trackAssistantError(event: AssistantTelemetryEvent)
}

internal class NoOpAssistantTelemetry @Inject constructor() : AssistantTelemetry {
    override fun trackAssistantError(event: AssistantTelemetryEvent) = Unit
}

internal data class AssistantTelemetryEvent(
    val kind: AssistantTelemetryErrorKind,
    val httpStatus: Int? = null,
    val requestId: String? = null,
    val retryAfterMs: Long? = null,
    val toolName: String? = null,
    val toolFailureKind: ToolFailureKind? = null,
    val toolRetryable: Boolean? = null,
)

internal enum class AssistantTelemetryErrorKind {
    NETWORK,
    AUTH,
    RATE_LIMIT,
    BAD_REQUEST,
    TIMEOUT,
    UPSTREAM_FAILURE,
    TOOL_FAILED,
    INVALID_TOOL_CALL,
    OUTCOME_UNKNOWN,
    CANCELLED,
    UNKNOWN,
}

internal fun AssistantError.toAssistantTelemetryEvent(): AssistantTelemetryEvent {
    val diagnostics = when (this) {
        is AssistantError.Network -> diagnostics
        is AssistantError.Auth -> diagnostics
        is AssistantError.RateLimit -> diagnostics
        is AssistantError.BadRequest -> diagnostics
        is AssistantError.Timeout -> diagnostics
        is AssistantError.UpstreamFailure -> diagnostics
        is AssistantError.ToolFailed -> diagnostics
        is AssistantError.InvalidToolCall -> diagnostics
        is AssistantError.OutcomeUnknown -> diagnostics
        AssistantError.Cancelled -> null
        is AssistantError.Unknown -> diagnostics
    }
    val transport = diagnostics?.transport
    val tool = diagnostics?.tool
    return AssistantTelemetryEvent(
        kind = toTelemetryKind(),
        httpStatus = transport?.httpStatus,
        requestId = transport?.requestId,
        retryAfterMs = transport?.retryAfterMs,
        toolName = tool?.toolName ?: fallbackToolName(),
        toolFailureKind = tool?.failureKind,
        toolRetryable = tool?.retryable,
    )
}

private fun AssistantError.toTelemetryKind(): AssistantTelemetryErrorKind = when (this) {
    is AssistantError.Network -> AssistantTelemetryErrorKind.NETWORK
    is AssistantError.Auth -> AssistantTelemetryErrorKind.AUTH
    is AssistantError.RateLimit -> AssistantTelemetryErrorKind.RATE_LIMIT
    is AssistantError.BadRequest -> AssistantTelemetryErrorKind.BAD_REQUEST
    is AssistantError.Timeout -> AssistantTelemetryErrorKind.TIMEOUT
    is AssistantError.UpstreamFailure -> AssistantTelemetryErrorKind.UPSTREAM_FAILURE
    is AssistantError.ToolFailed -> AssistantTelemetryErrorKind.TOOL_FAILED
    is AssistantError.InvalidToolCall -> AssistantTelemetryErrorKind.INVALID_TOOL_CALL
    is AssistantError.OutcomeUnknown -> AssistantTelemetryErrorKind.OUTCOME_UNKNOWN
    AssistantError.Cancelled -> AssistantTelemetryErrorKind.CANCELLED
    is AssistantError.Unknown -> AssistantTelemetryErrorKind.UNKNOWN
}

private fun AssistantError.fallbackToolName(): String? = when (this) {
    is AssistantError.ToolFailed -> toolName
    is AssistantError.InvalidToolCall -> toolName
    is AssistantError.OutcomeUnknown -> toolName
    else -> null
}
