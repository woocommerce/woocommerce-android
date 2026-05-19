package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenario
import com.woocommerce.android.aiassistant.core.headless.HeadlessTurnSpec
import com.woocommerce.android.aiassistant.core.headless.WooAssistantHeadless
import com.woocommerce.android.aiassistant.core.loop.BudgetedHistory
import com.woocommerce.android.aiassistant.core.loop.CatalogSnapshot
import com.woocommerce.android.aiassistant.core.loop.ConservativeRetryPolicy
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.NoOpToolRegistry
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.time.TimeSource

class JetpackAiChatServiceHeadlessHarnessTest {
    private lateinit var server: MockWebServer
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
    fun `given real chat service with mocked SSE response, when headless harness runs, then it captures assistant text`() =
        runTest {
            server.enqueue(sseResponse())
            val harness = WooAssistantHeadless(
                chatService = JetpackAiChatService(
                    httpClient = httpClient,
                    tokenProvider = StaticTokenProvider,
                    streamParser = ChatStreamParser(assistantJson),
                    json = assistantJson,
                    baseUrl = server.url("/").toString().removeSuffix("/"),
                    transportDiagnosticsFactory = TransportDiagnosticsFactory(),
                ),
                toolRegistry = NoOpToolRegistry(),
                retryPolicy = ConservativeRetryPolicy,
                historyBudgeter = HistoryBudgeter { system, transcript, user ->
                    BudgetedHistory(listOf(system) + transcript + user)
                },
                json = assistantJson,
                timeSource = TimeSource.Monotonic,
            )

            val result = harness.runScenario(
                HeadlessScenario(
                    id = "transport-smoke",
                    turns = listOf(
                        HeadlessTurnSpec(
                            userMessage = "Say hi",
                            hardChecks = emptyList(),
                        )
                    ),
                    initialHistory = listOf(AssistantMessage.System("You are helpful.")),
                    context = SessionContext(
                        siteId = 1L,
                        catalogSnapshot = CatalogSnapshot(ToolScope.GLOBAL, emptyList()),
                    ),
                )
            )

            assertThat(result.turns.single().outcome).isEqualTo(LoopOutcome.COMPLETED)
            assertThat(result.turns.single().assistantText).isEqualTo("Hello world")
            assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer test-token")
        }

    private fun sseResponse(): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(
            """
            data: {"choices":[{"delta":{"content":"Hello"}}]}

            data: {"choices":[{"delta":{"content":" world"}}]}

            data: {"choices":[{"finish_reason":"stop"}]}

            data: [DONE]

            """.trimIndent()
        )

    private object StaticTokenProvider : JwtTokenProvider {
        override suspend fun provide(): String = "test-token"
        override suspend fun invalidate() = Unit
    }
}
