package com.woocommerce.android.aiassistant.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreview
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard

data class AssistantUiState(
    val messages: List<AssistantUiMessage> = emptyList(),
    val status: AssistantUiStatus = AssistantUiStatus.IDLE,
    val error: AssistantUiError? = null,
    val canRetry: Boolean = false,
    val activeConfirmationId: String? = null,
    val activeAssistantMessageId: String? = null,
    val pendingNavigation: AssistantPendingNavigation? = null,
) {
    val isStreaming: Boolean
        get() = status == AssistantUiStatus.STREAMING

    val isTurnActive: Boolean
        get() = status == AssistantUiStatus.STREAMING ||
            status == AssistantUiStatus.AWAITING_CONFIRMATION

    val shouldShowStopControl: Boolean
        get() = status == AssistantUiStatus.STREAMING

    val shouldShowFallbackError: Boolean
        get() = status == AssistantUiStatus.ERROR &&
            error != null &&
            messages.lastOrNull()?.error == null

    /**
     * Mirrors iOS `streamingState == .sending`: dots are visible from submit until the active assistant
     * message starts streaming actual text. Tool activity segments don't count as "text", so the dots
     * stay visible during pre-text tool calls and disappear only once the model emits its first text
     * token. Within a single message text is appended monotonically, so once dots hide they don't bounce
     * back for that turn.
     */
    val shouldShowTypingIndicator: Boolean
        get() = status == AssistantUiStatus.STREAMING && !activeAssistantHasStreamedText

    private val activeAssistantHasStreamedText: Boolean
        get() {
            val active = messages.firstOrNull { it.id == activeAssistantMessageId } ?: return false
            return active.segments.any { it is AssistantUiSegment.Text && it.text.isNotEmpty() }
        }
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
    val segments: List<AssistantUiSegment>,
    val error: AssistantMessageError? = null,
) {
    constructor(
        id: String,
        role: Role,
        text: String,
        error: AssistantMessageError? = null,
    ) : this(
        id = id,
        role = role,
        segments = listOf(AssistantUiSegment.Text(text)),
        error = error,
    )

    val text: String
        get() = segments.filterIsInstance<AssistantUiSegment.Text>().joinToString(separator = "") { it.text }

    enum class Role {
        USER,
        ASSISTANT,
    }
}

data class AssistantToolActivity(
    val toolCallId: String,
    val toolName: String,
    val status: Status = Status.RUNNING,
) {
    enum class Status {
        RUNNING,
        COMPLETED,
    }
}

sealed interface AssistantUiSegment {
    data class Text(val text: String) : AssistantUiSegment

    data class ConfirmationCard(val model: AssistantConfirmationCard) : AssistantUiSegment

    data class CardGroup(val cards: List<AssistantCard>) : AssistantUiSegment

    data class ToolActivity(val activity: AssistantToolActivity) : AssistantUiSegment
}

@StringRes
internal fun AssistantToolActivity.labelRes(): Int = when (toolName) {
    "orders_list",
    "orders_get" -> R.string.assistant_chat_tool_activity_orders_read
    "orders_update",
    "orders_bulk_update" -> R.string.assistant_chat_tool_activity_orders_write
    "products_list",
    "products_get",
    "product_variations_list" -> R.string.assistant_chat_tool_activity_products_read
    "products_update",
    "products_bulk_update",
    "product_variations_update" -> R.string.assistant_chat_tool_activity_products_write
    "analytics_orders",
    "analytics_revenue" -> R.string.assistant_chat_tool_activity_analytics
    "customers_list" -> R.string.assistant_chat_tool_activity_customers
    else -> R.string.assistant_chat_tool_activity_generic
}

data class AssistantConfirmationCard(
    val confirmationId: String,
    val toolCall: ToolCall,
    val state: AssistantConfirmationCardState,
    val preview: RenderedConfirmationPreview? = null,
)

enum class AssistantConfirmationCardState {
    PENDING,
    CONFIRMED,
    CANCELLED,
}

@StringRes
internal fun AssistantConfirmationCardState.eyebrowRes(): Int = when (this) {
    AssistantConfirmationCardState.PENDING -> R.string.assistant_confirmation_eyebrow_pending
    AssistantConfirmationCardState.CONFIRMED -> R.string.assistant_confirmation_eyebrow_confirmed
    AssistantConfirmationCardState.CANCELLED -> R.string.assistant_confirmation_eyebrow_cancelled
}

@DrawableRes
internal fun AssistantConfirmationCardState.iconRes(): Int = when (this) {
    AssistantConfirmationCardState.PENDING -> R.drawable.ic_assistant_confirmation_pending
    AssistantConfirmationCardState.CONFIRMED -> R.drawable.ic_assistant_confirmation_confirmed
    AssistantConfirmationCardState.CANCELLED -> R.drawable.ic_assistant_confirmation_cancelled
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

@StringRes
internal fun AssistantUiError.toMessageRes(): Int = when (this) {
    AssistantUiError.NETWORK -> R.string.assistant_chat_error_network
    AssistantUiError.AUTH -> R.string.assistant_chat_error_auth
    AssistantUiError.RATE_LIMIT -> R.string.assistant_chat_error_rate_limit
    AssistantUiError.TIMEOUT -> R.string.assistant_chat_error_timeout
    AssistantUiError.UPSTREAM_FAILURE -> R.string.assistant_chat_error_upstream_failure
    AssistantUiError.TOOL_FAILED -> R.string.assistant_chat_error_tool_failed
    AssistantUiError.INVALID_TOOL_CALL -> R.string.assistant_chat_error_invalid_tool_call
    AssistantUiError.OUTCOME_UNKNOWN -> R.string.assistant_chat_error_outcome_unknown
    AssistantUiError.CANCELLED -> R.string.assistant_chat_error_cancelled
    AssistantUiError.CONFIRMATION_DEFERRED -> R.string.assistant_chat_error_confirmation_deferred
    AssistantUiError.MAX_ITERATIONS -> R.string.assistant_chat_error_max_iterations
    AssistantUiError.UNKNOWN -> R.string.assistant_chat_error_unknown
}
