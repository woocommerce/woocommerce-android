package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.chat.openai.OpenAiStreamChunk
import com.woocommerce.android.aiassistant.chat.openai.toEvents
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.extensions.rethrow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.transformWhile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
        val chunk = runCatching { json.decodeFromString<OpenAiStreamChunk>(payload) }
            .getOrElse {
                emit(
                    AssistantEvent.Failed(
                        kind = ChatStreamError.INVALID_STREAM,
                        cause = MalformedChunkException(payload, it)
                    )
                )
                return false
            }

        return runCatching {
            chunk.toEvents().forEach { emit(it) }
            true
        }.rethrow<CancellationException, _>()
            .getOrElse {
                emit(AssistantEvent.Failed(kind = ChatStreamError.INVALID_STREAM, cause = it))
                false
            }
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
