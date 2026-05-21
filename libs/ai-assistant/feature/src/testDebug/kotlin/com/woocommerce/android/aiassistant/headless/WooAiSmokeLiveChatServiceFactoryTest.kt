@file:Suppress("FunctionNaming", "MagicNumber")

package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.auth.WpComOAuthTokenProvider
import com.woocommerce.android.aiassistant.chat.ChatStreamParser
import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory
import com.woocommerce.android.aiassistant.chat.assistantJsonForTests
import com.woocommerce.android.aiassistant.chat.woomobileai.WooMobileAiWrapperErrorMapper
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class WooAiSmokeLiveChatServiceFactoryTest {
    private lateinit var server: MockWebServer

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
    fun `when service streams turn, then chat uses woo mobile ai wrapper without jwt mint`() = runTest {
        server.enqueue(sseResponse(SAMPLE_SSE_BODY))

        val events = service().streamTurn(ChatRequest(listOf(AssistantMessage.User("hi")))).toList()

        val chatRequest = server.takeRequest()
        assertThat(events).contains(AssistantEvent.TextDelta("Hello"))
        assertThat(chatRequest.path).isEqualTo("/wpcom/v2/woo-mobile-ai/chat/completions")
        assertThat(chatRequest.getHeader("Authorization")).isEqualTo("Bearer wpcom-token")
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `given chat error echoes bearer token, when service streams turn, then diagnostics redact token`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"Authorization: Bearer wpcom-token"}""")
        )

        val events = service().streamTurn(ChatRequest(listOf(AssistantMessage.User("hi")))).toList()

        val failure = events.filterIsInstance<AssistantEvent.Failed>().single()
        assertThat(failure.diagnostics.transport?.bodySnippet)
            .contains("[REDACTED]")
            .doesNotContain("wpcom-token")
            .doesNotContain("Bearer wpcom-token")
    }

    private fun service(): ChatService {
        val json = assistantJsonForTests()
        return WooAiSmokeLiveChatServiceFactory(
            httpClient = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .build(),
            streamParser = ChatStreamParser(json),
            json = json,
            baseUrl = server.url("/").toString().removeSuffix("/"),
            transportDiagnosticsFactory = TransportDiagnosticsFactory(),
            tokenProvider = FakeWpComOAuthTokenProvider(),
            wrapperErrorMapper = WooMobileAiWrapperErrorMapper(json),
        ).create(
            credentials = WooAiSmokeCredentialConfig(
                siteUrl = server.url("/").toString(),
                siteId = 2922L,
                username = "merchant@example.com",
                appPassword = "app password",
                storeLabel = "store",
                outputDirectory = File("build/woo-ai-smoke"),
                credentialSource = "test",
            ),
            redactor = WooAiSmokeRedactor(
                siteUrl = server.url("/").toString(),
                username = "merchant@example.com",
                appPassword = "app password",
            ),
        )
    }

    private fun sseResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(body)

    private class FakeWpComOAuthTokenProvider : WpComOAuthTokenProvider {
        override suspend fun provide(): String = "wpcom-token"
    }

    private companion object {
        private val SAMPLE_SSE_BODY = """
            data: {"choices":[{"delta":{"content":"Hello"}}]}

            data: {"choices":[{"finish_reason":"stop"}]}

            data: [DONE]

        """.trimIndent()
    }
}
