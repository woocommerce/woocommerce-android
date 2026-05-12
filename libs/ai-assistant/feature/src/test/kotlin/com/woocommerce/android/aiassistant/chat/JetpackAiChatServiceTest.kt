package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider
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

class JetpackAiChatServiceTest {
    private lateinit var server: MockWebServer
    private val tokenProvider = RecordingTokenProvider()
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
    fun `given a streaming response, when streamTurn collects, then content deltas and finish are emitted`() = runTest {
        server.enqueue(sseResponse(SAMPLE_SSE_BODY))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        assertThat(events).containsExactly(
            AssistantEvent.TextDelta("Hello"),
            AssistantEvent.TextDelta(" world"),
            AssistantEvent.Finish(FinishReason.STOP),
        )
    }

    @Test
    fun `given a request, when sent, then the bearer header and body shape match the contract`() = runTest {
        server.enqueue(sseResponse(SAMPLE_SSE_BODY))

        val service = newService()
        service.streamTurn(simpleRequest()).toList()

        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/wpcom/v2/jetpack-ai-query")
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer fake-token-1")
        assertThat(recorded.getHeader("Accept")).isEqualTo("text/event-stream")
        val body = recorded.body.readUtf8()
        val root = Json.parseToJsonElement(body).jsonObject
        assertThat(root.getValue("model").jsonPrimitive.content).isEqualTo(AssistantConfig.MODEL_ID)
        assertThat(root.getValue("feature").jsonPrimitive.content).isEqualTo(AssistantConfig.FEATURE_NAME)
        assertThat(body).contains(""""stream":true""")
        assertThat(body).contains(""""messages":[""")
    }

