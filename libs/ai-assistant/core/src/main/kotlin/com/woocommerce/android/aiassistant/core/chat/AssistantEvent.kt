package com.woocommerce.android.aiassistant.core.chat

/**
 * Stream events emitted by [ChatService]. The agentic loop consumes this
 * vocabulary directly; transport-level details (HTTP status codes, SSE
 * framing) never escape the [ChatService] implementation.
 */
sealed interface AssistantEvent {
    data class TextDelta(val text: String) : AssistantEvent

    /**
     * One delta of a streaming tool call. [index] stays stable across deltas
     * for the same call so the loop can join them. [id] and [name] arrive on
     * the first delta; [argumentsDelta] arrives in JSON fragments that
     * concatenate into a complete object.
     */
    data class ToolCallDelta(
        val index: Int,
        val id: String? = null,
        val name: String? = null,
        val argumentsDelta: String? = null,
    ) : AssistantEvent

    /** Stream completed cleanly. The loop uses [reason] to decide between continuation and exit. */
    data class Finish(val reason: FinishReason) : AssistantEvent

    /** Stream failed. [cause] is preserved for telemetry but never inspected by UI code. */
    data class Failed(
        val kind: ChatStreamError,
        val cause: Throwable? = null,
        val diagnostics: Diagnostics = Diagnostics(),
    ) : AssistantEvent
}

enum class FinishReason { STOP, TOOL_CALLS, LENGTH, CONTENT_FILTER, OTHER }
