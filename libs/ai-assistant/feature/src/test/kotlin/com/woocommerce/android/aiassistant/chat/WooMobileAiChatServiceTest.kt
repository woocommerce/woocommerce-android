package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.auth.WpComOAuthTokenProvider
import com.woocommerce.android.aiassistant.chat.woomobileai.WooMobileAiWrapperErrorMapper
import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.toAssistantError
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class WooMobileAiChatServiceTest {
    private lateinit var server: MockWebServer
    private val tokenProvider = RecordingWpComTokenProvider()
    private val assistantJson = assistantJsonForTests()

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `given a request, when sent, then it posts to the woo mobile ai chat completions path`() = runTest {
        server.enqueue(sseResponse(SAMPLE_SSE_BODY))

        newService().streamTurn(simpleRequest()).toList()

        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/wpcom/v2/woo-mobile-ai/chat/completions")
    }

    @Test
    fun `given a request, when sent, then it uses the wpcom bearer token`() = runTest {
        server.enqueue(sseResponse(SAMPLE_SSE_BODY))

        newService().streamTurn(simpleRequest()).toList()

        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer wpcom-token")
        assertThat(recorded.getHeader("Accept")).isEqualTo("text/event-stream")
        assertThat(tokenProvider.provideCalls).isEqualTo(1)
    }

    @Test
    fun `given a request, when sent, then body uses canonical open ai wrapper contract`() = runTest {
        server.enqueue(sseResponse(SAMPLE_SSE_BODY))

        newService().streamTurn(simpleRequest()).toList()

        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertThat(body.getValue("model").jsonPrimitive.content).isEqualTo(AssistantConfig.MODEL_ID)
        assertThat(body.getValue("stream").jsonPrimitive.boolean).isTrue()
        assertThat(body.getValue("stream_options").jsonObject.getValue("include_usage").jsonPrimitive.boolean).isTrue()
        assertThat(body).doesNotContainKey("feature")
        assertThat(body).doesNotContainKey("tool_choice")
    }

    @Test
    fun `given a streaming response, when streamTurn collects, then content deltas and finish are emitted`() = runTest {
        server.enqueue(sseResponse(SAMPLE_SSE_BODY))

        val events = newService().streamTurn(simpleRequest()).toList()

        assertThat(events).containsExactly(
            AssistantEvent.TextDelta("Hello"),
            AssistantEvent.TextDelta(" world"),
            AssistantEvent.Finish(FinishReason.STOP),
        )
    }

    @Test
    fun `given a tool call stream, when streamTurn collects, then tool call deltas are emitted`() = runTest {
        val toolCallChunk = """{"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1",""" +
            """"function":{"name":"show_cards","arguments":"{\"type\":"}}]}}]}"""
        server.enqueue(
            sseResponse(
                """
                data: $toolCallChunk

                data: {"choices":[{"finish_reason":"tool_calls"}]}

                data: [DONE]

                """.trimIndent()
            )
        )

        val events = newService().streamTurn(simpleRequest()).toList()

        assertThat(events).containsExactly(
            AssistantEvent.ToolCallDelta(
                index = 0,
                id = "call_1",
                name = "show_cards",
                argumentsDelta = """{"type":""",
            ),
            AssistantEvent.Finish(FinishReason.TOOL_CALLS),
        )
    }

    @Test
    fun `given a malformed stream chunk, when streamTurn collects, then invalid stream is emitted`() = runTest {
        server.enqueue(
            sseResponse(
                """
                data: not json

                """.trimIndent()
            )
        )

        val events = newService().streamTurn(simpleRequest()).toList()

        assertThat(events).hasSize(1)
        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.INVALID_STREAM)
    }

    @Test
    fun `given token provider auth failure, when streaming, then auth failure is emitted`() = runTest {
        val service = newService(
            tokenProvider = ThrowingWpComTokenProvider(AssistantAuthException("Missing WPCOM OAuth bearer"))
        )

        val events = service.streamTurn(simpleRequest()).toList()

        assertThat(events).hasSize(1)
        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.AUTH)
    }

    @Test
    fun `given rate limit http wrapper envelope, when streaming, then rate limit failure is emitted`() = runTest {
        server.enqueue(wrapperJsonResponse("woo_mobile_ai_user_rate_limit", status = 429))

        val events = newService().streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.RATE_LIMIT)
        assertThat(failed.kind.toAssistantError(diagnostics = failed.diagnostics))
            .isInstanceOf(AssistantError.RateLimit::class.java)
        assertThat(failed.diagnostics.transport?.httpStatus).isEqualTo(429)
    }

    @Test
    fun `given unauthorized http wrapper envelope, when streaming, then auth failure is emitted`() = runTest {
        server.enqueue(wrapperJsonResponse("rest_unauthorized", status = 401))

        val events = newService().streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.AUTH)
    }

    @Test
    fun `given forbidden http wrapper envelope, when streaming, then auth failure is emitted`() = runTest {
        server.enqueue(wrapperJsonResponse("rest_forbidden", status = 403))

        val events = newService().streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.AUTH)
    }

    @Test
    fun `given first stream frame is rate limit wrapper envelope, when streaming, then rate limit failure is emitted`() =
        runTest {
            val envelope = wrapperEnvelopeLine("woo_mobile_ai_user_rate_limit", status = 429)
            server.enqueue(sseResponse("""data: $envelope"""))

            val events = newService().streamTurn(simpleRequest()).toList()

            val failed = events.single() as AssistantEvent.Failed
            assertThat(failed.kind).isEqualTo(ChatStreamError.RATE_LIMIT)
        }

    @Test
    fun `given first stream frame is unauthorized wrapper envelope, when streaming, then auth failure is emitted`() =
        runTest {
            server.enqueue(sseResponse("""data: ${wrapperEnvelopeLine("rest_unauthorized", status = 401)}"""))

            val events = newService().streamTurn(simpleRequest()).toList()

            val failed = events.single() as AssistantEvent.Failed
            assertThat(failed.kind).isEqualTo(ChatStreamError.AUTH)
        }

    @Test
    fun `given first stream frame is forbidden wrapper envelope, when streaming, then auth failure is emitted`() =
        runTest {
            server.enqueue(sseResponse("""data: ${wrapperEnvelopeLine("rest_forbidden", status = 403)}"""))

            val events = newService().streamTurn(simpleRequest()).toList()

            val failed = events.single() as AssistantEvent.Failed
            assertThat(failed.kind).isEqualTo(ChatStreamError.AUTH)
        }

    @Test
    fun `given unknown stream wrapper code with status, when streaming, then status classification is used`() = runTest {
        val envelope = wrapperEnvelopeLine(
            code = "unknown_wrapper_code",
            status = 500,
            message = "Bearer secret-token",
        )
        server.enqueue(
            sseResponse(
                """data: $envelope"""
            )
        )

        val events = newService().streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.UPSTREAM_FAILURE)
        assertThat(failed.diagnostics.transport?.httpStatus).isEqualTo(500)
        assertThat(failed.diagnostics.transport?.bodySnippet).contains("Bearer [REDACTED]")
        assertThat(failed.diagnostics.transport?.bodySnippet).doesNotContain("secret-token")
    }

    @Test
    fun `given stream wrapper envelope after first frame, when streaming, then parser owns it as invalid stream`() =
        runTest {
            val envelope = wrapperEnvelopeLine("woo_mobile_ai_user_rate_limit", status = 429)
            server.enqueue(
                sseResponse(
                    """
                    data: {"choices":[{"delta":{"content":"Hello"}}]}

                    data: $envelope

                    """.trimIndent()
                )
            )

            val events = newService().streamTurn(simpleRequest()).toList()

            assertThat(events.first()).isEqualTo(AssistantEvent.TextDelta("Hello"))
            val failed = events.last() as AssistantEvent.Failed
            assertThat(failed.kind).isEqualTo(ChatStreamError.INVALID_STREAM)
        }

    @Test
    fun `given a request with assistant tool replay, when sent, then existing open ai message helper shape is used`() =
        runTest {
            server.enqueue(sseResponse(SAMPLE_SSE_BODY))

            newService().streamTurn(
                ChatRequest(
                    messages = listOf(
                        AssistantMessage.Assistant(
                            content = null,
                            toolCalls = listOf(
                                ToolCall(
                                    id = "call_1",
                                    name = "lookup_products",
                                    arguments = buildJsonObject {
                                        put("query", "shirt")
                                    },
                                )
                            ),
                        )
                    ),
                )
            ).toList()

            val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            val assistantMessage = body.getValue("messages").jsonArray.single().jsonObject

            assertThat(assistantMessage.getValue("content").jsonPrimitive.content).isEmpty()
        }

    private fun newService(
        tokenProvider: WpComOAuthTokenProvider = this.tokenProvider,
    ): WooMobileAiChatService = WooMobileAiChatService(
        httpClient = httpClient,
        tokenProvider = tokenProvider,
        streamParser = ChatStreamParser(assistantJson),
        json = assistantJson,
        baseUrl = server.url("/").toString().removeSuffix("/"),
        transportDiagnosticsFactory = TransportDiagnosticsFactory(),
        wrapperErrorMapper = WooMobileAiWrapperErrorMapper(assistantJson),
    )

    private fun simpleRequest(): ChatRequest = ChatRequest(
        messages = listOf(AssistantMessage.User("hi")),
    )

    private fun sseResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(body)

    private fun wrapperJsonResponse(code: String, status: Int): MockResponse = MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json; charset=UTF-8")
        .setBody(wrapperEnvelope(code, status))

    private fun wrapperEnvelope(
        code: String,
        status: Int,
        message: String = "Wrapper error",
    ): String = """
        {
          "code": "$code",
          "message": "$message",
          "data": {
            "status": $status
          }
        }
    """.trimIndent()

    private fun wrapperEnvelopeLine(
        code: String,
        status: Int,
        message: String = "Wrapper error",
    ): String = wrapperEnvelope(code, status, message).lineSequence().joinToString(separator = "")

    private class RecordingWpComTokenProvider : WpComOAuthTokenProvider {
        var provideCalls: Int = 0

        override suspend fun provide(): String {
            provideCalls++
            return "wpcom-token"
        }
    }

    private class ThrowingWpComTokenProvider(
        private val exception: AssistantAuthException,
    ) : WpComOAuthTokenProvider {
        override suspend fun provide(): String = throw exception
    }

    private companion object {
        private val SAMPLE_SSE_BODY = """
            data: {"choices":[{"delta":{"content":"Hello"}}]}

            data: {"choices":[{"delta":{"content":" world"}}]}

            data: {"choices":[{"finish_reason":"stop"}]}

            data: [DONE]


        """.trimIndent()
    }
}
