package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider
import com.woocommerce.android.aiassistant.core.chat.AssistantErrorKind
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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
        assertThat(body).contains(""""feature":"woo-ai-assistant"""")
        assertThat(body).contains(""""stream":true""")
        assertThat(body).contains(""""model":"""")
        assertThat(body).contains(""""messages":[""")
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
        assertThat(failed.kind).isEqualTo(AssistantErrorKind.AUTH)
        assertThat(tokenProvider.invalidations).isEqualTo(1)
    }

    @Test
    fun `given 429, when streaming, then a Failed RATE_LIMIT event is emitted`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(AssistantErrorKind.RATE_LIMIT)
    }

    @Test
    fun `given 503, when streaming, then a Failed UPSTREAM_FAILURE event is emitted`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val service = newService()
        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(AssistantErrorKind.UPSTREAM_FAILURE)
    }

    @Test
    fun `given the token provider throws AssistantAuthException, when streaming, then a Failed AUTH event is emitted`() = runTest {
        val service = newService(
            tokenProvider = object : JwtTokenProvider {
                override suspend fun provide(): String =
                    throw AssistantAuthException("no site selected")
            },
        )

        val events = service.streamTurn(simpleRequest()).toList()

        val failed = events.single() as AssistantEvent.Failed
        assertThat(failed.kind).isEqualTo(AssistantErrorKind.AUTH)
    }

    private fun newService(
        tokenProvider: JwtTokenProvider = this.tokenProvider,
    ): JetpackAiChatService = JetpackAiChatService(
        httpClient = httpClient,
        tokenProvider = tokenProvider,
        streamParser = ChatStreamParser(Json { ignoreUnknownKeys = true }),
        baseUrl = server.url("/").toString().removeSuffix("/"),
    )

    private fun simpleRequest(): ChatRequest = ChatRequest(
        messages = listOf(AssistantMessage.User("hi")),
    )

    private fun sseResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
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

    companion object {
        private val SAMPLE_SSE_BODY = """
            data: {"choices":[{"delta":{"content":"Hello"}}]}

            data: {"choices":[{"delta":{"content":" world"}}]}

            data: {"choices":[{"finish_reason":"stop"}]}

            data: [DONE]


        """.trimIndent()
    }
}
