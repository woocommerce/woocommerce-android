package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class ToolReplayTracker(private val json: Json) {
    private val cachedSuccessesBySignature = mutableMapOf<String, ToolResult.Success>()
    private val callCountsBySignature = mutableMapOf<String, Int>()
    private val callCountsByToolName = mutableMapOf<String, Int>()

    fun prepare(call: ToolCall): ToolReplayDecision {
        val toolCallCount = callCountsByToolName.increment(call.name)
        if (toolCallCount > PER_TOOL_TURN_CALL_LIMIT) {
            return ToolReplayDecision.CapExceeded(
                ToolResult.ValidationError(
                    toolCallId = call.id,
                    reason = "$PER_TOOL_CAP_REASON_PREFIX ${call.name}. $PER_TOOL_CAP_REASON_SUFFIX",
                )
            )
        }

        val signature = call.canonicalSignature(json)
        val signatureCount = callCountsBySignature.increment(signature)
        val cached = cachedSuccessesBySignature[signature]

        return if (cached == null || signatureCount == 1) {
            ToolReplayDecision.Execute(signature)
        } else {
            ToolReplayDecision.Replay(
                cached.toReplayResult(
                    toolCallId = call.id,
                    hint = if (signatureCount == SECOND_IDENTICAL_CALL_COUNT) {
                        DUPLICATE_REPLAY_SOFT_HINT
                    } else {
                        DUPLICATE_REPLAY_ESCALATED_HINT
                    }
                )
            )
        }
    }

    fun record(signature: String, result: ToolResult) {
        if (result is ToolResult.Success) {
            cachedSuccessesBySignature.putIfAbsent(signature, result)
        }
    }

    private fun MutableMap<String, Int>.increment(key: String): Int {
        val updated = getOrDefault(key, 0) + 1
        this[key] = updated
        return updated
    }
}

internal sealed interface ToolReplayDecision {
    data class Execute(val signature: String) : ToolReplayDecision
    data class Replay(val result: ToolResult.Success) : ToolReplayDecision
    data class CapExceeded(val result: ToolResult.ValidationError) : ToolReplayDecision
}

private fun ToolCall.canonicalSignature(json: Json): String =
    "$name|${json.encodeToString(JsonElement.serializer(), arguments.toCanonicalJsonElement())}"

private fun JsonElement.toCanonicalJsonElement(): JsonElement = when (this) {
    is JsonObject -> JsonObject(
        entries
            .sortedBy { it.key }
            .associate { (key, value) -> key to value.toCanonicalJsonElement() }
    )
    is JsonArray -> JsonArray(map { it.toCanonicalJsonElement() })
    else -> this
}

private fun ToolResult.Success.toReplayResult(
    toolCallId: String,
    hint: String,
): ToolResult.Success = copy(
    toolCallId = toolCallId,
    structured = structured.withReplayHint(hint),
)

private fun JsonElement.withReplayHint(hint: String): JsonObject = when (this) {
    is JsonObject -> JsonObject(this + (DUPLICATE_REPLAY_HINT_FIELD to JsonPrimitive(hint)))
    else -> buildJsonObject {
        put(DUPLICATE_REPLAY_RESULT_FIELD, this@withReplayHint)
        put(DUPLICATE_REPLAY_HINT_FIELD, hint)
    }
}

private const val SECOND_IDENTICAL_CALL_COUNT = 2
private const val PER_TOOL_TURN_CALL_LIMIT = 4
private const val DUPLICATE_REPLAY_HINT_FIELD = "_assistant_runtime_hint"
private const val DUPLICATE_REPLAY_RESULT_FIELD = "result"
private const val DUPLICATE_REPLAY_SOFT_HINT = "You already fetched this - use the result above."
private const val DUPLICATE_REPLAY_ESCALATED_HINT =
    "STOP - you have called this tool identically 3+ times. Use the result above and finish now."
private const val PER_TOOL_CAP_REASON_PREFIX = "Tool call limit exceeded for"
private const val PER_TOOL_CAP_REASON_SUFFIX =
    "This tool was already called 4 times this turn. Use the results above and finish now."
