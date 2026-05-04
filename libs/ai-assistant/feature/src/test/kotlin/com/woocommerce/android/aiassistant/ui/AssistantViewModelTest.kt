package com.woocommerce.android.aiassistant.ui

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.runtime.AssistantRuntime
import com.woocommerce.android.aiassistant.runtime.AssistantRuntimeConfirmationDispatchResult
import com.woocommerce.android.aiassistant.runtime.AssistantRuntimeEvent
import com.woocommerce.android.aiassistant.runtime.AssistantTurnRequest
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardEntry
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardKey
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var runtime: FakeAssistantRuntime
    private lateinit var selectedSite: SelectedSite
    private lateinit var viewModel: AssistantViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runtime = FakeAssistantRuntime()
        selectedSite = mock {
            on { get() } doReturn SiteModel().apply { siteId = SITE_ID }
        }
        viewModel = AssistantViewModel(
            conversationId = CONVERSATION_ID,
            runtime = runtime,
            selectedSite = selectedSite,
            idGenerator = SequentialAssistantMessageIdGenerator(),
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
            AssistantUiMessage(id = "message-1", role = AssistantUiMessage.Role.USER, text = "Show my recent orders"),
            AssistantUiMessage(id = "message-2", role = AssistantUiMessage.Role.ASSISTANT, text = ""),
        )
        assertThat(runtime.startRequests).containsExactly(
            AssistantTurnRequest(
                conversationId = CONVERSATION_ID,
                siteId = SITE_ID,
                toolScope = ToolScope.GLOBAL,
                userMessage = "Show my recent orders",
                history = emptyList(),
            )
        )
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
    fun `given active tool activity, when matching tool finishes, then activity is preserved as completed`() = runTest {
        viewModel.onSendMessage("Find order 123")
        runtime.emit(AssistantRuntimeEvent.ToolCallStarted(toolCallId = "call-1", toolName = "orders_get"))
        runtime.emit(AssistantRuntimeEvent.ToolCallFinished(toolCallId = "call-1"))
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
            AssistantRuntimeEvent.Finished(
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
            AssistantRuntimeEvent.Finished(
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
        runtime.emit(AssistantRuntimeEvent.ToolCallFinished(toolCallId = "call-1"))
        runtime.emit(
            AssistantRuntimeEvent.Finished(
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
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(AssistantMessage.User("Find order 123")),
                retryAvailable = true,
                error = AssistantError.Network,
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
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(AssistantMessage.User("Hello")),
                retryAvailable = true,
                error = AssistantError.Network,
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.ERROR)
        assertThat(viewModel.uiState.value.error).isEqualTo(AssistantUiError.NETWORK)
        assertThat(viewModel.uiState.value.canRetry).isTrue()
        assertThat(viewModel.uiState.value.isTurnActive).isFalse()

        viewModel.onRetry()

        assertThat(runtime.retryRequests).containsExactly(
            AssistantTurnRequest(
                conversationId = CONVERSATION_ID,
                siteId = SITE_ID,
                toolScope = ToolScope.GLOBAL,
                userMessage = "Hello",
                history = emptyList(),
            )
        )
    }

    @Test
    fun `given network failure with retry available, when turn fails, then active assistant message exposes retry`() =
        runTest {
            viewModel.onSendMessage("Hello")
            val activeAssistantId = viewModel.uiState.value.messages.last().id

            runtime.emit(
                AssistantRuntimeEvent.Finished(
                    outcome = LoopOutcome.FAILED,
                    updatedHistory = listOf(AssistantMessage.User("Hello")),
                    retryAvailable = true,
                    error = AssistantError.Network,
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
                        error = AssistantError.Network,
                        canRetry = true,
                    ),
                )
            )

            viewModel.onRetry()

            assertThat(runtime.retryRequests).containsExactly(
                AssistantTurnRequest(
                    conversationId = CONVERSATION_ID,
                    siteId = SITE_ID,
                    toolScope = ToolScope.GLOBAL,
                    userMessage = "Hello",
                    history = emptyList(),
                )
            )
        }

    @Test
    fun `given outcome unknown failure, when turn fails, then verify message has no retry action`() =
        runTest {
            viewModel.onSendMessage("Update order 42")
            val activeAssistantId = viewModel.uiState.value.messages.last().id

            runtime.emit(
                AssistantRuntimeEvent.Finished(
                    outcome = LoopOutcome.FAILED,
                    updatedHistory = listOf(
                        AssistantMessage.User("Update order 42"),
                        AssistantMessage.Assistant("I'll update that order."),
                    ),
                    retryAvailable = false,
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
                AssistantRuntimeEvent.Finished(
                    outcome = LoopOutcome.FAILED,
                    updatedHistory = listOf(AssistantMessage.User("Hello")),
                    retryAvailable = true,
                    error = AssistantError.UpstreamFailure,
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
                        error = AssistantError.UpstreamFailure,
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
                AssistantRuntimeEvent.Finished(
                    outcome = LoopOutcome.FAILED,
                    updatedHistory = listOf(AssistantMessage.User("Hello")),
                    retryAvailable = false,
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
            assertThat(normalizedError.toMessageRes()).isEqualTo(R.string.assistant_chat_error_unknown)
        }

    @Test
    fun `when turn reaches max iterations, then state exposes an error`() = runTest {
        viewModel.onSendMessage("Hello")

        runtime.emit(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.MAX_ITERATIONS,
                updatedHistory = listOf(AssistantMessage.User("Hello")),
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.ERROR)
        assertThat(viewModel.uiState.value.error).isEqualTo(AssistantUiError.MAX_ITERATIONS)
        assertThat(viewModel.uiState.value.shouldShowFallbackError).isTrue()
        assertThat(requireNotNull(viewModel.uiState.value.error).toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_max_iterations)
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
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.COMPLETED,
                updatedHistory = priorHistory,
            )
        )
        advanceUntilIdle()

        viewModel.onSendMessage("Current question")
        runtime.emit(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = priorHistory + listOf(
                    AssistantMessage.User("Current question"),
                    AssistantMessage.Assistant("Partial answer"),
                ),
                retryAvailable = true,
                error = AssistantError.Network,
            )
        )
        advanceUntilIdle()

        viewModel.onRetry()

        assertThat(runtime.retryRequests.last()).isEqualTo(
            AssistantTurnRequest(
                conversationId = CONVERSATION_ID,
                siteId = SITE_ID,
                toolScope = ToolScope.GLOBAL,
                userMessage = "Current question",
                history = priorHistory,
            )
        )
    }

    @Test
    fun `given retry fails, when retried again, then retry still uses original pre-turn history`() = runTest {
        val priorHistory = listOf(
            AssistantMessage.User("Previous question"),
            AssistantMessage.Assistant("Previous answer"),
        )
        viewModel.onSendMessage("Previous question")
        runtime.emit(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.COMPLETED,
                updatedHistory = priorHistory,
            )
        )
        advanceUntilIdle()
        viewModel.onSendMessage("Current question")
        runtime.emit(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = priorHistory + listOf(
                    AssistantMessage.User("Current question"),
                    AssistantMessage.Assistant("First failure"),
                ),
                retryAvailable = true,
                error = AssistantError.Network,
            )
        )
        advanceUntilIdle()

        viewModel.onRetry()
        runtime.emit(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = priorHistory + listOf(
                    AssistantMessage.User("Current question"),
                    AssistantMessage.Assistant("Second failure"),
                ),
                retryAvailable = true,
                error = AssistantError.Network,
            )
        )
        advanceUntilIdle()
        viewModel.onRetry()

        assertThat(runtime.retryRequests).hasSize(2)
        assertThat(runtime.retryRequests.map { it.history }).containsOnly(priorHistory)
    }

    @Test
    fun `given retryable failed turn, when a new turn starts, then previous retry action is disabled`() = runTest {
        viewModel.onSendMessage("First")
        runtime.emit(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(AssistantMessage.User("First")),
                retryAvailable = true,
                error = AssistantError.Network,
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
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(AssistantMessage.User("First")),
                retryAvailable = true,
                error = AssistantError.Network,
            )
        )
        advanceUntilIdle()

        viewModel.onSendMessage("Second")
        runtime.emit(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(
                    AssistantMessage.User("First"),
                    AssistantMessage.User("Second"),
                ),
                retryAvailable = true,
                error = AssistantError.Timeout,
            )
        )
        advanceUntilIdle()

        val messageErrors = viewModel.uiState.value.messages.mapNotNull { it.error }
        assertThat(messageErrors.map { it.canRetry }).containsExactly(false, true)

        viewModel.onRetry()

        assertThat(runtime.retryRequests).containsExactly(
            AssistantTurnRequest(
                conversationId = CONVERSATION_ID,
                siteId = SITE_ID,
                toolScope = ToolScope.GLOBAL,
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
    fun `given active assistant bubble, when card entries arrive, then card segments are appended`() = runTest {
        viewModel.onSendMessage("Show order 123")
        val activeBubbleId = viewModel.uiState.value.messages.last().id
        val orderEntry = givenOrderEntry(id = "123", number = "#123")

        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(orderEntry)))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last()).isEqualTo(
            AssistantUiMessage(
                id = activeBubbleId,
                role = AssistantUiMessage.Role.ASSISTANT,
                segments = listOf(
                    AssistantUiSegment.Text(""),
                    AssistantUiSegment.Card(orderEntry.card),
                ),
            )
        )
    }

    @Test
    fun `given cards arrive between text deltas, when turn finishes, then cards stay on active assistant message`() =
        runTest {
            viewModel.onSendMessage("Show order 123")
            val activeBubbleId = viewModel.uiState.value.messages.last().id
            val orderEntry = givenOrderEntry(id = "123", number = "#123")

            runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Here is the order."))
            runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(orderEntry)))
            runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Anything else?"))
            runtime.emit(
                AssistantRuntimeEvent.Finished(
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
                        AssistantUiSegment.Card(orderEntry.card),
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
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.COMPLETED,
                updatedHistory = listOf(
                    AssistantMessage.User("Show missing order"),
                    AssistantMessage.Assistant("I could not find that order."),
                ),
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last().segments.filterIsInstance<AssistantUiSegment.Card>())
            .isEmpty()
    }

    @Test
    fun `given duplicate card keys across one turn, when cards arrive, then first seen cards are kept`() = runTest {
        viewModel.onSendMessage("Show matching cards")
        val firstOrder = givenOrderEntry(id = "123", number = "#123")
        val duplicateOrder = givenOrderEntry(id = "123", number = "#duplicate")
        val secondOrder = givenOrderEntry(id = "456", number = "#456")

        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(firstOrder)))
        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(duplicateOrder, secondOrder)))
        advanceUntilIdle()

        val cardSegments = viewModel.uiState.value.messages.last().segments
            .filterIsInstance<AssistantUiSegment.Card>()

        assertThat(cardSegments).containsExactly(
            AssistantUiSegment.Card(firstOrder.card),
            AssistantUiSegment.Card(secondOrder.card),
        )
    }

    @Test
    fun `given same id across different families, when cards arrive, then both cards are kept`() = runTest {
        viewModel.onSendMessage("Show order and product 123")
        val order = givenOrderEntry(id = "123", number = "#123")
        val product = givenProductEntry(id = "123", name = "Socks")

        runtime.emit(AssistantRuntimeEvent.CardsResolved(listOf(order, product)))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last().segments)
            .contains(
                AssistantUiSegment.Card(order.card),
                AssistantUiSegment.Card(product.card),
            )
    }

    @Test
    fun `given finished history contains card shaped tool json, when reduced, then no card segment is created`() =
        runTest {
            viewModel.onSendMessage("Show analytics")

            runtime.emit(
                AssistantRuntimeEvent.Finished(
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

            assertThat(viewModel.uiState.value.messages.last().segments.filterIsInstance<AssistantUiSegment.Card>())
                .isEmpty()
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
            .isEqualTo(R.string.assistant_chat_error_cancelled)
        assertThat(viewModel.uiState.value.messages.last().error).isNull()
        assertThat(viewModel.uiState.value.canRetry).isFalse()
        assertThat(viewModel.uiState.value.activeConfirmationId).isNull()
        assertThat(viewModel.uiState.value.isTurnActive).isFalse()
    }

    @Test
    fun `when runtime finishes with cancelled error, then state exposes cancelled ui error`() = runTest {
        viewModel.onSendMessage("Hello")

        runtime.emit(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.STOPPED,
                updatedHistory = listOf(
                    AssistantMessage.User("Hello"),
                    AssistantMessage.Assistant("Partial"),
                ),
                retryAvailable = false,
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
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.FAILED,
                updatedHistory = listOf(
                    AssistantMessage.User("Hello"),
                    AssistantMessage.Assistant("Partial"),
                ),
                retryAvailable = true,
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
                AssistantTurnRequest(
                    conversationId = CONVERSATION_ID,
                    siteId = SITE_ID,
                    toolScope = ToolScope.GLOBAL,
                    userMessage = "What changed?",
                    history = listOf(
                        AssistantMessage.User("Summarize sales"),
                        AssistantMessage.Assistant("Sales are up today"),
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
                AssistantRuntimeEvent.Finished(
                    outcome = LoopOutcome.FAILED,
                    updatedHistory = listOf(AssistantMessage.User("Find order 123")),
                    retryAvailable = true,
                    error = AssistantError.Network,
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
            AssistantTurnRequest(
                conversationId = CONVERSATION_ID,
                siteId = SITE_ID,
                toolScope = ToolScope.GLOBAL,
                userMessage = "First",
                history = emptyList(),
            )
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
            AssistantTurnRequest(
                conversationId = CONVERSATION_ID,
                siteId = SITE_ID,
                toolScope = ToolScope.GLOBAL,
                userMessage = "Cancel order 123",
                history = emptyList(),
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
            AssistantRuntimeEvent.Finished(
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
            AssistantRuntimeEvent.Finished(
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
            .isEqualTo(R.string.assistant_chat_error_confirmation_deferred)
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
                AssistantRuntimeEvent.Finished(
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

    private fun givenConfirmationCard() = AssistantConfirmationCard(
        confirmationId = "confirmation-1",
        toolCall = ToolCall(
            id = "call-1",
            name = "orders_update",
            arguments = buildJsonObject { put("id", 123) },
        ),
        state = AssistantConfirmationCardState.PENDING,
    )

    private fun givenOrderEntry(id: String, number: String) = AssistantCardEntry(
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

    private fun givenProductEntry(id: String, name: String) = AssistantCardEntry(
        key = AssistantCardKey(family = "product", id = id),
        card = AssistantCard.Product(
            remoteProductId = id.toLong(),
            name = name,
            sku = "woo-socks",
            price = "9.99",
            stockStatus = "instock",
            status = "publish",
            imageUrl = "https://example.com/socks.png",
        ),
    )

    private class FakeAssistantRuntime : AssistantRuntime {
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
    }

    private class SequentialAssistantMessageIdGenerator : AssistantMessageIdGenerator {
        private var count = 0

        override fun nextId(): String {
            count += 1
            return "message-$count"
        }
    }

    private fun AssistantUiState.toolActivitySegments(): List<AssistantUiSegment.ToolActivity> =
        messages.toolActivitySegments()

    private fun List<AssistantUiMessage>.toolActivitySegments(): List<AssistantUiSegment.ToolActivity> =
        flatMap { it.segments }.filterIsInstance<AssistantUiSegment.ToolActivity>()

    private companion object {
        const val CONVERSATION_ID = "conversation-1"
        const val SITE_ID = 123L
    }
}
