package com.woocommerce.android.aiassistant.runtime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.CatalogSnapshot
import com.woocommerce.android.aiassistant.core.loop.LoopEvent
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.core.safety.SafetyDecision
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewRenderer
import com.woocommerce.android.aiassistant.safety.ConfirmationSnapshot
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreview
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreviewField
import com.woocommerce.android.aiassistant.safety.WooCommerceConfirmationPreviewBuilder
import com.woocommerce.android.aiassistant.safety.WooCommerceConfirmationSnapshotResolver
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductVariationsDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCardState
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardEntry
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardKey
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardUiStructuredParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AgenticLoopAssistantRuntimeTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    @Test
    fun `when agentic loop finishes with max iterations, then runtime preserves outcome`() = runTest {
        val updatedHistory = listOf(AssistantMessage.User("Hello"))
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    LoopEvent.Finished(
                        outcome = LoopOutcome.MAX_ITERATIONS,
                        updatedHistory = updatedHistory,
                    )
                )
            ),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.MAX_ITERATIONS,
                updatedHistory = updatedHistory,
            )
        )
    }

    @Test
    fun `when loop fails with cancelled before stopped finish, then runtime finish includes cancelled error`() = runTest {
        val updatedHistory = listOf(
            AssistantMessage.User("Hello"),
            AssistantMessage.Assistant("Partial"),
        )
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    LoopEvent.Failed(AssistantError.Cancelled),
                    LoopEvent.Finished(
                        outcome = LoopOutcome.STOPPED,
                        updatedHistory = updatedHistory,
                        retryAvailable = false,
                    )
                )
            ),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.STOPPED,
                updatedHistory = updatedHistory,
                retryAvailable = false,
                error = AssistantError.Cancelled,
            )
        )
    }

    @Test
    fun `when loop requests confirmation, then runtime emits inline confirmation card data`() = runTest {
        val request = ConfirmationRequest(
            id = "confirmation-1",
            toolCallId = "call-1",
            toolName = "orders_update",
            arguments = buildJsonObject {
                put("id", 123)
                put("status", "processing")
            },
            safetyLevel = ToolSafetyLevel.UNSAFE,
        )
        val snapshot = ConfirmationSnapshot(
            currentValues = mapOf(
                "status" to "pending",
            )
        )
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(events = listOf(LoopEvent.ConfirmationRequested(request))),
            snapshotResolver = FakeSnapshotResolver(snapshot),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.AwaitingConfirmation(
                AssistantConfirmationCard(
                    confirmationId = "confirmation-1",
                    toolCall = ToolCall(
                        id = "call-1",
                        name = "orders_update",
                        arguments = buildJsonObject {
                            put("id", 123)
                            put("status", "processing")
                        },
                    ),
                    state = AssistantConfirmationCardState.PENDING,
                    preview = RenderedConfirmationPreview(
                        message = "Set order #123 to processing (emails the customer)",
                        fields = listOf(
                            RenderedConfirmationPreviewField(
                                name = "status",
                                label = "Status",
                                value = "processing",
                                beforeValue = "pending",
                            )
                        ),
                        isBulk = false,
                    ),
                )
            )
        )
    }

    @Test
    fun `when loop resolves confirmation, then runtime forwards the resolution event`() = runTest {
        val resolved = ConfirmationResult("confirmation-1", ConfirmationDecision.CONFIRMED)
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(events = listOf(LoopEvent.ConfirmationResolved(resolved))),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.ConfirmationResolved(resolved)
        )
    }

    @Test
    fun `when loop emits tool lifecycle, then runtime forwards sanitized tool activity`() = runTest {
        val call = ToolCall(
            id = "call-1",
            name = "orders_get",
            arguments = buildJsonObject {
                put("private_order_id", 123)
            },
        )
        val result = ToolResult.Success(
            toolCallId = "call-1",
            structured = buildJsonObject {
                put("status", "processing")
            },
        )
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    LoopEvent.ToolCallStarted(call),
                    LoopEvent.ToolCallFinished(result),
                )
            ),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.ToolCallStarted(
                toolCallId = "call-1",
                toolName = "orders_get",
            ),
            AssistantRuntimeEvent.ToolCallFinished(toolCallId = "call-1"),
        )
    }

    @Test
    fun `when loop stops cleanly after confirmation cancel, then runtime finish has no cancelled error`() = runTest {
        val updatedHistory = listOf(
            AssistantMessage.User("Cancel order 123"),
            AssistantMessage.Assistant("I can do that"),
        )
        val resolved = ConfirmationResult("confirmation-1", ConfirmationDecision.CANCELLED)
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    LoopEvent.ConfirmationResolved(resolved),
                    LoopEvent.Finished(
                        outcome = LoopOutcome.STOPPED,
                        updatedHistory = updatedHistory,
                        retryAvailable = false,
                    )
                )
            ),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.ConfirmationResolved(resolved),
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.STOPPED,
                updatedHistory = updatedHistory,
                retryAvailable = false,
                error = null,
            )
        )
    }

    @Test
    fun `when confirmation is resolved, then runtime forwards the core confirmation result to safety orchestrator`() =
        runTest {
            val safetyOrchestrator = FakeSafetyOrchestrator()
            val runtime = runtime(safetyOrchestrator = safetyOrchestrator)
            val result = ConfirmationResult("confirmation-1", ConfirmationDecision.CONFIRMED)

            val dispatchResult = runtime.resolveConfirmation(result)

            assertThat(dispatchResult).isEqualTo(AssistantRuntimeConfirmationDispatchResult.Accepted)
            assertThat(safetyOrchestrator.results).containsExactly(result)
        }

    @Test
    fun `when confirmation is missing, then runtime reports deferred confirmation`() = runTest {
        val runtime = runtime(safetyOrchestrator = FakeSafetyOrchestrator(resolveResult = false))
        val result = ConfirmationResult("missing", ConfirmationDecision.CONFIRMED)

        val dispatchResult = runtime.resolveConfirmation(result)

        assertThat(dispatchResult).isEqualTo(AssistantRuntimeConfirmationDispatchResult.Deferred)
    }

    @Test
    fun `given show cards non success and success results, when adapted, then only success emits cards`() = runTest {
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    LoopEvent.ToolCallStarted(showCardsCall(id = "call-validation")),
                    LoopEvent.ToolCallFinished(ToolResult.ValidationError("call-validation", "bad args")),
                    LoopEvent.ToolCallStarted(showCardsCall(id = "call-success")),
                    LoopEvent.ToolCallFinished(
                        ToolResult.Success(
                            toolCallId = "call-success",
                            structured = buildJsonObject { put("rendered", 1) },
                            uiStructured = showCardsUiStructured(orderPayload(id = "123", title = "#123")),
                        )
                    ),
                    LoopEvent.ToolCallStarted(showCardsCall(id = "call-transport")),
                    LoopEvent.ToolCallFinished(ToolResult.TransportError("call-transport", retryable = true)),
                )
            )
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.CardsResolved(
                listOf(
                    AssistantCardEntry(
                        key = AssistantCardKey(family = "order", id = "123"),
                        card = AssistantCard.Order(
                            remoteOrderId = 123L,
                            number = "#123",
                            status = "processing",
                            total = "12.34",
                            currency = "USD",
                            customerName = "Jane Doe",
                            date = "2026-05-01T10:00:00Z",
                        ),
                    )
                )
            )
        )
    }

    @Test
    fun `given non show cards success with card shaped uiStructured, when adapted, then cards are ignored`() = runTest {
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    LoopEvent.ToolCallStarted(
                        ToolCall(
                            id = "call-orders",
                            name = "orders_list",
                            arguments = buildJsonObject {},
                        )
                    ),
                    LoopEvent.ToolCallFinished(
                        ToolResult.Success(
                            toolCallId = "call-orders",
                            structured = buildJsonObject { put("ok", true) },
                            uiStructured = showCardsUiStructured(orderPayload(id = "123", title = "#123")),
                        )
                    ),
                )
            )
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).isEmpty()
    }

    @Test
    fun `given show cards success with malformed uiStructured, when adapted, then no card event is emitted`() =
        runTest {
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(
                    events = listOf(
                        LoopEvent.ToolCallStarted(showCardsCall(id = "call-malformed")),
                        LoopEvent.ToolCallFinished(
                            ToolResult.Success(
                                toolCallId = "call-malformed",
                                structured = buildJsonObject { put("rendered", 1) },
                                uiStructured = buildJsonObject { put("cards", "not an array") },
                            )
                        ),
                        LoopEvent.Finished(
                            outcome = LoopOutcome.COMPLETED,
                            updatedHistory = listOf(AssistantMessage.User("Hello")),
                        ),
                    )
                )
            )

            val events = runtime.startTurn(givenTurnRequest()).toList()

            assertThat(events).containsExactly(
                AssistantRuntimeEvent.Finished(
                    outcome = LoopOutcome.COMPLETED,
                    updatedHistory = listOf(AssistantMessage.User("Hello")),
                )
            )
        }

    @Test
    fun `given show cards success with valid and unsupported cards, when adapted, then valid cards are emitted`() =
        runTest {
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(
                    events = listOf(
                        LoopEvent.ToolCallStarted(showCardsCall(id = "call-partial")),
                        LoopEvent.ToolCallFinished(
                            ToolResult.Success(
                                toolCallId = "call-partial",
                                structured = buildJsonObject { put("rendered", 1) },
                                uiStructured = showCardsUiStructured(
                                    orderPayload(id = "123", title = "#123"),
                                    ShowCardPayload(
                                        family = "customer",
                                        id = "456",
                                        title = "Customer",
                                        details = ShowCardDetails.Product(),
                                    ),
                                ),
                            )
                        ),
                    )
                )
            )

            val events = runtime.startTurn(givenTurnRequest()).toList()

            assertThat(events).containsExactly(
                AssistantRuntimeEvent.CardsResolved(listOf(expectedOrderEntry(id = "123", number = "#123")))
            )
        }

    @Test
    fun `given show cards success with empty cards, when adapted, then no card event is emitted`() = runTest {
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    LoopEvent.ToolCallStarted(showCardsCall(id = "call-empty")),
                    LoopEvent.ToolCallFinished(
                        ToolResult.Success(
                            toolCallId = "call-empty",
                            structured = buildJsonObject { put("rendered", 0) },
                            uiStructured = showCardsUiStructured(),
                        )
                    ),
                    LoopEvent.AssistantTextDelta("I could not find matching cards."),
                )
            )
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.AssistantTextDelta("I could not find matching cards.")
        )
    }

    @Test
    fun `when cancelled confirmation is resolved, then runtime forwards the cancellation result to safety orchestrator`() =
        runTest {
            val safetyOrchestrator = FakeSafetyOrchestrator()
            val runtime = runtime(safetyOrchestrator = safetyOrchestrator)
            val result = ConfirmationResult("confirmation-1", ConfirmationDecision.CANCELLED)

            runtime.resolveConfirmation(result)

            assertThat(safetyOrchestrator.results).containsExactly(result)
        }

    private fun runtime(
        agenticLoop: AgenticLoop = FakeAgenticLoop(events = emptyList()),
        safetyOrchestrator: SafetyOrchestrator = FakeSafetyOrchestrator(),
        snapshotResolver: WooCommerceConfirmationSnapshotResolver = FakeSnapshotResolver(),
    ) = AgenticLoopAssistantRuntime(
        agenticLoop = agenticLoop,
        toolRegistry = EmptyToolRegistry,
        toolCatalogSelector = PassThroughToolCatalogSelector,
        safetyOrchestrator = safetyOrchestrator,
        confirmationPreviewBuilder = WooCommerceConfirmationPreviewBuilder(),
        confirmationPreviewRenderer = ConfirmationPreviewRenderer(ApplicationProvider.getApplicationContext<Context>()),
        confirmationSnapshotResolver = snapshotResolver,
        cardParser = AssistantCardUiStructuredParser(json),
    )

    private fun givenTurnRequest() = AssistantTurnRequest(
        conversationId = "conversation-1",
        siteId = 123L,
        toolScope = ToolScope.GLOBAL,
        userMessage = "Hello",
        history = emptyList(),
    )

    private fun showCardsCall(id: String) = ToolCall(
        id = id,
        name = "show_cards",
        arguments = buildJsonObject {},
    )

    private fun showCardsUiStructured(vararg cards: ShowCardPayload) =
        json.encodeToJsonElement(ShowCardsUiStructured(cards = cards.toList()))

    private fun orderPayload(id: String, title: String) = ShowCardPayload(
        family = "order",
        id = id,
        title = title,
        details = ShowCardDetails.Order(
            status = "processing",
            total = "12.34",
            currency = "USD",
            dateCreated = "2026-05-01T10:00:00Z",
            customerName = "Jane Doe",
        ),
    )

    private fun expectedOrderEntry(id: String, number: String) = AssistantCardEntry(
        key = AssistantCardKey(family = "order", id = id),
        card = AssistantCard.Order(
            remoteOrderId = id.toLong(),
            number = number,
            status = "processing",
            total = "12.34",
            currency = "USD",
            customerName = "Jane Doe",
            date = "2026-05-01T10:00:00Z",
        ),
    )

    private class FakeAgenticLoop(
        private val events: List<LoopEvent>,
    ) : AgenticLoop {
        override fun runTurn(
            conversationId: String,
            userMessage: String,
            history: List<AssistantMessage>,
            context: SessionContext,
        ): Flow<LoopEvent> = flowOf(*events.toTypedArray())
    }

    private object EmptyToolRegistry : ToolRegistry {
        override fun descriptors(): List<ToolDescriptor> = emptyList()

        override suspend fun execute(call: ToolCall): ToolResult =
            error("Unexpected tool execution in runtime adapter test")
    }

    private object PassThroughToolCatalogSelector : ToolCatalogSelector {
        override fun select(scope: ToolScope, fullRegistry: List<ToolDescriptor>): CatalogSnapshot =
            CatalogSnapshot(scope = scope, tools = fullRegistry)
    }

    private class FakeSafetyOrchestrator(
        private val resolveResult: Boolean = true,
    ) : SafetyOrchestrator {
        val results = mutableListOf<ConfirmationResult>()

        override suspend fun evaluate(call: ToolCall, descriptor: ToolDescriptor): SafetyDecision =
            error("Unexpected safety evaluation in runtime adapter test")

        override suspend fun awaitResult(requestId: String): ConfirmationResult =
            error("Unexpected confirmation await in runtime adapter test")

        override fun resolve(result: ConfirmationResult): Boolean {
            results += result
            return resolveResult
        }

        override fun cancelPending(requestId: String): Boolean = false
    }

    private class FakeSnapshotResolver(
        private val snapshot: ConfirmationSnapshot? = null,
    ) : WooCommerceConfirmationSnapshotResolver(
        ordersDataSource = mock<AIOrdersDataSource>(),
        productsDataSource = mock<AIProductsDataSource>(),
        variationsDataSource = mock<AIProductVariationsDataSource>(),
    ) {
        override suspend fun resolve(request: ConfirmationRequest): ConfirmationSnapshot? = snapshot
    }
}