    @Test
    fun `given assistant tool calls with null content, when sent, then request uses empty assistant content for backend compatibility`() = runTest {
        server.enqueue(sseResponse(SAMPLE_SSE_BODY))

        val service = newService()
        service.streamTurn(
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
                            ),
                        ),
                    ),
                ),
            )
        ).toList()

        val recorded = server.takeRequest()
        val body = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
        val assistantMessage = body.getValue("messages").jsonArray.single().jsonObject

        assertThat(assistantMessage.getValue("content").jsonPrimitive.content).isEmpty()
        assertThat(assistantMessage.getValue("tool_calls").jsonArray).hasSize(1)
    }

    @Test
    fun `given 401 before any data, when streaming, then token is invalidated and the call retried once`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(sseResponse(SAMPLE_SSE_BODY))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        assertThat(tokenProvider.invalidations).isEqualTo(1)
        assertThat(tokenProvider.tokensProvided).containsExactly("fake-token-1", "fake-token-2")
        assertThat(events).contains(AssistantEvent.TextDelta("Hello"))
        assertThat(events.last()).isEqualTo(AssistantEvent.Finish(FinishReason.STOP))
    }

    @Test
    fun `given 401 on every retry, when streaming, then a Failed AUTH event is emitted exactly once`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        assertThat(events).hasSize(1)
        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.AUTH)
        assertThat(tokenProvider.invalidations).isEqualTo(1)
    }

    @Test
    fun `given 403 before any data, when streaming, then the auth failure is not retried`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        assertThat(events).hasSize(1)
        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.AUTH)
        assertThat(tokenProvider.invalidations).isZero()
        assertThat(tokenProvider.tokensProvided).containsExactly("fake-token-1")
    }

    @Test
    fun `given 429, when streaming, then a Failed RATE_LIMIT event is emitted`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.RATE_LIMIT)
    }

    @Test
    fun `given 429 with retry after, when streaming, then RateLimit carries retry delay diagnostics`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "7")
        )

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        val error = failed.kind.toAssistantError(diagnostics = failed.diagnostics)
        assertThat((error as AssistantError.RateLimit).diagnostics.transport?.retryAfterMs).isEqualTo(7_000L)
    }

    @Test
    fun `given 408 response, when streaming, then emits Timeout before generic bad request`() = runTest {
        server.enqueue(MockResponse().setResponseCode(408))
        server.enqueue(MockResponse().setResponseCode(408))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.TIMEOUT)
        assertThat(failed.diagnostics.transport?.httpStatus).isEqualTo(408)
    }

    @Test
    fun `given 400 response, when streaming, then emits BadRequest`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.BAD_REQUEST)
        assertThat(failed.kind.toAssistantError(failed.cause, failed.diagnostics))
            .isNotInstanceOf(AssistantError.Unknown::class.java)
    }

    @Test
    fun `given 400 response, when streaming, then failure carries http status diagnostics`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        val error = failed.kind.toAssistantError(diagnostics = failed.diagnostics)
        assertThat((error as AssistantError.BadRequest).diagnostics.transport?.httpStatus).isEqualTo(400)
    }

    @Test
    fun `given 200 json error with logical 400, when streaming, then emits BadRequest diagnostics`() = runTest {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": "invalid_json_schema",
                  "message": "Invalid schema for function parameters.",
                  "data": {
                    "status": 400
                  }
                }
                """.trimIndent()
            )
        )

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        val error = failed.kind.toAssistantError(diagnostics = failed.diagnostics)
        assertThat(error).isInstanceOf(AssistantError.BadRequest::class.java)
        assertThat((error as AssistantError.BadRequest).diagnostics.transport?.httpStatus).isEqualTo(400)
        assertThat(error.diagnostics.transport?.bodySnippet).contains("invalid_json_schema")
        assertThat(error.diagnostics.transport?.bodySnippet).contains("Invalid schema")
    }

    @Test
    fun `given 200 empty json response, when streaming, then emits BadRequest with transport status`() = runTest {
        server.enqueue(jsonResponse(""))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        val error = failed.kind.toAssistantError(diagnostics = failed.diagnostics)
        assertThat(error).isInstanceOf(AssistantError.BadRequest::class.java)
        assertThat((error as AssistantError.BadRequest).diagnostics.transport?.httpStatus).isEqualTo(200)
        assertThat(error.diagnostics.transport?.bodySnippet).isNull()
    }

    @Test
    fun `given non special 4xx responses, when streaming, then emits BadRequest`() = runTest {
        listOf(402, 404, 422).forEach { code ->
            server.enqueue(MockResponse().setResponseCode(code))
        }

        val service = newService()
        val failures = List(3) {
            service.streamTurn(simpleRequest()).toList().single() as AssistantEvent.Failed
        }

        assertThat(failures.map { it.kind }).containsExactly(
            ChatStreamError.BAD_REQUEST,
            ChatStreamError.BAD_REQUEST,
            ChatStreamError.BAD_REQUEST,
        )
        assertThat(failures.map { it.diagnostics.transport?.httpStatus }).containsExactly(402, 404, 422)
    }

    @Test
    fun `given 503, when streaming, then a Failed UPSTREAM_FAILURE event is emitted`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.UPSTREAM_FAILURE)
    }

    @Test
    fun `given the token provider throws AssistantAuthException, when streaming, then a Failed AUTH event is emitted without retry`() = runTest {
        val tokenProvider = ThrowingTokenProvider(AssistantAuthException("no site selected"))
        val service = newService(tokenProvider = tokenProvider)

        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(ChatStreamError.AUTH)
        assertThat(tokenProvider.provideCalls).isEqualTo(1)
    }

    private fun newService(
        tokenProvider: JwtTokenProvider = this.tokenProvider,
    ): JetpackAiChatService = JetpackAiChatService(
        httpClient = httpClient,
        tokenProvider = tokenProvider,
        streamParser = ChatStreamParser(assistantJson),
        json = assistantJson,
        baseUrl = server.url("/").toString().removeSuffix("/"),
        transportDiagnosticsFactory = TransportDiagnosticsFactory(),
    )

    private fun simpleRequest(): ChatRequest = ChatRequest(
        messages = listOf(AssistantMessage.User("hi")),
    )

    private fun sseResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(body)

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json; charset=UTF-8")
        .setBody(body)

    private class RecordingTokenProvider : JwtTokenProvider {
        var invalidations: Int = 0
        val tokensProvided = mutableListOf<String>()
        private var counter = 0

        override suspend fun provide(): String {
            counter++
            val token = "fake-token-$counter"
            tokensProvided += token
            return token
        }

        override suspend fun invalidate() {
            invalidations++
        }
    }

    private class ThrowingTokenProvider(
        private val exception: AssistantAuthException,
    ) : JwtTokenProvider {
        var provideCalls: Int = 0

        override suspend fun provide(): String {
            provideCalls++
            throw exception
        }
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
