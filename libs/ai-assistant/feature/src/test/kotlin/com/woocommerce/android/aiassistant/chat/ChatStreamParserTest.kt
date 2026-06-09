package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ChatStreamParserTest {
    private val parser = ChatStreamParser(assistantJsonForTests())

    @Test
    fun `given a content delta payload, when parsed, then a TextDelta is emitted`() = runTest {
        val payload = """{"choices":[{"delta":{"content":"Hello"}}]}"""

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).containsExactly(AssistantEvent.TextDelta("Hello"))
    }

    @Test
    fun `given an empty content delta, when parsed, then no event is emitted`() = runTest {
        val payload = """{"choices":[{"delta":{"content":""}}]}"""

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).isEmpty()
    }

    @Test
    fun `given a tool-call fragment, when parsed, then a ToolCallDelta is emitted with verbatim arguments`() = runTest {
        val payload = """{"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1",""" +
            """"function":{"name":"show_cards","arguments":"{\"a\":"}}]}}]}"""

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).containsExactly(
            AssistantEvent.ToolCallDelta(
                index = 0,
                id = "call_1",
                name = "show_cards",
                argumentsDelta = """{"a":""",
            )
        )
    }

    @Test
    fun `given a tool-call delta without index, when parsed, then it is skipped silently`() = runTest {
        val payload = """{"choices":[{"delta":{"tool_calls":[{"function":{"name":"x"}}]}}]}"""

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).isEmpty()
    }

    @Test
    fun `given finish_reason stop, when parsed, then Finish Stop is emitted`() = runTest {
        val payload = """{"choices":[{"delta":{},"finish_reason":"stop"}]}"""

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).containsExactly(AssistantEvent.Finish(FinishReason.STOP))
    }

    @Test
    fun `given finish_reason tool_calls, when parsed, then Finish ToolCalls is emitted`() = runTest {
        val payload = """{"choices":[{"finish_reason":"tool_calls"}]}"""

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).containsExactly(AssistantEvent.Finish(FinishReason.TOOL_CALLS))
    }

    @Test
    fun `given an unknown finish reason, when parsed, then Finish Other is emitted`() = runTest {
        val payload = """{"choices":[{"finish_reason":"refusal"}]}"""

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).containsExactly(AssistantEvent.Finish(FinishReason.OTHER))
    }

    @Test
    fun `given the DONE sentinel, when parsed, then no further events are emitted`() = runTest {
        val payload1 = """{"choices":[{"delta":{"content":"Hi"}}]}"""
        val done = "[DONE]"
        val later = """{"choices":[{"delta":{"content":"ignored"}}]}"""

        val events = parser.parse(flowOf(payload1, done, later)).toList()

        assertThat(events).containsExactly(AssistantEvent.TextDelta("Hi"))
    }

    @Test
    fun `given the DONE sentinel, when upstream stays open, then parsing completes immediately`() = runTest {
        val payload = """{"choices":[{"delta":{"content":"Hi"}}]}"""
        val done = "[DONE]"

        val events = withTimeout(1_000) {
            parser.parse(
                flow {
                    emit(payload)
                    emit(done)
                    awaitCancellation()
                }
            ).toList()
        }

        assertThat(events).containsExactly(AssistantEvent.TextDelta("Hi"))
    }

    @Test
    fun `given a malformed payload, when parsed, then a Failed INVALID_STREAM event is emitted`() = runTest {
        val payload = "not json at all"

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).hasSize(1)
        val failure = events.single() as AssistantEvent.Failed
        assertThat(failure.kind).isEqualTo(ChatStreamError.INVALID_STREAM)
        assertThat(failure.cause).isInstanceOf(MalformedChunkException::class.java)
    }

    @Test
    fun `given choices is null, when parsed, then strict parsing prevents null coercion fallback`() = runTest {
        val payload = """{"choices":null}"""

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).hasSize(1)
        val failure = events.single() as AssistantEvent.Failed
        assertThat(failure.kind).isEqualTo(ChatStreamError.INVALID_STREAM)
        assertThat(failure.cause).isInstanceOf(MalformedChunkException::class.java)
    }

    @Test
    fun `given empty and blank lines, when parsed, then they are ignored`() = runTest {
        val flow = flowOf("", "   ", """{"choices":[{"delta":{"content":"x"}}]}""")

        val events = parser.parse(flow).toList()

        assertThat(events).containsExactly(AssistantEvent.TextDelta("x"))
    }

    @Test
    fun `given empty choices usage payload, when parsed, then no event is emitted`() = runTest {
        val payload = """{"choices":[],"usage":{"prompt_tokens":1,"completion_tokens":2,"total_tokens":3}}"""

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).isEmpty()
    }

    @Test
    fun `given content and finish in the same chunk, when parsed, then both events are emitted in order`() = runTest {
        val payload = """{"choices":[{"delta":{"content":"done"},"finish_reason":"stop"}]}"""

        val events = parser.parse(flowOf(payload)).toList()

        assertThat(events).containsExactly(
            AssistantEvent.TextDelta("done"),
            AssistantEvent.Finish(FinishReason.STOP),
        )
    }
}
