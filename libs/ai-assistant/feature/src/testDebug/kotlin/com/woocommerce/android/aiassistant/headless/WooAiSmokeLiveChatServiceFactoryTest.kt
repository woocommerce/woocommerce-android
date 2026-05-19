@file:Suppress("FunctionNaming", "MagicNumber")

package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.chat.ChatStreamParser
import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory
import com.woocommerce.android.aiassistant.chat.assistantJsonForTests
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
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
    fun `when service streams turn, then jwt uses basic auth and chat uses bearer auth`() = runTest {
        server.enqueue(jsonResponse("""{"token":"jwt-token"}"""))
        server.enqueue(sseResponse(SAMPLE_SSE_BODY))
        val json = assistantJsonForTests()
        val service = WooAiSmokeLiveChatServiceFactory(
            httpClient = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .build(),
            streamParser = ChatStreamParser(json),
            json = json,
            baseUrl = server.url("/").toString().removeSuffix("/"),
            transportDiagnosticsFactory = TransportDiagnosticsFactory(),
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

        val events = service.streamTurn(ChatRequest(listOf(AssistantMessage.User("hi")))).toList()

        val jwtRequest = server.takeRequest()
        val chatRequest = server.takeRequest()
        assertThat(events).contains(AssistantEvent.TextDelta("Hello"))
        assertThat(jwtRequest.path).isEqualTo("/wp-json/jetpack/v4/jetpack-ai-jwt")
        assertThat(jwtRequest.getHeader("Authorization")).startsWith("Basic ")
        assertThat(chatRequest.path).isEqualTo("/wpcom/v2/jetpack-ai-query")
        assertThat(chatRequest.getHeader("Authorization")).isEqualTo("Bearer jwt-token")
    }

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun sseResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(body)

    private companion object {
        private val SAMPLE_SSE_BODY = """
            data: {"choices":[{"delta":{"content":"Hello"}}]}

            data: {"choices":[{"finish_reason":"stop"}]}

            data: [DONE]

        """.trimIndent()
    }
}
