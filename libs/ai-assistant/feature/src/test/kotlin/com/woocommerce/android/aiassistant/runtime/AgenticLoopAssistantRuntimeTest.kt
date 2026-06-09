package com.woocommerce.android.aiassistant.runtime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.automattic.eventhorizon.AiAssistantErrorKindValue
import com.automattic.eventhorizon.AiAssistantToolStatusValue
import com.woocommerce.android.aiassistant.config.AssistantSystemPromptProvider
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
import com.woocommerce.android.aiassistant.core.loop.RetryAffordance
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.loop.ToolDecision
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.core.safety.SafetyDecision
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import com.woocommerce.android.aiassistant.safety.ConfirmationPreview
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewContext
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewField
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProviderRegistry
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProviderRegistryImpl
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewRenderer
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewText
import com.woocommerce.android.aiassistant.safety.OrdersConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreview
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreviewField
import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetryContext
import com.woocommerce.android.aiassistant.telemetry.ShowCardsCounts
import com.woocommerce.android.aiassistant.tools.CachedLookupResult
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCardState
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardUiStructuredParser
import com.woocommerce.android.aiassistant.ui.cards.metric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

@RunWith(RobolectricTestRunner::class)
class AgenticLoopAssistantRuntimeTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    @Test
    fun `when turn starts, then runtime prepends injected system prompt before calling core loop`() = runTest {
        val fakeLoop = CapturingAgenticLoop(events = emptyList())
        val runtime = runtime(agenticLoop = fakeLoop, systemPrompt = "prompt from provider")

        runtime.startTurn(givenTurnRequest()).toList()

        assertThat(fakeLoop.receivedHistory).containsExactly(
            AssistantMessage.System("prompt from provider")
        )
    }

    @Test
    fun `given stale system prompt in history, when turn starts, then runtime replaces it`() = runTest {
        val fakeLoop = CapturingAgenticLoop(events = emptyList())
        val runtime = runtime(agenticLoop = fakeLoop, systemPrompt = "fresh prompt")
        val request = givenTurnRequest().copy(
            history = listOf(
                AssistantMessage.System("stale prompt"),
                AssistantMessage.User("previous"),
                AssistantMessage.Assistant("answer"),
            )
        )

        runtime.startTurn(request).toList()

        assertThat(fakeLoop.receivedHistory).containsExactly(
            AssistantMessage.System("fresh prompt"),
            AssistantMessage.User("previous"),
            AssistantMessage.Assistant("answer"),
        )
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
                        retryAffordance = RetryAffordance.None,
                    )
                )
            ),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.STOPPED,
                updatedHistory = updatedHistory,
                retryAffordance = RetryAffordance.None,
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
        val previewRegistry = FakePreviewRegistry()
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(events = listOf(LoopEvent.ConfirmationRequested(request))),
            toolRegistry = FixedToolRegistry(listOf(orderUpdateDescriptor())),
            confirmationPreviewProviderRegistry = previewRegistry,
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(previewRegistry.receivedContext.request).isEqualTo(request)
        assertThat(previewRegistry.receivedContext.descriptor).isEqualTo(orderUpdateDescriptor())
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
                        message = "Preview message",
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
    fun `given confirmation preview resolves an order name, when loop requests confirmation, then pending card is emitted once with resolved preview`() =
        runTest {
            val request = ConfirmationRequest(
                id = "confirmation-1",
                toolCallId = "call-1",
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 3479)
                    put("status", "pending")
                },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            )
            val ordersDataSource: AIOrdersDataSource = mock()
            whenever(ordersDataSource.getOrders(listOf(3479L))).thenReturn(
                Result.success(
                    CachedLookupResult(
                        items = listOf(
                            OrderEntity(
                                localSiteId = LocalId(1),
                                orderId = 3479L,
                                billingFirstName = "Jane",
                                billingLastName = "Doe",
                            )
                        ),
                        cacheHitCount = 1,
                        cacheMissCount = 0,
                        fetchAttempted = false,
                        fetchFailed = false,
                    )
                )
            )
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(events = listOf(LoopEvent.ConfirmationRequested(request))),
                toolRegistry = FixedToolRegistry(listOf(orderUpdateDescriptor())),
                confirmationPreviewProviderRegistry = ConfirmationPreviewProviderRegistryImpl(
                    setOf(OrdersConfirmationPreviewProvider(mock(), ordersDataSource))
                ),
            )

            val events = runtime.startTurn(givenTurnRequest()).toList()

            val confirmationEvents = events.filterIsInstance<AssistantRuntimeEvent.AwaitingConfirmation>()
            assertThat(confirmationEvents).hasSize(1)
            val confirmation = confirmationEvents.single().confirmation
            assertThat(confirmation.state).isEqualTo(AssistantConfirmationCardState.PENDING)
            assertThat(confirmation.preview?.summary).isEqualTo("Update order #3479 (Jane Doe)")
            val confirmationSummaries = events.mapNotNull {
                (it as? AssistantRuntimeEvent.AwaitingConfirmation)?.confirmation?.preview?.summary
            }
            assertThat(confirmationSummaries).doesNotContain("Update order #3479")
            verify(ordersDataSource).getOrders(listOf(3479L))
            verify(ordersDataSource, never()).getOrder(3479L)
        }

    @Test
    fun `given confirmation descriptor is missing, when runtime builds card, then fallback descriptor is used`() =
        runTest {
            val request = ConfirmationRequest(
                id = "confirmation-1",
                toolCallId = "call-1",
                toolName = "mystery_write",
                arguments = buildJsonObject { put("reason", "Preview") },
                safetyLevel = ToolSafetyLevel.UNSAFE,
            )
            val previewRegistry = FakePreviewRegistry()
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(events = listOf(LoopEvent.ConfirmationRequested(request))),
                toolRegistry = FixedToolRegistry(emptyList()),
                confirmationPreviewProviderRegistry = previewRegistry,
            )

            runtime.startTurn(givenTurnRequest()).toList()

            assertThat(previewRegistry.receivedContext.descriptor).isEqualTo(
                ToolDescriptor(
                    name = "mystery_write",
                    description = "",
                    inputSchema = buildJsonObject {},
                    safetyLevel = ToolSafetyLevel.UNSAFE,
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
                    loopToolCallFinished(result),
                )
            ),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.ToolCallStarted(
                toolCallId = "call-1",
                toolName = "orders_get",
            ),
            AssistantRuntimeEvent.ToolCallFinished(
                toolCallId = "call-1",
                toolName = "orders_get",
                status = AiAssistantToolStatusValue.Success,
                errorKind = null,
                durationMs = null,
                emitTelemetry = true,
                telemetryContext = givenTurnRequest().telemetryContext,
            ),
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
                        retryAffordance = RetryAffordance.None,
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
                retryAffordance = RetryAffordance.None,
                error = null,
            )
        )
    }

    @Test
    fun `when loop emits rejected safety decision, then runtime emits failed tool telemetry event`() = runTest {
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    loopToolCallFinished(
                        result = ToolResult.RejectedBySafety("call-1"),
                        toolName = "orders_update",
                        decision = ToolDecision.REJECTED_BY_SAFETY,
                        durationMs = null,
                    )
                )
            ),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.ToolCallFinished(
                toolCallId = "call-1",
                toolName = "orders_update",
                status = AiAssistantToolStatusValue.Failure,
                errorKind = AiAssistantErrorKindValue.ValidationError,
                durationMs = null,
                emitTelemetry = true,
                telemetryContext = givenTurnRequest().telemetryContext,
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
                    loopToolCallFinished(ToolResult.ValidationError("call-validation", "bad args")),
                    LoopEvent.ToolCallStarted(showCardsCall(id = "call-success")),
                    loopToolCallFinished(
                        ToolResult.Success(
                            toolCallId = "call-success",
                            structured = buildJsonObject { put("rendered", 1) },
                            uiStructured = showCardsUiStructured(orderPayload(id = "123", title = "#123")),
                        )
                    ),
                    LoopEvent.ToolCallStarted(showCardsCall(id = "call-transport")),
                    loopToolCallFinished(ToolResult.TransportError("call-transport", retryable = true)),
                )
            )
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events.cardEvents()).containsExactly(
            AssistantRuntimeEvent.CardsResolved(
                listOf(
                    AssistantCard.Order(
                        remoteOrderId = 123L,
                        number = "#123",
                        status = "processing",
                        total = "12.34",
                        currency = "USD",
                        customerName = "Jane Doe",
                        date = "2026-05-01T10:00:00Z",
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
                    loopToolCallFinished(
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

        assertThat(events.cardEvents()).isEmpty()
    }

    @Test
    fun `given analytics orders success, when adapted, then no stats card is emitted directly`() =
        runTest {
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(
                    events = listOf(
                        LoopEvent.ToolCallStarted(analyticsOrdersCall(id = "call-analytics")),
                        loopToolCallFinished(
                            ToolResult.Success(
                                toolCallId = "call-analytics",
                                structured = analyticsOrdersStructured(),
                            )
                        ),
                    )
                )
            )

            val events = runtime.startTurn(givenTurnRequest()).toList()

            assertThat(events.cardEvents()).isEmpty()
        }

    @Test
    fun `given show cards success with analytics stats, when adapted, then stats card is emitted`() =
        runTest {
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(
                    events = listOf(
                        LoopEvent.ToolCallStarted(showCardsCall(id = "call-stats")),
                        loopToolCallFinished(
                            ToolResult.Success(
                                toolCallId = "call-stats",
                                structured = buildJsonObject { put("rendered", 1) },
                                uiStructured = showCardsUiStructured(analyticsStatsPayload()),
                            )
                        ),
                    )
                )
            )

            val events = runtime.startTurn(givenTurnRequest()).toList()

            assertThat(events.cardEvents()).containsExactly(
                AssistantRuntimeEvent.CardsResolved(
                    listOf(expectedAnalyticsStatsCard())
                )
            )
        }

    @Test
    fun `given show cards success with orders analytics stats, when adapted, then order stats card is emitted`() =
        runTest {
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(
                    listOf(
                        LoopEvent.ToolCallStarted(showCardsCall(id = "call-stats")),
                        loopToolCallFinished(
                            ToolResult.Success(
                                toolCallId = "call-stats",
                                structured = buildJsonObject { put("rendered", 1) },
                                uiStructured = showCardsUiStructured(ordersAnalyticsStatsPayload()),
                            )
                        ),
                    )
                )
            )

            val events = runtime.startTurn(givenTurnRequest()).toList()

            val statsCard = events.cardEvents().single().cards.single() as AssistantCard.Stats
            assertThat(statsCard.id).isEqualTo(ANALYTICS_STATS_ID)
            assertThat(statsCard.metric(AssistantCard.Stats.MetricType.TotalSales).value).isEqualTo("170.35")
            assertThat(statsCard.metric(AssistantCard.Stats.MetricType.NetSales).value).isEqualTo("120.15")
            assertThat(statsCard.metric(AssistantCard.Stats.MetricType.TotalOrders).value).isEqualTo("42")
            assertThat(statsCard.metric(AssistantCard.Stats.MetricType.AverageOrderValue).chartPoints)
                .containsExactly(AssistantCard.Stats.ChartPoint(date = "2026-05-01", value = 80.10))
        }

    @Test
    fun `given analytics orders validation error, when adapted, then no stats card is emitted`() = runTest {
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    LoopEvent.ToolCallStarted(analyticsOrdersCall(id = "call-validation")),
                    loopToolCallFinished(ToolResult.ValidationError("call-validation", "bad args")),
                )
            )
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events.cardEvents()).isEmpty()
    }

    @Test
    fun `given analytics orders success with cards key but no stats fields, when adapted, then no fallback cards are emitted`() =
        runTest {
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(
                    events = listOf(
                        LoopEvent.ToolCallStarted(analyticsOrdersCall(id = "call-arbitrary")),
                        loopToolCallFinished(
                            ToolResult.Success(
                                toolCallId = "call-arbitrary",
                                structured = buildJsonObject {
                                    put("cards", "model visible but not UI cards")
                                },
                                uiStructured = buildJsonObject {
                                    put("cards", "also ignored")
                                },
                            )
                        ),
                    )
                )
            )

            val events = runtime.startTurn(givenTurnRequest()).toList()

            assertThat(events.cardEvents()).isEmpty()
        }

    @Test
    fun `given show cards success with malformed uiStructured, when adapted, then no card event is emitted`() =
        runTest {
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(
                    events = listOf(
                        LoopEvent.ToolCallStarted(showCardsCall(id = "call-malformed")),
                        loopToolCallFinished(
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

            assertThat(events.cardEvents()).isEmpty()
            assertThat(events).contains(
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
                        loopToolCallFinished(
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

            assertThat(events.cardEvents()).containsExactly(
                AssistantRuntimeEvent.CardsResolved(listOf(expectedOrderCard(id = "123", number = "#123")))
            )
        }

    @Test
    fun `given show cards success with empty cards, when adapted, then no card event is emitted`() = runTest {
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    LoopEvent.ToolCallStarted(showCardsCall(id = "call-empty")),
                    loopToolCallFinished(
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

        assertThat(events.cardEvents()).isEmpty()
        assertThat(events).contains(
            AssistantRuntimeEvent.AssistantTextDelta("I could not find matching cards.")
        )
    }

    @Test
    fun `given show cards structured success, when adapted, then aggregate show cards telemetry is emitted`() =
        runTest {
            val request = givenTurnRequest()
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(
                    events = listOf(
                        LoopEvent.ToolCallStarted(showCardsCall(id = "call-show-cards")),
                        loopToolCallFinished(
                            ToolResult.Success(
                                toolCallId = "call-show-cards",
                                structured = showCardsStructured(
                                    requested = 3,
                                    rendered = 1,
                                    missing = 1,
                                    rejected = 1,
                                ),
                                uiStructured = showCardsUiStructured(orderPayload(id = "123", title = "#123")),
                            )
                        ),
                    )
                )
            )

            val events = runtime.startTurn(request).toList()

            assertThat(events.filterIsInstance<AssistantRuntimeEvent.ShowCardsProcessed>()).containsExactly(
                AssistantRuntimeEvent.ShowCardsProcessed(
                    counts = ShowCardsCounts(
                        requestedCount = 3,
                        renderedCount = 1,
                        missingCount = 1,
                        rejectedCount = 1,
                    ),
                    telemetryContext = request.telemetryContext,
                )
            )
        }

    @Test
    fun `given show cards success with malformed structured payload, when adapted, then aggregate telemetry is skipped`() =
        runTest {
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(
                    events = listOf(
                        LoopEvent.ToolCallStarted(showCardsCall(id = "call-malformed")),
                        loopToolCallFinished(
                            ToolResult.Success(
                                toolCallId = "call-malformed",
                                structured = buildJsonObject { put("requested", "not a number") },
                                uiStructured = showCardsUiStructured(orderPayload(id = "123", title = "#123")),
                            )
                        ),
                    )
                )
            )

            val events = runtime.startTurn(givenTurnRequest()).toList()

            assertThat(events.filterIsInstance<AssistantRuntimeEvent.ShowCardsProcessed>()).isEmpty()
        }

    @Test
    fun `given orders list success with json cards key, when adapted, then no fallback cards are emitted`() =
        runTest {
            val runtime = runtime(
                agenticLoop = FakeAgenticLoop(
                    events = listOf(
                        LoopEvent.ToolCallStarted(
                            ToolCall(
                                id = "call-arbitrary",
                                name = "orders_list",
                                arguments = buildJsonObject {},
                            )
                        ),
                        loopToolCallFinished(
                            ToolResult.Success(
                                toolCallId = "call-arbitrary",
                                structured = buildJsonObject {
                                    put("cards", "model visible but not UI cards")
                                },
                                uiStructured = buildJsonObject {
                                    put("cards", "also ignored")
                                },
                            )
                        ),
                    )
                )
            )

            val events = runtime.startTurn(givenTurnRequest()).toList()

            assertThat(events.cardEvents()).isEmpty()
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
        toolRegistry: ToolRegistry = EmptyToolRegistry,
        safetyOrchestrator: SafetyOrchestrator = FakeSafetyOrchestrator(),
        confirmationPreviewProviderRegistry: ConfirmationPreviewProviderRegistry = FakePreviewRegistry(),
        systemPrompt: String = "system prompt v1",
    ) = AgenticLoopAssistantRuntime(
        agenticLoop = agenticLoop,
        toolRegistry = toolRegistry,
        toolCatalogSelector = PassThroughToolCatalogSelector,
        safetyOrchestrator = safetyOrchestrator,
        confirmationPreviewProviderRegistry = confirmationPreviewProviderRegistry,
        confirmationPreviewRenderer = ConfirmationPreviewRenderer(ApplicationProvider.getApplicationContext<Context>()),
        cardParser = AssistantCardUiStructuredParser(json),
        systemPromptProvider = FakeSystemPromptProvider(systemPrompt),
        json = json,
    )

    private fun givenTurnRequest(
        telemetryContext: AssistantTelemetryContext = AssistantTelemetryContext(
            conversationId = "conversation-1",
            requestId = "request-1",
            messageId = "message-1",
        ),
    ) = AssistantTurnRequest(
        conversationId = "conversation-1",
        telemetryContext = telemetryContext,
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

    private fun loopToolCallFinished(
        result: ToolResult,
        toolName: String = "",
        decision: ToolDecision = ToolDecision.EXECUTED,
        durationMs: Long? = null,
    ) = LoopEvent.ToolCallFinished(
        result = result,
        toolName = toolName,
        decision = decision,
        durationMs = durationMs,
    )

    private fun analyticsOrdersCall(id: String) = ToolCall(
        id = id,
        name = "analytics_orders",
        arguments = buildJsonObject {},
    )

    private fun analyticsOrdersStructured() = buildJsonObject {
        put("after", "2026-05-01")
        put("before", "2026-05-03")
        put("currency", "USD")
        putJsonObject("totals") {
            put("total_sales", "170.35")
            put("net_revenue", "123.45")
            put("orders_count", 3)
            put("avg_order_value", "41.15")
        }
    }

    private fun showCardsUiStructured(vararg cards: ShowCardPayload) =
        json.encodeToJsonElement(ShowCardsUiStructured(cards = cards.toList()))

    private fun showCardsStructured(
        requested: Int,
        rendered: Int,
        missing: Int,
        rejected: Int,
    ) = buildJsonObject {
        put("requested", requested)
        put("validated", requested - rejected)
        put("rendered", rendered)
        putJsonArray("resolved_refs") {}
        putJsonArray("missing_refs") {
            repeat(missing) { index ->
                add(
                    buildJsonObject {
                        put("family", "order")
                        put("id", "missing-$index")
                        put("reason", "not_found")
                    }
                )
            }
        }
        putJsonArray("rejected_refs") {
            repeat(rejected) { index ->
                add(
                    buildJsonObject {
                        put("index", index)
                        put("reason", "duplicate_ref")
                    }
                )
            }
        }
    }

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

    private fun analyticsStatsPayload() = ShowCardPayload(
        family = "analytics_stats",
        id = ANALYTICS_STATS_ID,
        title = "Analytics",
        details = ShowCardDetails.AnalyticsStats(
            after = "2026-05-01",
            before = "2026-05-03",
            currency = "USD",
            totals = buildJsonObject {
                put("total_sales", "170.35")
                put("net_revenue", "120.15")
                put("orders_count", "42")
                put("avg_order_value", "85.30")
            },
            intervalSubtotals = listOf(
                buildJsonObject {
                    put("interval", "2026-05-01")
                    putJsonObject("subtotals") {
                        put("total_sales", "100.00")
                        put("net_revenue", "80.00")
                        put("orders_count", "12")
                        put("avg_order_value", "80.10")
                    }
                },
                buildJsonObject {
                    put("interval", "2026-05-02")
                    putJsonObject("subtotals") {
                        put("total_sales", "70.35")
                        put("net_revenue", "40.15")
                        put("orders_count", "30")
                        put("avg_order_value", "87.38")
                    }
                },
            ),
        ),
    )

    private fun ordersAnalyticsStatsPayload() = ShowCardPayload(
        family = "analytics_stats",
        id = ANALYTICS_STATS_ID,
        title = "Analytics",
        details = ShowCardDetails.AnalyticsStats(
            after = "2026-05-01",
            before = "2026-05-03",
            currency = "USD",
            totals = buildJsonObject {
                put("total_sales", "170.35")
                put("net_revenue", "120.15")
                put("orders_count", "42")
                put("avg_order_value", "85.30")
            },
            intervalSubtotals = listOf(
                buildJsonObject {
                    put("interval", "2026-05-01")
                    putJsonObject("subtotals") {
                        put("total_sales", "50.00")
                        put("net_revenue", "35.00")
                        put("orders_count", "12")
                        put("avg_order_value", "80.10")
                    }
                },
            ),
        ),
    )

    private fun expectedOrderCard(id: String, number: String) = AssistantCard.Order(
        remoteOrderId = id.toLong(),
        number = number,
        status = "processing",
        total = "12.34",
        currency = "USD",
        customerName = "Jane Doe",
        date = "2026-05-01T10:00:00Z",
    )

    private fun expectedAnalyticsStatsCard() = AssistantCard.Stats(
        id = ANALYTICS_STATS_ID,
        after = "2026-05-01",
        before = "2026-05-03",
        currency = "USD",
        metrics = listOf(
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.TotalSales,
                value = "170.35",
                chartPoints = listOf(
                    AssistantCard.Stats.ChartPoint(date = "2026-05-01", value = 100.0),
                    AssistantCard.Stats.ChartPoint(date = "2026-05-02", value = 70.35),
                ),
            ),
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.NetSales,
                value = "120.15",
                chartPoints = listOf(
                    AssistantCard.Stats.ChartPoint(date = "2026-05-01", value = 80.0),
                    AssistantCard.Stats.ChartPoint(date = "2026-05-02", value = 40.15),
                ),
            ),
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.TotalOrders,
                value = "42",
                chartPoints = listOf(
                    AssistantCard.Stats.ChartPoint(date = "2026-05-01", value = 12.0),
                    AssistantCard.Stats.ChartPoint(date = "2026-05-02", value = 30.0),
                ),
            ),
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.AverageOrderValue,
                value = "85.30",
                chartPoints = listOf(
                    AssistantCard.Stats.ChartPoint(date = "2026-05-01", value = 80.10),
                    AssistantCard.Stats.ChartPoint(date = "2026-05-02", value = 87.38),
                ),
            ),
        ),
    )

    private fun List<AssistantRuntimeEvent>.cardEvents() =
        filterIsInstance<AssistantRuntimeEvent.CardsResolved>()

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

    private class CapturingAgenticLoop(
        private val events: List<LoopEvent>,
    ) : AgenticLoop {
        lateinit var receivedHistory: List<AssistantMessage>

        override fun runTurn(
            conversationId: String,
            userMessage: String,
            history: List<AssistantMessage>,
            context: SessionContext,
        ): Flow<LoopEvent> {
            receivedHistory = history
            return flowOf(*events.toTypedArray())
        }
    }

    private class FakeSystemPromptProvider(
        private val prompt: String,
    ) : AssistantSystemPromptProvider {
        override fun systemPrompt(todayIsoDate: String?): String = prompt
    }

    private object EmptyToolRegistry : ToolRegistry {
        override fun descriptors(): List<ToolDescriptor> = emptyList()

        override suspend fun execute(call: ToolCall): ToolResult =
            error("Unexpected tool execution in runtime adapter test")
    }

    private class FixedToolRegistry(
        private val descriptors: List<ToolDescriptor>,
    ) : ToolRegistry {
        override fun descriptors(): List<ToolDescriptor> = descriptors

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

    private class FakePreviewRegistry(
        private val preview: ConfirmationPreview = ConfirmationPreview(
            message = ConfirmationPreviewText.Raw("Preview message"),
            fields = listOf(
                ConfirmationPreviewField(
                    name = "status",
                    label = ConfirmationPreviewText.Raw("Status"),
                    value = ConfirmationPreviewText.Raw("processing"),
                    beforeValue = ConfirmationPreviewText.Raw("pending"),
                )
            ),
        ),
    ) : ConfirmationPreviewProviderRegistry {
        lateinit var receivedContext: ConfirmationPreviewContext

        override fun providerFor(context: ConfirmationPreviewContext): ConfirmationPreviewProvider =
            error("providerFor is not used by AgenticLoopAssistantRuntime")

        override suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview {
            receivedContext = context
            return preview
        }
    }

    private fun orderUpdateDescriptor() = ToolDescriptor(
        name = "orders_update",
        description = "Update order",
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("id") { put("type", "integer") }
                putJsonObject("status") { put("type", "string") }
            }
        },
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )

    private companion object {
        private const val ANALYTICS_STATS_ID =
            "analytics_orders:after:2026-05-01:before:2026-05-03:interval:day"
    }
}
