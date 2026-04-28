package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.core.chat.AssistantErrorKind
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.transformWhile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses an OpenAI-compatible chat-completion SSE stream into a flow of
 * [AssistantEvent]s.
 *
 * The caller is expected to have already peeled the transport envelope: each
 * item in the input flow is the body of one `data: <payload>` line with the
 * `data: ` prefix stripped and empty lines filtered out. The sentinel line
 * `[DONE]` terminates the stream.
 *
 * The parser stays stateless with respect to tool-call assembly — each
 * streamed `tool_calls[*]` fragment becomes one [AssistantEvent.ToolCallDelta]
 * with its `argumentsDelta` kept verbatim. Higher layers (the agentic loop)
 * concatenate deltas by index into final tool calls; doing the merge here
 * would hide the raw stream from anything that wants to display partial
 * progress.
 */
@Singleton
internal class ChatStreamParser @Inject constructor(
    @AiAssistantJson private val json: Json,
) {
    fun parse(lines: Flow<String>): Flow<AssistantEvent> = lines.transformWhile { raw ->
        val payload = raw.trim()
        when {
            payload.isEmpty() -> true
            payload == DONE_SENTINEL -> false
            else -> parsePayload(payload)
        }
    }

    private suspend fun FlowCollector<AssistantEvent>.parsePayload(payload: String): Boolean {
        val chunk = runCatching { json.parseToJsonElement(payload).jsonObject }
            .getOrElse {
                emit(AssistantEvent.Failed(AssistantErrorKind.INVALID_STREAM, MalformedChunkException(payload, it)))
                return false
            }

        return runCatching {
            emitChunk(chunk)
            true
        }.getOrElse {
            emit(AssistantEvent.Failed(AssistantErrorKind.INVALID_STREAM, it))
            false
        }
    }

    private suspend fun FlowCollector<AssistantEvent>.emitChunk(chunk: JsonObject) {
        val choice = chunk["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return
        val delta = choice["delta"]?.jsonObject

        if (delta != null) {
            delta["content"]?.stringOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { emit(AssistantEvent.TextDelta(it)) }

            delta["tool_calls"]?.jsonArray?.forEach { element ->
                val toolCall = element.jsonObject
                val index = toolCall["index"]?.jsonPrimitive?.int ?: return@forEach
                val id = toolCall["id"]?.stringOrNull()
                val function = toolCall["function"]?.jsonObject
                val name = function?.get("name")?.stringOrNull()
                val argumentsDelta = function?.get("arguments")?.stringOrNull()
                emit(
                    AssistantEvent.ToolCallDelta(
                        index = index,
                        id = id,
                        name = name,
                        argumentsDelta = argumentsDelta,
                    )
                )
            }
        }

        val finish = choice["finish_reason"]?.stringOrNull()
        if (finish != null) {
            emit(AssistantEvent.Finish(finish.toFinishReason()))
        }
    }

    private fun JsonElement.stringOrNull(): String? {
        if (this is JsonNull) return null
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.content
    }

    private fun String.toFinishReason(): FinishReason = when (this) {
        "stop" -> FinishReason.STOP
        "tool_calls" -> FinishReason.TOOL_CALLS
        "length" -> FinishReason.LENGTH
        "content_filter" -> FinishReason.CONTENT_FILTER
        else -> FinishReason.OTHER
    }

    private companion object {
        private const val DONE_SENTINEL = "[DONE]"
    }
}

internal class MalformedChunkException(
    val payload: String,
    cause: Throwable,
) : RuntimeException("Malformed SSE chunk: ${payload.take(MAX_PAYLOAD_PREVIEW_CHARS)}", cause) {
    companion object {
        private const val MAX_PAYLOAD_PREVIEW_CHARS = 200
    }
}
