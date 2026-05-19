package com.woocommerce.android.aiassistant.ui

import com.automattic.eventhorizon.AiAssistantCardTappedEvent
import com.automattic.eventhorizon.AiAssistantConversationStartedEvent
import com.automattic.eventhorizon.AiAssistantErrorKindValue
import com.automattic.eventhorizon.AiAssistantShowCardsProcessedEvent
import com.automattic.eventhorizon.AiAssistantToolCallCompletedEvent
import com.automattic.eventhorizon.AiAssistantToolStatusValue
import com.automattic.eventhorizon.AiAssistantTurnCompletedEvent
import com.automattic.eventhorizon.AiAssistantTurnOutcomeValue
import com.automattic.eventhorizon.AiAssistantTurnStartedEvent
import com.automattic.eventhorizon.Trackable
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.Diagnostics
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.TransportDiagnostics
import com.woocommerce.android.aiassistant.core.history.AssistantSessionHistory
import com.woocommerce.android.aiassistant.core.history.AssistantSessionHistoryMapper
import com.woocommerce.android.aiassistant.core.history.AssistantSessionMessage
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.RetryAffordance
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.runtime.AssistantRuntime
import com.woocommerce.android.aiassistant.runtime.AssistantRuntimeConfirmationDispatchResult
import com.woocommerce.android.aiassistant.runtime.AssistantRuntimeEvent
import com.woocommerce.android.aiassistant.runtime.AssistantTurnRequest
import com.woocommerce.android.aiassistant.telemetry.AssistantIdGenerator
import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetryContext
import com.woocommerce.android.aiassistant.telemetry.RecordingAssistantTelemetryTracker
import com.woocommerce.android.aiassistant.telemetry.ShowCardsCounts
import com.woocommerce.android.aiassistant.tools.handlers.cards.SHOW_CARDS_TOOL_NAME
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class AssistantViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var runtime: FakeAssistantRuntime
    private lateinit var selectedSite: SelectedSite
    private lateinit var assistantIdGenerator: AssistantIdGenerator
    private lateinit var assistantTelemetryTracker: RecordingAssistantTelemetryTracker
    private lateinit var timeSource: TestTimeSource
    private lateinit var viewModel: AssistantViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runtime = FakeAssistantRuntime()
        selectedSite = mock {
            on { get() } doReturn SiteModel().apply { siteId = SITE_ID }
        }
        assistantIdGenerator = sequentialAssistantIdGenerator()
        assistantTelemetryTracker = RecordingAssistantTelemetryTracker()
        timeSource = TestTimeSource()
        viewModel = AssistantViewModel(
            runtime = runtime,
            selectedSite = selectedSite,
            assistantTelemetryTracker = assistantTelemetryTracker,
            assistantTelemetryTimeSource = timeSource,
            assistantIdGenerator = assistantIdGenerator,
            sessionHistoryMapper = AssistantSessionHistoryMapper(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when initialized, then state is idle`() {
        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.IDLE)
        assertThat(viewModel.uiState.value.messages).isEmpty()
        assertThat(viewModel.uiState.value.activeConfirmationId).isNull()
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `when message is sent, then user and active assistant messages are added and runtime starts turn`() = runTest {
        viewModel.onSendMessage("Show my recent orders")

        val state = viewModel.uiState.value
        assertThat(state.status).isEqualTo(AssistantUiStatus.STREAMING)
        assertThat(state.isTurnActive).isTrue()
        assertThat(state.messages).containsExactly(
            AssistantUiMessage(
                id = "assistant-id-2",
                role = AssistantUiMessage.Role.USER,
                text = "Show my recent orders",
            ),
            AssistantUiMessage(id = "assistant-id-3", role = AssistantUiMessage.Role.ASSISTANT, text = ""),
        )
        assertThat(runtime.startRequests).containsExactly(
            expectedTurnRequest(
                requestId = "assistant-id-4",
                messageId = "assistant-id-3",
                userMessage = "Show my recent orders",
            )
        )
    }

    @Test
    fun `given idle view model, when first non-empty message is sent, then context reuses assistant message id and has fresh telemetry ids`() =
        runTest {
            viewModel.onSendMessage("Show my recent orders")

            val context = runtime.startRequests.single().telemetryContext

            assertThat(context.messageId).isEqualTo("assistant-id-3")
            assertThat(context.conversationId).isEqualTo(CONVERSATION_ID)
            assertThat(context.requestId).isEqualTo("assistant-id-4")
            assertThat(context.conversationId).isNotEqualTo(context.requestId)
            assertThat(context.requestId).isNotEqualTo(context.messageId)
        }

    @Test
    fun `given existing conversation, when second message is sent, then conversation id is stable and request id is new`() =
        runTest {
            viewModel.onSendMessage("first")
            runtime.emitTurnFinished()
            viewModel.onSendMessage("second")

            val starts = runtime.startRequests

            assertThat(starts[0].telemetryContext.conversationId)
                .isEqualTo(starts[1].telemetryContext.conversationId)
            assertThat(starts[0].telemetryContext.requestId).isNotEqualTo(starts[1].telemetryContext.requestId)
        }

    @Test
    fun `given conversation in progress, when restart and then send, then conversation id rotates`() = runTest {
        viewModel.onSendMessage("first")
        val first = runtime.startRequests.single().telemetryContext.conversationId

        viewModel.onRestartConversation()
        viewModel.onSendMessage("second")
        val second = runtime.startRequests.last().telemetryContext.conversationId

        assertThat(second).isNotEqualTo(first)
    }

    @Test
    fun `given blank input, when sent, then no turn_started is emitted`() = runTest {
        viewModel.onSendMessage("   ")

        assertThat(assistantTelemetryTracker.events.filterIsInstance<AiAssistantTurnStartedEvent>()).isEmpty()
    }

    @Test
    fun `when message is accepted, then turn_started is emitted with is_retry false and explicit version metadata`() =
        runTest {
            viewModel.onSendMessage("Show orders")

            val started = assistantTelemetryTracker.singleEvent<AiAssistantTurnStartedEvent>()

            assertThat(started.isRetry).isFalse()
            assertThat(started.completionStack).isEqualTo("jetpack_ai_query")
            assertThat(started.promptVersion).isEqualTo(AssistantConfig.PROMPT_VERSION)
            assertThat(started.toolCatalogVersion).isEqualTo(AssistantConfig.TOOL_CATALOG_VERSION)
        }

    @Test
    fun `when first message is accepted, then conversation_started is emitted once before turn_started`() =
        runTest {
            viewModel.onSendMessage("Show orders")

            val conversationStarted = assistantTelemetryTracker.events
                .filterIsInstance<AiAssistantConversationStartedEvent>()
            val turnStarted = assistantTelemetryTracker.events.filterIsInstance<AiAssistantTurnStartedEvent>()

            assertThat(conversationStarted).hasSize(1)
            assertThat(conversationStarted.single().context()).isEqualTo(turnStarted.single().context())
            assertThat(assistantTelemetryTracker.events.indexOf(conversationStarted.single()))
                .isLessThan(assistantTelemetryTracker.events.indexOf(turnStarted.single()))
        }

    @Test
    fun `given existing conversation, when second message is accepted, then conversation_started is not emitted again`() =
        runTest {
            viewModel.onSendMessage("First")
            runtime.emitTurnFinished()
            viewModel.onSendMessage("Second")

            assertThat(
                assistantTelemetryTracker.events.filterIsInstance<AiAssistantConversationStartedEvent>()
            ).hasSize(1)
        }

    @Test
    fun `given restarted conversation, when next message is accepted, then conversation_started is emitted for new context`() =
        runTest {
            viewModel.onSendMessage("First")
            val firstContext = runtime.startRequests.single().telemetryContext
            viewModel.onRestartConversation()

            viewModel.onSendMessage("Second")

            val conversationStarts = assistantTelemetryTracker.events
                .filterIsInstance<AiAssistantConversationStartedEvent>()
            assertThat(conversationStarts).hasSize(2)
            assertThat(conversationStarts.last().conversationId).isNotEqualTo(firstContext.conversationId)
            assertThat(conversationStarts.last().context()).isEqualTo(runtime.startRequests.last().telemetryContext)
        }

    @Test
    fun `given failed turn, when retry is accepted, then a new turn_started has a fresh request id and is_retry true`() =
        runTest {
            viewModel.onSendMessage("Show orders")
            val firstRequestId = assistantTelemetryTracker.events
                .filterIsInstance<AiAssistantTurnStartedEvent>()
                .single()
                .requestId
            runtime.emitTurnFinished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(AssistantMessage.User("Show orders")),
                retryAffordance = RetryAffordance.Manual,
                error = AssistantError.Network(),
            )

            viewModel.onRetry()

            val starts = assistantTelemetryTracker.events.filterIsInstance<AiAssistantTurnStartedEvent>()
            assertThat(starts).hasSize(2)
            assertThat(starts[1].isRetry).isTrue()
            assertThat(starts[1].requestId).isNotEqualTo(firstRequestId)
        }

    @Test
    fun `given successful loop completion, when runtime finishes, then turn_completed is emitted once with duration`() =
        runTest {
            viewModel.onSendMessage("Show orders")
            timeSource += 750.milliseconds
            runtime.emitTurnFinished(LoopOutcome.COMPLETED)

            val completed = assistantTelemetryTracker.events.filterIsInstance<AiAssistantTurnCompletedEvent>()

            assertThat(completed).hasSize(1)
            assertThat(completed.single().outcome).isEqualTo(AiAssistantTurnOutcomeValue.Success)
            assertThat(completed.single().errorKind).isNull()
            assertThat(completed.single().durationMs).isEqualTo(750L)
        }

    @Test
    fun `given failed loop, when runtime finishes, then turn_completed has outcome failed and bounded error_kind`() =
        runTest {
            viewModel.onSendMessage("Boom")
            runtime.emitTurnFinished(LoopOutcome.FAILED, error = AssistantError.Network())

            val completed = assistantTelemetryTracker.singleEvent<AiAssistantTurnCompletedEvent>()

            assertThat(completed.outcome).isEqualTo(AiAssistantTurnOutcomeValue.Failed)
            assertThat(completed.errorKind).isEqualTo(AiAssistantErrorKindValue.Network)
        }

    @Test
    fun `given transport diagnostics request id, when runtime fails, then turn_completed uses telemetry request id`() =
        runTest {
            viewModel.onSendMessage("Boom")
            val telemetryRequestId = runtime.startRequests.single().telemetryContext.requestId
            val transportRequestId = "transport-request-id-canary"

            runtime.emitTurnFinished(
                outcome = LoopOutcome.FAILED,
                error = AssistantError.BadRequest(
                    diagnostics = Diagnostics(
                        transport = TransportDiagnostics(requestId = transportRequestId)
                    )
                ),
            )

            val completed = assistantTelemetryTracker.singleEvent<AiAssistantTurnCompletedEvent>()
            assertThat(completed.requestId).isEqualTo(telemetryRequestId)
            assertThat(completed.requestId).isNotEqualTo(transportRequestId)
            assertThat(completed.errorKind).isEqualTo(AiAssistantErrorKindValue.ValidationError)
        }

    @Test
    fun `given user cancels active turn, when cancellation is requested, then turn_completed has no error_kind`() =
        runTest {
            viewModel.onSendMessage("Show orders")

            viewModel.onCancelTurn()

            val completed = assistantTelemetryTracker.singleEvent<AiAssistantTurnCompletedEvent>()
            assertThat(completed.outcome).isEqualTo(AiAssistantTurnOutcomeValue.CancelledByUser)
            assertThat(completed.errorKind).isNull()
        }

    @Test
    fun `given stopped loop with cancelled error, when runtime finishes, then turn_completed has no error_kind`() =
        runTest {
            viewModel.onSendMessage("Show orders")

            runtime.emitTurnFinished(
                outcome = LoopOutcome.STOPPED,
                error = AssistantError.Cancelled,
            )

            val completed = assistantTelemetryTracker.singleEvent<AiAssistantTurnCompletedEvent>()
            assertThat(completed.outcome).isEqualTo(AiAssistantTurnOutcomeValue.CancelledByUser)
            assertThat(completed.errorKind).isNull()
        }

    @Test
    fun `given max iterations, when runtime finishes, then outcome is max_iterations without error_kind`() = runTest {
        viewModel.onSendMessage("Show orders")

        runtime.emitTurnFinished(LoopOutcome.MAX_ITERATIONS)

        val completed = assistantTelemetryTracker.singleEvent<AiAssistantTurnCompletedEvent>()
        assertThat(completed.outcome).isEqualTo(AiAssistantTurnOutcomeValue.MaxIterations)
        assertThat(completed.errorKind).isNull()
    }

    @Test
    fun `given eligible runtime tool decision, when tool finishes, then tool_call_completed is tracked from event context`() =
        runTest {
            viewModel.onSendMessage("Show order 123")
            val context = runtime.startRequests.single().telemetryContext

            runtime.emit(
                givenToolCallFinished(
                    toolCallId = "call-1",
                    toolName = "orders_get",
                    status = AiAssistantToolStatusValue.Success,
                    durationMs = 25L,
                    emitTelemetry = true,
                    context = context,
                )
            )
            advanceUntilIdle()

            val completed = assistantTelemetryTracker.singleEvent<AiAssistantToolCallCompletedEvent>()
            assertThat(completed.context()).isEqualTo(context)
            assertThat(completed.toolName).isEqualTo("orders_get")
            assertThat(completed.status).isEqualTo(AiAssistantToolStatusValue.Success)
            assertThat(completed.errorKind).isNull()
            assertThat(completed.durationMs).isEqualTo(25L)
        }

    @Test
    fun `given replayed runtime tool decision, when tool finishes, then tool_call_completed is not tracked`() =
        runTest {
            viewModel.onSendMessage("Show order 123")

            runtime.emit(
                givenToolCallFinished(
                    toolCallId = "call-1",
                    toolName = "orders_get",
                    emitTelemetry = false,
                )
            )
            advanceUntilIdle()

            assertThat(assistantTelemetryTracker.events.filterIsInstance<AiAssistantToolCallCompletedEvent>()).isEmpty()
        }

    @Test
    fun `given stale runtime tool decision after restart, when a new turn is active, then old context is not tracked`() =
        runTest {
            viewModel.onSendMessage("First")
            val firstContext = runtime.startRequests.single().telemetryContext
            viewModel.onRestartConversation()
            viewModel.onSendMessage("Second")

            runtime.emit(
                givenToolCallFinished(
                    toolCallId = "call-1",
                    toolName = "orders_get",
                    emitTelemetry = true,
                    context = firstContext,
                )
            )
            advanceUntilIdle()

            assertThat(assistantTelemetryTracker.events.filterIsInstance<AiAssistantToolCallCompletedEvent>()).isEmpty()
        }

    @Test
    fun `given runtime show cards counts, when processed, then show_cards_processed is tracked from event context`() =
        runTest {
            viewModel.onSendMessage("Show matching orders")
            val context = runtime.startRequests.single().telemetryContext

            runtime.emit(
                AssistantRuntimeEvent.ShowCardsProcessed(
                    counts = ShowCardsCounts(
                        requestedCount = 3,
                        renderedCount = 1,
                        missingCount = 1,
                        rejectedCount = 1,
                    ),
                    telemetryContext = context,
                )
            )
            advanceUntilIdle()

            val processed = assistantTelemetryTracker.singleEvent<AiAssistantShowCardsProcessedEvent>()
            assertThat(processed.context()).isEqualTo(context)
            assertThat(processed.requestedCount).isEqualTo(3L)
            assertThat(processed.renderedCount).isEqualTo(1L)
            assertThat(processed.missingCount).isEqualTo(1L)
            assertThat(processed.rejectedCount).isEqualTo(1L)
        }

    @Test
    fun `given stale runtime show cards counts after restart, when a new turn is active, then old context is not tracked`() =
        runTest {
            viewModel.onSendMessage("First")
            val firstContext = runtime.startRequests.single().telemetryContext
            viewModel.onRestartConversation()
            viewModel.onSendMessage("Second")

            runtime.emit(
                AssistantRuntimeEvent.ShowCardsProcessed(
                    counts = ShowCardsCounts(
                        requestedCount = 1,
                        renderedCount = 0,
                        missingCount = 1,
                        rejectedCount = 0,
                    ),
                    telemetryContext = firstContext,
                )
            )
            advanceUntilIdle()

            assertThat(
                assistantTelemetryTracker.events.filterIsInstance<AiAssistantShowCardsProcessedEvent>()
            ).isEmpty()
        }

    @Test
    fun `when message is sent, then empty active assistant message shows typing indicator`() = runTest {
        viewModel.onSendMessage("Show my recent orders")

        val state = viewModel.uiState.value
        val assistantMessage = state.messages.last()
        assertThat(state.activeAssistantMessageId).isEqualTo(assistantMessage.id)
        assertThat(state.shouldShowTypingIndicator).isTrue()
    }

    @Test
    fun `given empty conversation, when message is sent, then empty state is hidden`() = runTest {
        viewModel.onSendMessage("Show revenue this week")

        assertThat(viewModel.uiState.value.shouldShowEmptyState).isFalse()
    }

    @Test
    fun `given active assistant bubble, when text delta arrives, then typing indicator is hidden`() = runTest {
        viewModel.onSendMessage("Summarize sales")
        runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Sales are up today."))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.shouldShowTypingIndicator).isFalse()
    }

    @Test
    fun `given active assistant bubble, when tool starts, then typing indicator stays visible`() = runTest {
        viewModel.onSendMessage("Find order 123")

        runtime.emit(
            AssistantRuntimeEvent.ToolCallStarted(
                toolCallId = "call-1",
                toolName = "orders_get",
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last().segments).containsExactly(
            AssistantUiSegment.Text(""),
            AssistantUiSegment.ToolActivity(
                AssistantToolActivity(
                    toolCallId = "call-1",
                    toolName = "orders_get",
                )
            ),
        )
        assertThat(viewModel.uiState.value.shouldShowTypingIndicator).isTrue()
    }

    @Test
    fun `given active assistant bubble, when show cards tool starts, then tool activity is hidden`() = runTest {
        viewModel.onSendMessage("Show matching orders")

        runtime.emit(
            AssistantRuntimeEvent.ToolCallStarted(
                toolCallId = "call-1",
                toolName = SHOW_CARDS_TOOL_NAME,
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last().segments).containsExactly(
            AssistantUiSegment.Text(""),
        )
        assertThat(viewModel.uiState.value.shouldShowTypingIndicator).isTrue()
    }

    @Test
    fun `given active tool activity, when matching tool finishes, then activity is preserved as completed`() = runTest {
        viewModel.onSendMessage("Find order 123")
        runtime.emit(AssistantRuntimeEvent.ToolCallStarted(toolCallId = "call-1", toolName = "orders_get"))
        runtime.emit(givenToolCallFinished(toolCallId = "call-1", toolName = "orders_get"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last().segments).containsExactly(
            AssistantUiSegment.Text(""),
            AssistantUiSegment.ToolActivity(
                AssistantToolActivity(
                    toolCallId = "call-1",
                    toolName = "orders_get",
                    status = AssistantToolActivity.Status.COMPLETED,
                )
            ),
        )
        assertThat(viewModel.uiState.value.shouldShowTypingIndicator).isTrue()
    }

    @Test
    fun `given active assistant bubble, when text deltas arrive, then the same bubble grows`() = runTest {
        viewModel.onSendMessage("Summarize sales")
        val activeBubbleId = viewModel.uiState.value.messages.last().id

        runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Sales are "))
        runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("up today"))
        advanceUntilIdle()

        val assistantMessages = viewModel.uiState.value.messages.filter { it.role == AssistantUiMessage.Role.ASSISTANT }
        assertThat(assistantMessages).containsExactly(
            AssistantUiMessage(
                id = activeBubbleId,
                role = AssistantUiMessage.Role.ASSISTANT,
                text = "Sales are up today",
            )
        )
    }

    @Test
    fun `when turn finishes successfully, then state returns to idle`() = runTest {
        viewModel.onSendMessage("Hello")

        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.COMPLETED,
                updatedHistory = listOf(
                    AssistantMessage.User("Hello"),
                    AssistantMessage.Assistant("Hi there"),
                ),
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.status).isEqualTo(AssistantUiStatus.IDLE)
        assertThat(state.isTurnActive).isFalse()
        assertThat(state.canRetry).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `given running tool activity, when turn completes, then unfinished activity is cleared`() = runTest {
        viewModel.onSendMessage("Find order 123")
        runtime.emit(AssistantRuntimeEvent.ToolCallStarted(toolCallId = "call-1", toolName = "orders_get"))
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.COMPLETED,
                updatedHistory = listOf(
                    AssistantMessage.User("Find order 123"),
                    AssistantMessage.Assistant("Order 123 is processing."),
                ),
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.activeAssistantMessageId).isNull()
        assertThat(state.toolActivitySegments()).isEmpty()
        assertThat(state.shouldShowTypingIndicator).isFalse()
    }

    @Test
    fun `given completed tool activity, when turn completes, then completed activity is preserved`() = runTest {
        viewModel.onSendMessage("Find order 123")
        runtime.emit(AssistantRuntimeEvent.ToolCallStarted(toolCallId = "call-1", toolName = "orders_get"))
        runtime.emit(givenToolCallFinished(toolCallId = "call-1", toolName = "orders_get"))
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.COMPLETED,
                updatedHistory = listOf(
                    AssistantMessage.User("Find order 123"),
                    AssistantMessage.Assistant("Order 123 is processing."),
                ),
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.toolActivitySegments()).containsExactly(
            AssistantUiSegment.ToolActivity(
                AssistantToolActivity(
                    toolCallId = "call-1",
                    toolName = "orders_get",
                    status = AssistantToolActivity.Status.COMPLETED,
                )
            ),
        )
    }

    @Test
    fun `given active tool activity, when turn fails, then transient activity is cleared`() = runTest {
        viewModel.onSendMessage("Find order 123")
        runtime.emit(AssistantRuntimeEvent.ToolCallStarted(toolCallId = "call-1", toolName = "orders_get"))
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(AssistantMessage.User("Find order 123")),
                retryAffordance = RetryAffordance.Manual,
                error = AssistantError.Network(),
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.activeAssistantMessageId).isNull()
        assertThat(state.toolActivitySegments()).isEmpty()
        assertThat(state.canRetry).isTrue()
    }

    @Test
    fun `when turn fails with retry available, then state exposes error and retry calls runtime`() = runTest {
        viewModel.onSendMessage("Hello")
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(AssistantMessage.User("Hello")),
                retryAffordance = RetryAffordance.Manual,
                error = AssistantError.Network(),
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.ERROR)
        assertThat(viewModel.uiState.value.error).isEqualTo(AssistantUiError.NETWORK)
        assertThat(viewModel.uiState.value.canRetry).isTrue()
        assertThat(viewModel.uiState.value.isTurnActive).isFalse()

        viewModel.onRetry()

        assertThat(runtime.retryRequests).containsExactly(
            expectedTurnRequest(requestId = "assistant-id-6", messageId = "assistant-id-5", userMessage = "Hello")
        )
    }

    @Test
    fun `given network failure with retry available, when turn fails, then active assistant message exposes retry`() =
        runTest {
            viewModel.onSendMessage("Hello")
            val activeAssistantId = viewModel.uiState.value.messages.last().id

            runtime.emit(
                givenFinished(
                    outcome = LoopOutcome.FAILED,
                    updatedHistory = listOf(AssistantMessage.User("Hello")),
                    retryAffordance = RetryAffordance.Manual,
                    error = AssistantError.Network(),
                )
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.status).isEqualTo(AssistantUiStatus.ERROR)
            assertThat(state.canRetry).isTrue()
            assertThat(state.messages.last()).isEqualTo(
                AssistantUiMessage(
                    id = activeAssistantId,
                    role = AssistantUiMessage.Role.ASSISTANT,
                    text = "",
                    error = AssistantMessageError(
                        error = AssistantError.Network(),
                        canRetry = true,
                    ),
                )
            )

            viewModel.onRetry()

            assertThat(runtime.retryRequests).containsExactly(
                expectedTurnRequest(requestId = "assistant-id-6", messageId = "assistant-id-5", userMessage = "Hello")
            )
        }

    @Test
    fun `given outcome unknown failure, when turn fails, then verify message has no retry action`() =
        runTest {
            viewModel.onSendMessage("Update order 42")
            val activeAssistantId = viewModel.uiState.value.messages.last().id

            runtime.emit(
                givenFinished(
                    outcome = LoopOutcome.FAILED,
                    updatedHistory = listOf(
                        AssistantMessage.User("Update order 42"),
                        AssistantMessage.Assistant("I'll update that order."),
                    ),
                    retryAffordance = RetryAffordance.None,
                    error = AssistantError.OutcomeUnknown(toolName = "orders_update"),
                )
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.status).isEqualTo(AssistantUiStatus.ERROR)
            assertThat(state.canRetry).isFalse()
            assertThat(state.messages.last()).isEqualTo(
                AssistantUiMessage(
                    id = activeAssistantId,
                    role = AssistantUiMessage.Role.ASSISTANT,
                    text = "",
                    error = AssistantMessageError(
                        error = AssistantError.OutcomeUnknown(toolName = "orders_update"),
                        canRetry = false,
                    ),
                )
            )

            viewModel.onRetry()

            assertThat(runtime.retryRequests).isEmpty()
        }

    @Test
    fun `given upstream failure marked retryable by runtime, when turn fails, then retry is not exposed`() =
        runTest {
            viewModel.onSendMessage("Hello")
            val activeAssistantId = viewModel.uiState.value.messages.last().id

            runtime.emit(
                givenFinished(
                    outcome = LoopOutcome.FAILED,
                    updatedHistory = listOf(AssistantMessage.User("Hello")),
                    retryAffordance = RetryAffordance.Manual,
                    error = AssistantError.UpstreamFailure(),
                )
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.status).isEqualTo(AssistantUiStatus.ERROR)
            assertThat(state.canRetry).isFalse()
            assertThat(state.messages.last()).isEqualTo(
                AssistantUiMessage(
                    id = activeAssistantId,
                    role = AssistantUiMessage.Role.ASSISTANT,
                    text = "",
                    error = AssistantMessageError(
                        error = AssistantError.UpstreamFailure(),
                        canRetry = false,
                    ),
                )
            )

            viewModel.onRetry()

            assertThat(runtime.retryRequests).isEmpty()
        }

    @Test
    fun `given unknown error with raw cause, when turn fails, then normalized error metadata drives the transcript`() =
        runTest {
            val rawCause = IllegalStateException("raw upstream token abc123")
            val normalizedError = AssistantError.Unknown(cause = rawCause)
            viewModel.onSendMessage("Hello")
            val activeAssistantId = viewModel.uiState.value.messages.last().id

            runtime.emit(
                givenFinished(
                    outcome = LoopOutcome.FAILED,
                    updatedHistory = listOf(AssistantMessage.User("Hello")),
                    retryAffordance = RetryAffordance.None,
                    error = normalizedError,
                )
            )
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.messages.last()).isEqualTo(
                AssistantUiMessage(
                    id = activeAssistantId,
                    role = AssistantUiMessage.Role.ASSISTANT,
                    text = "",
                    error = AssistantMessageError(
                        error = normalizedError,
                        canRetry = false,
                    ),
                )
            )
            assertThat(normalizedError.toMessageRes()).isEqualTo(R.string.ai_assistant_chat_error_unknown)
        }

    @Test
    fun `when turn reaches max iterations, then state exposes an error`() = runTest {
        viewModel.onSendMessage("Hello")

        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.MAX_ITERATIONS,
                updatedHistory = listOf(AssistantMessage.User("Hello")),
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.ERROR)
        assertThat(viewModel.uiState.value.error).isEqualTo(AssistantUiError.MAX_ITERATIONS)
        assertThat(viewModel.uiState.value.shouldShowFallbackError).isTrue()
        assertThat(requireNotNull(viewModel.uiState.value.error).toMessageRes())
            .isEqualTo(R.string.ai_assistant_chat_error_max_iterations)
        assertThat(viewModel.uiState.value.messages.last().error).isNull()
        assertThat(viewModel.uiState.value.canRetry).isFalse()
        assertThat(viewModel.uiState.value.isTurnActive).isFalse()
    }

    @Test
    fun `given prior history, when failed turn is retried, then retry uses pre-turn history`() = runTest {
        val priorHistory = listOf(
            AssistantMessage.User("Previous question"),
            AssistantMessage.Assistant("Previous answer"),
        )
        viewModel.onSendMessage("Previous question")
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.COMPLETED,
                updatedHistory = priorHistory,
            )
        )
        advanceUntilIdle()

        viewModel.onSendMessage("Current question")
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = priorHistory + listOf(
                    AssistantMessage.User("Current question"),
                    AssistantMessage.Assistant("Partial answer"),
                ),
                retryAffordance = RetryAffordance.Manual,
                error = AssistantError.Network(),
            )
        )
        advanceUntilIdle()

        viewModel.onRetry()

        assertThat(runtime.retryRequests.last()).isEqualTo(
            expectedTurnRequest(
                requestId = "assistant-id-9",
                messageId = "assistant-id-8",
                userMessage = "Current question",
                history = priorHistory,
            )
        )
    }

    @Test
    fun `given completed conversation, when restarted, then state is cleared and next turn uses empty history`() =
        runTest {
            val completedHistory = listOf(
                AssistantMessage.User("Previous question"),
                AssistantMessage.Assistant("Previous answer"),
            )
            viewModel.onSendMessage("Previous question")
            runtime.emit(
                givenFinished(
                    outcome = LoopOutcome.COMPLETED,
                    updatedHistory = completedHistory,
                )
            )
            advanceUntilIdle()

            viewModel.onRestartConversation()
            viewModel.onSendMessage("New question")

            assertThat(viewModel.uiState.value.messages).containsExactly(
                AssistantUiMessage(id = "assistant-id-6", role = AssistantUiMessage.Role.USER, text = "New question"),
                AssistantUiMessage(id = "assistant-id-7", role = AssistantUiMessage.Role.ASSISTANT, text = ""),
            )
            assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.STREAMING)
            assertThat(viewModel.uiState.value.error).isNull()
            assertThat(viewModel.uiState.value.canRetry).isFalse()
            assertThat(runtime.startRequests.last()).isEqualTo(
                expectedTurnRequest(
                    conversationId = "assistant-id-5",
                    requestId = "assistant-id-8",
                    messageId = "assistant-id-7",
                    userMessage = "New question",
                )
            )
        }

    @Test
    fun `given active conversation, when conversation restarts, then empty state is visible`() = runTest {
        viewModel.onSendMessage("Previous question")

        viewModel.onRestartConversation()

        assertThat(viewModel.uiState.value.messages).isEmpty()
        assertThat(viewModel.uiState.value.shouldShowEmptyState).isTrue()
    }

    @Test
    fun `given active turn, when restarted, then state is cleared and runtime turn is cancelled`() = runTest {
        viewModel.onSendMessage("Hello")
        runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Partial"))
        advanceUntilIdle()

        viewModel.onRestartConversation()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(AssistantUiState())
        assertThat(runtime.cancelledConversationIds).containsExactly(CONVERSATION_ID)
    }

    @Test
    fun `given retry fails, when retried again, then retry still uses original pre-turn history`() = runTest {
        val priorHistory = listOf(
            AssistantMessage.User("Previous question"),
            AssistantMessage.Assistant("Previous answer"),
        )
        viewModel.onSendMessage("Previous question")
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.COMPLETED,
                updatedHistory = priorHistory,
            )
        )
        advanceUntilIdle()
        viewModel.onSendMessage("Current question")
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = priorHistory + listOf(
                    AssistantMessage.User("Current question"),
                    AssistantMessage.Assistant("First failure"),
                ),
                retryAffordance = RetryAffordance.Manual,
                error = AssistantError.Network(),
            )
        )
        advanceUntilIdle()

        viewModel.onRetry()
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = priorHistory + listOf(
                    AssistantMessage.User("Current question"),
                    AssistantMessage.Assistant("Second failure"),
                ),
                retryAffordance = RetryAffordance.Manual,
                error = AssistantError.Network(),
            )
        )
        advanceUntilIdle()
        viewModel.onRetry()

        assertThat(runtime.retryRequests).hasSize(2)
        assertThat(runtime.retryRequests.map { it.sessionHistory }).containsOnly(sessionHistoryFrom(priorHistory))
    }

    @Test
    fun `given retryable failed turn, when a new turn starts, then previous retry action is disabled`() = runTest {
        viewModel.onSendMessage("First")
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(AssistantMessage.User("First")),
                retryAffordance = RetryAffordance.Manual,
                error = AssistantError.Network(),
            )
        )
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.messages.last().error?.canRetry).isTrue()

        viewModel.onSendMessage("Second")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.first { it.text.isEmpty() }.error?.canRetry).isFalse()
        assertThat(viewModel.uiState.value.canRetry).isFalse()
        assertThat(runtime.retryRequests).isEmpty()
    }

    @Test
    fun `given two retryable failed turns, when retry is requested, then latest failed turn is retried`() = runTest {
        viewModel.onSendMessage("First")
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(AssistantMessage.User("First")),
                retryAffordance = RetryAffordance.Manual,
                error = AssistantError.Network(),
            )
        )
        advanceUntilIdle()

        viewModel.onSendMessage("Second")
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(
                    AssistantMessage.User("First"),
                    AssistantMessage.User("Second"),
                ),
                retryAffordance = RetryAffordance.Manual,
                error = AssistantError.Timeout(),
            )
        )
        advanceUntilIdle()

        val messageErrors = viewModel.uiState.value.messages.mapNotNull { it.error }
        assertThat(messageErrors.map { it.canRetry }).containsExactly(false, true)

        viewModel.onRetry()

        assertThat(runtime.retryRequests).containsExactly(
            expectedTurnRequest(
                requestId = "assistant-id-9",
                messageId = "assistant-id-8",
                userMessage = "Second",
                history = listOf(AssistantMessage.User("First")),
            )
        )
    }

    @Test
    fun `when runtime awaits confirmation, then assistant message gains inline confirmation segment`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        val confirmation = AssistantConfirmationCard(
            confirmationId = "confirmation-1",
            toolCall = ToolCall(
                id = "call-1",
                name = "orders_update",
                arguments = buildJsonObject { put("id", 123) },
            ),
            state = AssistantConfirmationCardState.PENDING,
        )

        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(confirmation))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.AWAITING_CONFIRMATION)
        assertThat(viewModel.uiState.value.activeConfirmationId).isEqualTo("confirmation-1")
        assertThat(viewModel.uiState.value.messages.last().segments).containsExactly(
            AssistantUiSegment.Text(""),
            AssistantUiSegment.ConfirmationCard(confirmation),
        )
    }

    @Test
    fun `given active assistant bubble, when cards arrive, then grouped card segment is appended`() = runTest {
        viewModel.onSendMessage("Show order 123")
        val activeBubbleId = viewModel.uiState.value.messages.last().id
        val orderCard = givenOrderCard(id = "123", number = "#123")

        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(orderCard)))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last()).isEqualTo(
            AssistantUiMessage(
                id = activeBubbleId,
                role = AssistantUiMessage.Role.ASSISTANT,
                segments = listOf(
                    AssistantUiSegment.Text(""),
                    AssistantUiSegment.CardGroup(listOf(orderCard)),
                ),
            )
        )
    }

    @Test
    fun `given show cards tool activity is hidden, when cards resolve, then card group is still appended`() = runTest {
        viewModel.onSendMessage("Show order 123")
        val orderCard = givenOrderCard(id = "123", number = "#123")

        runtime.emit(AssistantRuntimeEvent.ToolCallStarted(toolCallId = "call-1", toolName = SHOW_CARDS_TOOL_NAME))
        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(orderCard)))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last().segments).containsExactly(
            AssistantUiSegment.Text(""),
            AssistantUiSegment.CardGroup(listOf(orderCard)),
        )
    }

    @Test
    fun `given cards arrive between text deltas, when turn finishes, then cards stay on active assistant message`() =
        runTest {
            viewModel.onSendMessage("Show order 123")
            val activeBubbleId = viewModel.uiState.value.messages.last().id
            val orderCard = givenOrderCard(id = "123", number = "#123")

            runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Here is the order."))
            runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(orderCard)))
            runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Anything else?"))
            runtime.emit(
                givenFinished(
                    outcome = LoopOutcome.COMPLETED,
                    updatedHistory = listOf(
                        AssistantMessage.User("Show order 123"),
                        AssistantMessage.Assistant("Here is the order. Anything else?"),
                    ),
                )
            )
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.messages.last()).isEqualTo(
                AssistantUiMessage(
                    id = activeBubbleId,
                    role = AssistantUiMessage.Role.ASSISTANT,
                    segments = listOf(
                        AssistantUiSegment.Text("Here is the order."),
                        AssistantUiSegment.CardGroup(listOf(orderCard)),
                        AssistantUiSegment.Text("Anything else?"),
                    ),
                )
            )
            assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.IDLE)
        }

    @Test
    fun `given no card event is emitted, when turn finishes, then no card segment is present`() = runTest {
        viewModel.onSendMessage("Show missing order")

        runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("I could not find that order."))
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.COMPLETED,
                updatedHistory = listOf(
                    AssistantMessage.User("Show missing order"),
                    AssistantMessage.Assistant("I could not find that order."),
                ),
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last().segments.filterIsInstance<AssistantUiSegment.CardGroup>())
            .isEmpty()
    }

    @Test
    fun `given duplicate card keys across one turn, when cards arrive, then first seen cards are kept`() = runTest {
        viewModel.onSendMessage("Show matching cards")
        val firstOrder = givenOrderCard(id = "123", number = "#123")
        val duplicateOrder = givenOrderCard(id = "123", number = "#duplicate")
        val secondOrder = givenOrderCard(id = "456", number = "#456")

        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(firstOrder)))
        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(duplicateOrder, secondOrder)))
        advanceUntilIdle()

        val cardGroups = viewModel.uiState.value.messages.last().segments
            .filterIsInstance<AssistantUiSegment.CardGroup>()

        assertThat(cardGroups).containsExactly(
            AssistantUiSegment.CardGroup(listOf(firstOrder, secondOrder)),
        )
    }

    @Test
    fun `given grouped variation cards have distinct parent product ids, when cards arrive, then both remain visible`() =
        runTest {
            viewModel.onSendMessage("Show matching variations")
            val firstVariation = givenVariationCard(parentProductId = 100L, variationId = 10L, name = "Blue socks")
            val secondVariation = givenVariationCard(parentProductId = 101L, variationId = 10L, name = "Green socks")

            runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(firstVariation, secondVariation)))
            advanceUntilIdle()

            val cardGroups = viewModel.uiState.value.messages.last().segments
                .filterIsInstance<AssistantUiSegment.CardGroup>()

            assertThat(cardGroups).containsExactly(
                AssistantUiSegment.CardGroup(listOf(firstVariation, secondVariation)),
            )
        }

    @Test
    fun `given grouped variation cards have same composite id, when cards arrive, then duplicate is filtered`() =
        runTest {
            viewModel.onSendMessage("Show matching variations")
            val firstVariation = givenVariationCard(parentProductId = 100L, variationId = 10L, name = "Blue socks")
            val duplicateVariation = givenVariationCard(parentProductId = 100L, variationId = 10L, name = "Red socks")
            val secondVariation = givenVariationCard(parentProductId = 100L, variationId = 11L, name = "Green socks")

            runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(firstVariation)))
            runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(duplicateVariation, secondVariation)))
            advanceUntilIdle()

            val cardGroups = viewModel.uiState.value.messages.last().segments
                .filterIsInstance<AssistantUiSegment.CardGroup>()

            assertThat(cardGroups).containsExactly(
                AssistantUiSegment.CardGroup(listOf(firstVariation, secondVariation)),
            )
        }

    @Test
    fun `given repeated show cards calls, when cards arrive, then batches merge`() = runTest {
        viewModel.onSendMessage("Show matching cards")
        val firstOrder = givenOrderCard(id = "123", number = "#123")
        val secondOrder = givenOrderCard(id = "456", number = "#456")

        runtime.emit(AssistantRuntimeEvent.ToolCallStarted(toolCallId = "call-1", toolName = SHOW_CARDS_TOOL_NAME))
        runtime.emit(givenToolCallFinished(toolCallId = "call-1", toolName = SHOW_CARDS_TOOL_NAME))
        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(firstOrder)))
        runtime.emit(AssistantRuntimeEvent.ToolCallStarted(toolCallId = "call-2", toolName = SHOW_CARDS_TOOL_NAME))
        runtime.emit(givenToolCallFinished(toolCallId = "call-2", toolName = SHOW_CARDS_TOOL_NAME))
        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(secondOrder)))
        advanceUntilIdle()

        val cardGroups = viewModel.uiState.value.messages.last().segments
            .filterIsInstance<AssistantUiSegment.CardGroup>()

        assertThat(cardGroups).containsExactly(
            AssistantUiSegment.CardGroup(listOf(firstOrder, secondOrder)),
        )
    }

    @Test
    fun `given same id across different families, when cards arrive, then both cards are kept`() = runTest {
        viewModel.onSendMessage("Show order and product 123")
        val order = givenOrderCard(id = "123", number = "#123")
        val product = givenProductCard(id = "123", name = "Socks")

        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(order, product)))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last().segments)
            .contains(
                AssistantUiSegment.CardGroup(listOf(order, product)),
            )
    }

    @Test
    fun `given duplicate stats card date ranges across one turn, when cards arrive, then first seen card is kept`() =
        runTest {
            viewModel.onSendMessage("Show sales")
            val firstStats = givenStatsCard(after = "2026-05-01", before = "2026-05-07", totalSales = "123.45")
            val duplicateStats = givenStatsCard(after = "2026-05-01", before = "2026-05-07", totalSales = "999.99")
            val secondStats = givenStatsCard(after = "2026-05-08", before = "2026-05-14", totalSales = "456.78")

            runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(firstStats)))
            runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(duplicateStats, secondStats)))
            advanceUntilIdle()

            val cardGroups = viewModel.uiState.value.messages.last().segments
                .filterIsInstance<AssistantUiSegment.CardGroup>()

            assertThat(cardGroups).containsExactly(
                AssistantUiSegment.CardGroup(listOf(firstStats, secondStats)),
            )
        }

    @Test
    fun `given finished history contains card shaped tool json, when reduced, then no card segment is created`() =
        runTest {
            viewModel.onSendMessage("Show analytics")

            runtime.emit(
                givenFinished(
                    outcome = LoopOutcome.COMPLETED,
                    updatedHistory = listOf(
                        AssistantMessage.User("Show analytics"),
                        AssistantMessage.Tool(
                            toolCallId = "call-analytics",
                            content = """{"cards":[{"family":"order","id":"123"}]}""",
                        ),
                        AssistantMessage.Assistant("Revenue is up today."),
                    ),
                )
            )
            advanceUntilIdle()

            val cardGroups = viewModel.uiState.value.messages.last().segments
                .filterIsInstance<AssistantUiSegment.CardGroup>()

            assertThat(cardGroups).isEmpty()
        }

    @Test
    fun `given rendered card, when card is tapped, then card_tapped is tracked before navigation is emitted`() =
        runTest {
            val navigations = mutableListOf<AssistantCardAction>()
            val navigationJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.pendingCardNavigation.toList(navigations)
            }
            viewModel.onSendMessage("Show order 123")
            val sourceContext = runtime.startRequests.single().telemetryContext
            val orderCard = givenOrderCard(id = "123", number = "#123")
            val action = AssistantCardAction.OpenOrder(123L)

            viewModel.onCardTapped(orderCard, action, sourceContext.messageId)
            advanceUntilIdle()

            val tapped = assistantTelemetryTracker.singleEvent<AiAssistantCardTappedEvent>()
            assertThat(tapped.context()).isEqualTo(sourceContext)
            assertThat(navigations).containsExactly(action)
            navigationJob.cancel()
        }

    @Test
    fun `given first turn card and second active turn, when first card is tapped, then source context is tracked`() =
        runTest {
            viewModel.onSendMessage("Show order 123")
            val firstContext = runtime.startRequests.single().telemetryContext
            val orderCard = givenOrderCard(id = "123", number = "#123")
            runtime.emitTurnFinished()
            viewModel.onSendMessage("Second turn")

            viewModel.onCardTapped(orderCard, AssistantCardAction.OpenOrder(123L), firstContext.messageId)
            advanceUntilIdle()

            val tapped = assistantTelemetryTracker.singleEvent<AiAssistantCardTappedEvent>()
            assertThat(tapped.context()).isEqualTo(firstContext)
        }

    @Test
    fun `given card from cleared conversation, when card is tapped after restart, then telemetry and navigation are suppressed`() =
        runTest {
            val navigations = mutableListOf<AssistantCardAction>()
            val navigationJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.pendingCardNavigation.toList(navigations)
            }
            viewModel.onSendMessage("Show order 123")
            val firstContext = runtime.startRequests.single().telemetryContext
            viewModel.onRestartConversation()

            viewModel.onCardTapped(
                card = givenOrderCard(id = "123", number = "#123"),
                action = AssistantCardAction.OpenOrder(123L),
                sourceMessageId = firstContext.messageId,
            )
            advanceUntilIdle()

            assertThat(assistantTelemetryTracker.events.filterIsInstance<AiAssistantCardTappedEvent>()).isEmpty()
            assertThat(navigations).isEmpty()
            navigationJob.cancel()
        }

    @Test
    fun `when cancel is requested, then runtime is cancelled and turn is no longer active`() = runTest {
        viewModel.onSendMessage("Hello")

        viewModel.onCancelTurn()
        advanceUntilIdle()

        assertThat(runtime.cancelledConversationIds).containsExactly(CONVERSATION_ID)
        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.ERROR)
        assertThat(viewModel.uiState.value.error).isEqualTo(AssistantUiError.CANCELLED)
        assertThat(viewModel.uiState.value.shouldShowFallbackError).isTrue()
        assertThat(requireNotNull(viewModel.uiState.value.error).toMessageRes())
            .isEqualTo(R.string.ai_assistant_chat_error_cancelled)
        assertThat(viewModel.uiState.value.messages.last().error).isNull()
        assertThat(viewModel.uiState.value.canRetry).isFalse()
        assertThat(viewModel.uiState.value.activeConfirmationId).isNull()
        assertThat(viewModel.uiState.value.isTurnActive).isFalse()
    }

    @Test
    fun `when runtime finishes with cancelled error, then state exposes cancelled ui error`() = runTest {
        viewModel.onSendMessage("Hello")

        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.STOPPED,
                updatedHistory = listOf(
                    AssistantMessage.User("Hello"),
                    AssistantMessage.Assistant("Partial"),
                ),
                retryAffordance = RetryAffordance.None,
                error = AssistantError.Cancelled,
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.ERROR)
        assertThat(viewModel.uiState.value.error).isEqualTo(AssistantUiError.CANCELLED)
        assertThat(viewModel.uiState.value.canRetry).isFalse()
        assertThat(viewModel.uiState.value.isTurnActive).isFalse()
    }

    @Test
    fun `when runtime finishes cancelled failed retryable, then retry is not exposed`() = runTest {
        viewModel.onSendMessage("Hello")

        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(
                    AssistantMessage.User("Hello"),
                    AssistantMessage.Assistant("Partial"),
                ),
                retryAffordance = RetryAffordance.Manual,
                error = AssistantError.Cancelled,
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.ERROR)
        assertThat(viewModel.uiState.value.error).isEqualTo(AssistantUiError.CANCELLED)
        assertThat(viewModel.uiState.value.canRetry).isFalse()
        assertThat(viewModel.uiState.value.isTurnActive).isFalse()
    }

    @Test
    fun `given active tool activity, when cancel is requested, then transient activity is cleared`() = runTest {
        viewModel.onSendMessage("Find order 123")
        runtime.emit(AssistantRuntimeEvent.ToolCallStarted(toolCallId = "call-1", toolName = "orders_get"))
        advanceUntilIdle()

        viewModel.onCancelTurn()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.activeAssistantMessageId).isNull()
        assertThat(state.toolActivitySegments()).isEmpty()
        assertThat(state.error).isEqualTo(AssistantUiError.CANCELLED)
    }

    @Test
    fun `given partial assistant text, when cancel is requested, then partial text remains visible`() = runTest {
        viewModel.onSendMessage("Summarize sales")
        val activeBubbleId = viewModel.uiState.value.messages.last().id
        runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Sales are "))
        runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("up today"))
        advanceUntilIdle()

        viewModel.onCancelTurn()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages).contains(
            AssistantUiMessage(
                id = activeBubbleId,
                role = AssistantUiMessage.Role.ASSISTANT,
                text = "Sales are up today",
            )
        )
    }

    @Test
    fun `given partial assistant text, when cancelled and new message is sent, then next turn history includes partial text`() =
        runTest {
            viewModel.onSendMessage("Summarize sales")
            runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Sales are up today"))
            advanceUntilIdle()

            viewModel.onCancelTurn()
            advanceUntilIdle()
            viewModel.onSendMessage("What changed?")

            assertThat(runtime.startRequests.last()).isEqualTo(
                expectedTurnRequest(
                    requestId = "assistant-id-7",
                    messageId = "assistant-id-6",
                    userMessage = "What changed?",
                    history = listOf(
                        AssistantMessage.User("Summarize sales"),
                        AssistantMessage.Assistant("Sales are up today"),
                    ),
                )
            )
        }

    @Test
    fun `given no assistant text, when cancelled and new message is sent, then next turn history includes only cancelled user`() =
        runTest {
            viewModel.onSendMessage("Summarize sales")
            advanceUntilIdle()

            viewModel.onCancelTurn()
            advanceUntilIdle()
            viewModel.onSendMessage("What changed?")

            assertThat(runtime.startRequests.last()).isEqualTo(
                expectedTurnRequest(
                    requestId = "assistant-id-7",
                    messageId = "assistant-id-6",
                    userMessage = "What changed?",
                    history = listOf(
                        AssistantMessage.User("Summarize sales"),
                    ),
                )
            )
        }

    @Test
    fun `given failed turn with tool activity, when retry starts, then old transient activity is not replayed`() =
        runTest {
            viewModel.onSendMessage("Find order 123")
            runtime.emit(AssistantRuntimeEvent.ToolCallStarted(toolCallId = "call-1", toolName = "orders_get"))
            runtime.emit(
                givenFinished(
                    outcome = LoopOutcome.FAILED,
                    updatedHistory = listOf(AssistantMessage.User("Find order 123")),
                    retryAffordance = RetryAffordance.Manual,
                    error = AssistantError.Network(),
                )
            )
            advanceUntilIdle()

            viewModel.onRetry()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.messages.dropLast(1).toolActivitySegments()).isEmpty()
            assertThat(state.shouldShowTypingIndicator).isTrue()
        }

    @Test
    fun `given active streaming turn, when another message is sent, then second turn is ignored`() = runTest {
        viewModel.onSendMessage("First")

        viewModel.onSendMessage("Second")
        advanceUntilIdle()

        assertThat(runtime.startRequests).containsExactly(
            expectedTurnRequest(requestId = "assistant-id-4", messageId = "assistant-id-3", userMessage = "First")
        )
        assertThat(viewModel.uiState.value.messages.map { it.text }).containsExactly("First", "")
    }

    @Test
    fun `given awaiting confirmation, when another message is sent, then second turn is ignored`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenConfirmationCard()))
        advanceUntilIdle()

        viewModel.onSendMessage("Second")
        advanceUntilIdle()

        assertThat(runtime.startRequests).containsExactly(
            expectedTurnRequest(
                requestId = "assistant-id-4",
                messageId = "assistant-id-3",
                userMessage = "Cancel order 123",
            )
        )
        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.AWAITING_CONFIRMATION)
        assertThat(viewModel.uiState.value.activeConfirmationId).isEqualTo("confirmation-1")
    }

    @Test
    fun `given active streaming turn, when retry is requested, then retry is ignored`() = runTest {
        viewModel.onSendMessage("Hello")

        viewModel.onRetry()
        advanceUntilIdle()

        assertThat(runtime.retryRequests).isEmpty()
        assertThat(runtime.startRequests).hasSize(1)
    }

    @Test
    fun `given pending confirmation, when confirm write is requested, then runtime confirm is called`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenConfirmationCard()))
        advanceUntilIdle()

        viewModel.onConfirmWrite()
        advanceUntilIdle()

        assertThat(runtime.results).containsExactly(
            ConfirmationResult("confirmation-1", ConfirmationDecision.CONFIRMED)
        )
        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.STREAMING)
        assertThat(viewModel.uiState.value.error).isNull()
        assertThat(viewModel.uiState.value.activeConfirmationId).isNull()
    }

    @Test
    fun `given confirmed write, when assistant text resumes, then text is appended after confirmation card`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        val activeBubbleId = viewModel.uiState.value.messages.last().id
        runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("I'll update that order."))
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenConfirmationCard()))
        advanceUntilIdle()

        viewModel.onConfirmWrite()
        runtime.emit(
            AssistantRuntimeEvent.ConfirmationResolved(
                ConfirmationResult("confirmation-1", ConfirmationDecision.CONFIRMED)
            )
        )
        runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Order updated"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last()).isEqualTo(
            AssistantUiMessage(
                id = activeBubbleId,
                role = AssistantUiMessage.Role.ASSISTANT,
                segments = listOf(
                    AssistantUiSegment.Text("I'll update that order."),
                    AssistantUiSegment.ConfirmationCard(
                        givenConfirmationCard().copy(state = AssistantConfirmationCardState.CONFIRMED)
                    ),
                    AssistantUiSegment.Text("Order updated"),
                ),
            )
        )
    }

    @Test
    fun `given confirmed confirmation resolves, when turn later finishes, then confirmed card stays in transcript`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenConfirmationCard()))
        advanceUntilIdle()

        viewModel.onConfirmWrite()
        runtime.emit(
            AssistantRuntimeEvent.ConfirmationResolved(
                ConfirmationResult("confirmation-1", ConfirmationDecision.CONFIRMED)
            )
        )
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.COMPLETED,
                updatedHistory = listOf(
                    AssistantMessage.User("Cancel order 123"),
                    AssistantMessage.Assistant("Done"),
                ),
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last().segments).contains(
            AssistantUiSegment.ConfirmationCard(
                givenConfirmationCard().copy(state = AssistantConfirmationCardState.CONFIRMED)
            )
        )
    }

    @Test
    fun `given cancelled confirmation resolves, when turn stops, then cancelled card stays in transcript`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenConfirmationCard()))
        advanceUntilIdle()

        runtime.emit(
            AssistantRuntimeEvent.ConfirmationResolved(
                ConfirmationResult("confirmation-1", ConfirmationDecision.CANCELLED)
            )
        )
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.STOPPED,
                updatedHistory = listOf(
                    AssistantMessage.User("Cancel order 123"),
                ),
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.IDLE)
        assertThat(viewModel.uiState.value.error).isNull()
        assertThat(viewModel.uiState.value.shouldShowFallbackError).isFalse()
        assertThat(viewModel.uiState.value.messages.last().segments).contains(
            AssistantUiSegment.ConfirmationCard(
                givenConfirmationCard().copy(state = AssistantConfirmationCardState.CANCELLED)
            )
        )
    }

    @Test
    fun `given pending confirmation, when confirm is deferred, then state exposes error`() = runTest {
        runtime.confirmationResult = AssistantRuntimeConfirmationDispatchResult.Deferred
        viewModel.onSendMessage("Cancel order 123")
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenConfirmationCard()))
        advanceUntilIdle()

        viewModel.onConfirmWrite()
        advanceUntilIdle()

        assertThat(runtime.results).containsExactly(
            ConfirmationResult("confirmation-1", ConfirmationDecision.CONFIRMED)
        )
        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.ERROR)
        assertThat(viewModel.uiState.value.error).isEqualTo(AssistantUiError.CONFIRMATION_DEFERRED)
        assertThat(viewModel.uiState.value.shouldShowFallbackError).isTrue()
        assertThat(requireNotNull(viewModel.uiState.value.error).toMessageRes())
            .isEqualTo(R.string.ai_assistant_chat_error_confirmation_deferred)
        assertThat(viewModel.uiState.value.messages.last().error).isNull()
        assertThat(viewModel.uiState.value.activeConfirmationId).isNull()
    }

    @Test
    fun `given pending confirmation, when cancel write is requested, then runtime cancel is dispatched without ending turn`() =
        runTest {
            viewModel.onSendMessage("Cancel order 123")
            runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenConfirmationCard()))
            advanceUntilIdle()

            viewModel.onCancelWrite()
            advanceUntilIdle()

            assertThat(runtime.results).containsExactly(
                ConfirmationResult("confirmation-1", ConfirmationDecision.CANCELLED)
            )
            assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.AWAITING_CONFIRMATION)
            assertThat(viewModel.uiState.value.activeConfirmationId).isNull()
            assertThat(viewModel.uiState.value.error).isNull()
            assertThat(viewModel.uiState.value.isTurnActive).isTrue()
        }

    @Test
    fun `given pending confirmation, when conversation cancel is requested, then confirmation is cancelled and transcript stays active until finish`() =
        runTest {
            viewModel.onSendMessage("Cancel order 123")
            runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenConfirmationCard()))
            advanceUntilIdle()

            viewModel.onCancelTurn()
            advanceUntilIdle()

            assertThat(runtime.results).containsExactly(
                ConfirmationResult("confirmation-1", ConfirmationDecision.CANCELLED)
            )
            assertThat(runtime.cancelledConversationIds).isEmpty()
            assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.AWAITING_CONFIRMATION)
            assertThat(viewModel.uiState.value.activeConfirmationId).isNull()
            assertThat(viewModel.uiState.value.error).isNull()

            runtime.emit(
                AssistantRuntimeEvent.ConfirmationResolved(
                    ConfirmationResult("confirmation-1", ConfirmationDecision.CANCELLED)
                )
            )
            runtime.emit(
                givenFinished(
                    outcome = LoopOutcome.STOPPED,
                    updatedHistory = listOf(
                        AssistantMessage.User("Cancel order 123"),
                    ),
                )
            )
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.IDLE)
            assertThat(viewModel.uiState.value.error).isNull()
            assertThat(viewModel.uiState.value.messages.last().segments).contains(
                AssistantUiSegment.ConfirmationCard(
                    givenConfirmationCard().copy(state = AssistantConfirmationCardState.CANCELLED)
                )
            )
        }

    @Test
    fun `given cancelled confirmation is resolved, when next turn starts, then transcript card stays closed`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenConfirmationCard()))
        advanceUntilIdle()

        viewModel.onCancelTurn()
        runtime.emit(
            AssistantRuntimeEvent.ConfirmationResolved(
                ConfirmationResult("confirmation-1", ConfirmationDecision.CANCELLED)
            )
        )
        runtime.emit(
            givenFinished(
                outcome = LoopOutcome.STOPPED,
                updatedHistory = listOf(AssistantMessage.User("Cancel order 123")),
            )
        )
        advanceUntilIdle()
        val resolvedMessageId = viewModel.uiState.value.messages.last().id

        viewModel.onSendMessage("Show order 123")
        advanceUntilIdle()

        val resolvedMessage = viewModel.uiState.value.messages.first { it.id == resolvedMessageId }
        assertThat(resolvedMessage.segments).contains(
            AssistantUiSegment.ConfirmationCard(
                givenConfirmationCard().copy(state = AssistantConfirmationCardState.CANCELLED)
            )
        )
        assertThat(viewModel.uiState.value.activeConfirmationId).isNull()
        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.STREAMING)
    }

    private fun givenConfirmationCard() = AssistantConfirmationCard(
        confirmationId = "confirmation-1",
        toolCall = ToolCall(
            id = "call-1",
            name = "orders_update",
            arguments = buildJsonObject { put("id", 123) },
        ),
        state = AssistantConfirmationCardState.PENDING,
    )

    private fun givenOrderCard(id: String, number: String) = AssistantCard.Order(
        remoteOrderId = id.toLong(),
        number = number,
        status = "processing",
        total = "12.34",
        currency = "USD",
        customerName = "Jane Doe",
        date = "2026-05-01T10:00:00Z",
    )

    private fun givenProductCard(id: String, name: String) = AssistantCard.Product(
        remoteProductId = id.toLong(),
        name = name,
        sku = "woo-socks",
        price = "9.99",
        stockStatus = "instock",
        status = "publish",
        imageUrl = "https://example.com/socks.png",
    )

    private fun givenVariationCard(
        parentProductId: Long,
        variationId: Long,
        name: String = "Blue socks",
    ) = AssistantCard.Variation(
        parentProductId = parentProductId,
        variationId = variationId,
        name = name,
        sku = "woo-socks-blue",
        price = "12.99",
        stockStatus = "instock",
        status = "publish",
        imageUrl = "https://example.com/blue-socks.png",
        attributes = listOf(AssistantCard.Variation.Attribute(name = "Size", option = "M")),
    )

    private fun givenStatsCard(
        after: String,
        before: String,
        totalSales: String,
    ) = AssistantCard.Stats(
        id = "analytics_orders:after:$after:before:$before:interval:day",
        after = after,
        before = before,
        currency = "USD",
        metrics = listOf(
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.TotalSales,
                value = totalSales,
                chartPoints = listOf(AssistantCard.Stats.ChartPoint("2026-05-01", 12.0)),
            ),
            AssistantCard.Stats.Metric(
                type = AssistantCard.Stats.MetricType.NetSales,
                value = "100.15",
                chartPoints = listOf(AssistantCard.Stats.ChartPoint("2026-05-01", 10.0)),
            ),
        ),
    )

    private inner class FakeAssistantRuntime : AssistantRuntime {
        val startRequests = mutableListOf<AssistantTurnRequest>()
        val retryRequests = mutableListOf<AssistantTurnRequest>()
        val cancelledConversationIds = mutableListOf<String>()
        val results = mutableListOf<ConfirmationResult>()
        var confirmationResult: AssistantRuntimeConfirmationDispatchResult =
            AssistantRuntimeConfirmationDispatchResult.Accepted

        private val events = MutableSharedFlow<AssistantRuntimeEvent>(extraBufferCapacity = 10)

        override fun startTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> {
            startRequests += request
            return events
        }

        override fun retryTurn(request: AssistantTurnRequest): Flow<AssistantRuntimeEvent> {
            retryRequests += request
            return events
        }

        override suspend fun cancelTurn(conversationId: String) {
            cancelledConversationIds += conversationId
        }

        override suspend fun resolveConfirmation(
            result: ConfirmationResult,
        ): AssistantRuntimeConfirmationDispatchResult {
            results += result
            return confirmationResult
        }

        suspend fun emit(event: AssistantRuntimeEvent) {
            events.emit(event)
        }

        suspend fun emitTurnFinished(
            outcome: LoopOutcome = LoopOutcome.COMPLETED,
            updatedHistory: List<AssistantMessage> = emptyList(),
            retryAffordance: RetryAffordance = RetryAffordance.None,
            error: AssistantError? = null,
        ) {
            emit(
                givenFinished(
                    outcome = outcome,
                    updatedSessionHistory = sessionHistoryFrom(updatedHistory),
                    retryAffordance = retryAffordance,
                    error = error,
                )
            )
        }
    }

    private fun givenFinished(
        outcome: LoopOutcome,
        updatedHistory: List<AssistantMessage> = emptyList(),
        updatedSessionHistory: AssistantSessionHistory? = null,
        retryAffordance: RetryAffordance = RetryAffordance.None,
        error: AssistantError? = null,
    ) = AssistantRuntimeEvent.Finished(
        outcome = outcome,
        updatedSessionHistory = updatedSessionHistory ?: sessionHistoryFrom(updatedHistory),
        retryAffordance = retryAffordance,
        error = error,
    )

    private fun sequentialAssistantIdGenerator(): AssistantIdGenerator {
        var count = 0
        return mock {
            on { nextId() } doAnswer {
                count += 1
                "assistant-id-$count"
            }
        }
    }

    private fun expectedTurnRequest(
        conversationId: String = CONVERSATION_ID,
        requestId: String,
        messageId: String,
        userMessage: String,
        history: List<AssistantMessage> = emptyList(),
    ) = AssistantTurnRequest(
        conversationId = conversationId,
        telemetryContext = AssistantTelemetryContext(
            conversationId = conversationId,
            requestId = requestId,
            messageId = messageId,
        ),
        siteId = SITE_ID,
        toolScope = ToolScope.GLOBAL,
        userMessage = userMessage,
        sessionHistory = sessionHistoryFrom(history),
    )

    private fun sessionHistoryFrom(history: List<AssistantMessage>) = AssistantSessionHistory(
        messages = history.mapNotNull { message ->
            when (message) {
                is AssistantMessage.User -> AssistantSessionMessage.User(message.content)
                is AssistantMessage.Assistant -> message.content?.let(AssistantSessionMessage::Assistant)
                is AssistantMessage.System,
                is AssistantMessage.Tool -> null
            }
        }
    )

    private fun givenToolCallFinished(
        toolCallId: String,
        toolName: String,
        status: AiAssistantToolStatusValue = AiAssistantToolStatusValue.Success,
        errorKind: AiAssistantErrorKindValue? = null,
        durationMs: Long? = 1L,
        emitTelemetry: Boolean = false,
        context: AssistantTelemetryContext = runtime.startRequests.last().telemetryContext,
    ) = AssistantRuntimeEvent.ToolCallFinished(
        toolCallId = toolCallId,
        toolName = toolName,
        status = status,
        errorKind = errorKind,
        durationMs = durationMs,
        emitTelemetry = emitTelemetry,
        telemetryContext = context,
    )

    private fun AssistantUiState.toolActivitySegments(): List<AssistantUiSegment.ToolActivity> =
        messages.toolActivitySegments()

    private fun List<AssistantUiMessage>.toolActivitySegments(): List<AssistantUiSegment.ToolActivity> =
        flatMap { it.segments }.filterIsInstance<AssistantUiSegment.ToolActivity>()

    private inline fun <reified T : Trackable> RecordingAssistantTelemetryTracker.singleEvent(): T =
        events.filterIsInstance<T>().single()

    private fun AiAssistantConversationStartedEvent.context() = AssistantTelemetryContext(
        conversationId = conversationId,
        requestId = requestId,
        messageId = messageId,
    )

    private fun AiAssistantTurnStartedEvent.context() = AssistantTelemetryContext(
        conversationId = conversationId,
        requestId = requestId,
        messageId = messageId,
    )

    private fun AiAssistantToolCallCompletedEvent.context() = AssistantTelemetryContext(
        conversationId = conversationId,
        requestId = requestId,
        messageId = messageId,
    )

    private fun AiAssistantShowCardsProcessedEvent.context() = AssistantTelemetryContext(
        conversationId = conversationId,
        requestId = requestId,
        messageId = messageId,
    )

    private fun AiAssistantCardTappedEvent.context() = AssistantTelemetryContext(
        conversationId = conversationId,
        requestId = requestId,
        messageId = messageId,
    )

    private companion object {
        const val CONVERSATION_ID = "assistant-id-1"
        const val SITE_ID = 123L
    }
}
