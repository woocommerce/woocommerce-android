package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.auth.WpComOAuthTokenProvider
import com.woocommerce.android.aiassistant.chat.woomobileai.WooMobileAiWrapperErrorMapper
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.headless.HeadlessScenario
import com.woocommerce.android.aiassistant.core.headless.HeadlessToolResultKind
import com.woocommerce.android.aiassistant.core.headless.HeadlessTurnSpec
import com.woocommerce.android.aiassistant.core.headless.RecordingHeadlessToolRegistry
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
import kotlin.time.TimeSource

class WooMobileAiChatServiceHeadlessHarnessTest {
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
                chatService = newService(),
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
            val request = server.takeRequest()
            assertThat(request.path).isEqualTo("/wpcom/v2/woo-mobile-ai/chat/completions")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-token")
        }

    @Test
    fun `given real chat service streams tool call, when headless harness runs, then it executes tool and continues`() =
        runTest {
            server.enqueue(toolCallSseResponse())
            server.enqueue(sseResponse())
            val toolDescriptor = lookupProductsToolDescriptor()
            val toolRegistry = RecordingHeadlessToolRegistry(
                descriptors = listOf(toolDescriptor),
                results = mapOf(
                    "lookup_products" to ToolResult.Success(
                        toolCallId = "call_1",
                        structured = buildJsonObject {
                            put("count", 1)
                        },
                    )
                ),
            )
            val harness = WooAssistantHeadless(
                chatService = newService(),
                toolRegistry = toolRegistry,
                retryPolicy = ConservativeRetryPolicy,
                historyBudgeter = HistoryBudgeter { system, transcript, user ->
                    BudgetedHistory(listOf(system) + transcript + user)
                },
                json = assistantJson,
                timeSource = TimeSource.Monotonic,
            )

            val result = harness.runScenario(
                HeadlessScenario(
                    id = "tool-transport-smoke",
                    turns = listOf(
                        HeadlessTurnSpec(
                            userMessage = "Find shirts",
                            hardChecks = emptyList(),
                        )
                    ),
                    initialHistory = listOf(AssistantMessage.System("You are helpful.")),
                    context = SessionContext(
                        siteId = 1L,
                        catalogSnapshot = CatalogSnapshot(
                            scope = ToolScope.GLOBAL,
                            tools = listOf(toolDescriptor),
                        ),
                    ),
                )
            )

            val turn = result.turns.single()
            assertThat(turn.outcome).isEqualTo(LoopOutcome.COMPLETED)
            assertThat(turn.assistantText).isEqualTo("Hello world")
            assertThat(turn.toolCalls.single().name).isEqualTo("lookup_products")
            assertThat(turn.toolCalls.single().arguments.getValue("query").jsonPrimitive.content)
                .isEqualTo("shirt")
            assertThat(turn.toolCalls.single().resultKind).isEqualTo(HeadlessToolResultKind.SUCCESS)

            val firstRequest = server.takeRequest()
            val secondRequest = server.takeRequest()
            assertThat(firstRequest.getHeader("Authorization")).isEqualTo("Bearer test-token")
            assertThat(secondRequest.getHeader("Authorization")).isEqualTo("Bearer test-token")
            assertThat(toolRegistry.calls.single().name).isEqualTo("lookup_products")
        }

    private fun newService(): WooMobileAiChatService = WooMobileAiChatService(
        httpClient = httpClient,
        tokenProvider = StaticTokenProvider,
        streamParser = ChatStreamParser(assistantJson),
        json = assistantJson,
        baseUrl = server.url("/").toString().removeSuffix("/"),
        transportDiagnosticsFactory = TransportDiagnosticsFactory(),
        wrapperErrorMapper = WooMobileAiWrapperErrorMapper(assistantJson),
    )

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

    private fun toolCallSseResponse(): MockResponse {
        val toolCallChunk = """{"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1",""" +
            """"function":{"name":"lookup_products","arguments":"{\"query\":\"shirt\"}"}}]}}]}"""
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                """
                data: $toolCallChunk

                data: {"choices":[{"finish_reason":"tool_calls"}]}

                data: [DONE]

                """.trimIndent()
            )
    }

    private fun lookupProductsToolDescriptor() = ToolDescriptor(
        name = "lookup_products",
        description = "Looks up products.",
        inputSchema = JsonObject(
            mapOf(
                "type" to JsonPrimitive("object"),
            )
        ),
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    private object StaticTokenProvider : WpComOAuthTokenProvider {
        override suspend fun provide(): String = "test-token"
    }
}
