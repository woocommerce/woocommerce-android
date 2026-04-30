package com.woocommerce.android.aiassistant.ui

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.runtime.AssistantPendingConfirmation
import com.woocommerce.android.aiassistant.runtime.AssistantRuntime
import com.woocommerce.android.aiassistant.runtime.AssistantRuntimeConfirmationResult
import com.woocommerce.android.aiassistant.runtime.AssistantRuntimeEvent
import com.woocommerce.android.aiassistant.runtime.AssistantTurnRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var runtime: FakeAssistantRuntime
    private lateinit var viewModel: AssistantViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runtime = FakeAssistantRuntime()
        viewModel = AssistantViewModel(
            runtime = runtime,
            conversationId = CONVERSATION_ID,
            siteId = SITE_ID,
            toolScope = ToolScope.GLOBAL,
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
        assertThat(viewModel.uiState.value.pendingConfirmation).isNull()
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `when message is sent, then user and active assistant messages are added and runtime starts turn`() = runTest {
        viewModel.onSendMessage("Show my recent orders")

        val state = viewModel.uiState.value
        assertThat(state.status).isEqualTo(AssistantUiStatus.STREAMING)
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
        assertThat(state.canRetry).isFalse()
        assertThat(state.error).isNull()
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
        assertThat(viewModel.uiState.value.canRetry).isFalse()
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
    fun `when runtime awaits confirmation, then state exposes pending confirmation`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        val confirmation = AssistantPendingConfirmation(
            id = "confirmation-1",
            toolCall = ToolCall(
                id = "call-1",
                name = "orders_update",
                arguments = buildJsonObject { put("id", 123) },
            ),
        )

        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(confirmation))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.AWAITING_CONFIRMATION)
        assertThat(viewModel.uiState.value.pendingConfirmation).isEqualTo(confirmation)
    }

    @Test
    fun `when cancel is requested, then runtime is cancelled and state returns to idle`() = runTest {
        viewModel.onSendMessage("Hello")

        viewModel.onCancelTurn()
        advanceUntilIdle()

        assertThat(runtime.cancelledConversationIds).containsExactly(CONVERSATION_ID)
        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.IDLE)
    }

    @Test
    fun `given pending confirmation, when confirm write is requested, then runtime confirm is called`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenPendingConfirmation()))
        advanceUntilIdle()

        viewModel.onConfirmWrite()
        advanceUntilIdle()

        assertThat(runtime.confirmedConfirmationIds).containsExactly("confirmation-1")
        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.STREAMING)
        assertThat(viewModel.uiState.value.error).isNull()
        assertThat(viewModel.uiState.value.pendingConfirmation).isNull()
    }

    @Test
    fun `given confirmed write, when assistant text resumes, then existing assistant bubble grows`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        val activeBubbleId = viewModel.uiState.value.messages.last().id
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenPendingConfirmation()))
        advanceUntilIdle()

        viewModel.onConfirmWrite()
        runtime.emit(AssistantRuntimeEvent.AssistantTextDelta("Order updated"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages.last()).isEqualTo(
            AssistantUiMessage(
                id = activeBubbleId,
                role = AssistantUiMessage.Role.ASSISTANT,
                text = "Order updated",
            )
        )
    }

    @Test
    fun `given pending confirmation, when confirm is deferred, then state exposes error`() = runTest {
        runtime.confirmationResult = AssistantRuntimeConfirmationResult.Deferred
        viewModel.onSendMessage("Cancel order 123")
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenPendingConfirmation()))
        advanceUntilIdle()

        viewModel.onConfirmWrite()
        advanceUntilIdle()

        assertThat(runtime.confirmedConfirmationIds).containsExactly("confirmation-1")
        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.ERROR)
        assertThat(viewModel.uiState.value.error).isEqualTo(AssistantUiError.CONFIRMATION_DEFERRED)
        assertThat(viewModel.uiState.value.pendingConfirmation).isNull()
    }

    @Test
    fun `given pending confirmation, when cancel write is requested, then runtime cancel write is called`() = runTest {
        viewModel.onSendMessage("Cancel order 123")
        runtime.emit(AssistantRuntimeEvent.AwaitingConfirmation(givenPendingConfirmation()))
        advanceUntilIdle()

        viewModel.onCancelWrite()
        advanceUntilIdle()

        assertThat(runtime.cancelledConfirmationIds).containsExactly("confirmation-1")
        assertThat(viewModel.uiState.value.status).isEqualTo(AssistantUiStatus.IDLE)
        assertThat(viewModel.uiState.value.pendingConfirmation).isNull()
    }

    private fun givenPendingConfirmation() = AssistantPendingConfirmation(
        id = "confirmation-1",
        toolCall = ToolCall(
            id = "call-1",
            name = "orders_update",
            arguments = buildJsonObject { put("id", 123) },
        ),
    )

    private class FakeAssistantRuntime : AssistantRuntime {
        val startRequests = mutableListOf<AssistantTurnRequest>()
        val retryRequests = mutableListOf<AssistantTurnRequest>()
        val cancelledConversationIds = mutableListOf<String>()
        val confirmedConfirmationIds = mutableListOf<String>()
        val cancelledConfirmationIds = mutableListOf<String>()
        var confirmationResult: AssistantRuntimeConfirmationResult = AssistantRuntimeConfirmationResult.Accepted

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

        override suspend fun confirmWrite(confirmationId: String): AssistantRuntimeConfirmationResult {
            confirmedConfirmationIds += confirmationId
            return confirmationResult
        }

        override suspend fun cancelWrite(confirmationId: String) {
            cancelledConfirmationIds += confirmationId
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

    private companion object {
        const val CONVERSATION_ID = "conversation-1"
        const val SITE_ID = 123L
    }
}
