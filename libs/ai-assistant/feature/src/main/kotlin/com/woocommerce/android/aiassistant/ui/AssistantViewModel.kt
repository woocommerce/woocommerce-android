package com.woocommerce.android.aiassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automattic.eventhorizon.AiAssistantErrorKindValue
import com.automattic.eventhorizon.AiAssistantTurnOutcomeValue
import com.woocommerce.android.aiassistant.config.AssistantConfig
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.RetryAffordance
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.runtime.AssistantRuntime
import com.woocommerce.android.aiassistant.runtime.AssistantRuntimeConfirmationDispatchResult
import com.woocommerce.android.aiassistant.runtime.AssistantRuntimeEvent
import com.woocommerce.android.aiassistant.runtime.AssistantTurnRequest
import com.woocommerce.android.aiassistant.telemetry.AssistantErrorKindMapper
import com.woocommerce.android.aiassistant.telemetry.AssistantIdGenerator
import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetryContext
import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetryEventFactory
import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetryTracker
import com.woocommerce.android.aiassistant.telemetry.CardTelemetryFamilyMapper
import com.woocommerce.android.aiassistant.telemetry.SystemClock
import com.woocommerce.android.aiassistant.tools.handlers.cards.SHOW_CARDS_TOOL_NAME
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardKey
import com.woocommerce.android.tools.SelectedSite
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AssistantViewModel.Factory::class)
@Suppress("LargeClass")
class AssistantViewModel @AssistedInject constructor(
    private val runtime: AssistantRuntime,
    private val selectedSite: SelectedSite,
    private val assistantTelemetryTracker: AssistantTelemetryTracker,
    private val systemClock: SystemClock,
    private val assistantIdGenerator: AssistantIdGenerator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()
    private val _pendingCardNavigation = MutableSharedFlow<AssistantCardAction>(replay = 0, extraBufferCapacity = 1)
    val pendingCardNavigation: SharedFlow<AssistantCardAction> = _pendingCardNavigation.asSharedFlow()

    private var turnJob: Job? = null
    private var activeAssistantMessageId: String? = null
    private var activeTurn: ActiveTurn? = null
    private var conversationId: String = assistantIdGenerator.nextId()
    private var conversationStartedTracked = false
    private var history: List<AssistantMessage> = emptyList()
    private var lastTurnBaseHistory: List<AssistantMessage> = emptyList()
    private var lastUserMessage: String? = null
    private val activeCardKeys = linkedSetOf<AssistantCardKey>()
    private val messageTurnContext = linkedMapOf<String, AssistantTelemetryContext>()
    private val suppressedRuntimeTelemetryRequestIds = mutableSetOf<String>()

    fun onSendMessage(message: String) {
        if (_uiState.value.isTurnActive) return

        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) return

        lastUserMessage = trimmedMessage
        startTurn(trimmedMessage, isRetry = false)
    }

    fun onRetry() {
        if (_uiState.value.isTurnActive || !_uiState.value.canRetry) return

        val message = lastUserMessage ?: return
        startTurn(message, isRetry = true)
    }

    fun onCancelTurn() {
        if (!_uiState.value.isTurnActive) return
        if (_uiState.value.activeConfirmationId != null) {
            cancelOpenConfirmationSegments()
            return
        }

        preserveCancelledTurnInHistory()
        activeTurn?.let { finalizeTurn(it, AiAssistantTurnOutcomeValue.CancelledByUser, errorKind = null) }
        turnJob?.cancel()
        turnJob = null
        activeAssistantMessageId = null
        viewModelScope.launch {
            runtime.cancelTurn(conversationId)
        }
        _uiState.update {
            it.copy(
                messages = it.messages.withoutTransientActivity(),
                status = AssistantUiStatus.ERROR,
                error = AssistantError.Cancelled.toAssistantUiError(),
                canRetry = false,
                activeConfirmationId = null,
                activeAssistantMessageId = null,
            )
        }
    }

    fun onConfirmWrite() {
        val confirmationId = _uiState.value.activeConfirmationId ?: return
        viewModelScope.launch {
            when (
                runtime.resolveConfirmation(
                    ConfirmationResult(confirmationId, ConfirmationDecision.CONFIRMED)
                )
            ) {
                AssistantRuntimeConfirmationDispatchResult.Accepted -> {
                    _uiState.update {
                        it.copy(
                            status = AssistantUiStatus.STREAMING,
                            error = null,
                            canRetry = false,
                            activeConfirmationId = null,
                        )
                    }
                }
                AssistantRuntimeConfirmationDispatchResult.Deferred -> {
                    activeAssistantMessageId = null
                    _uiState.update {
                        it.copy(
                            messages = it.messages.withoutTransientActivity(),
                            status = AssistantUiStatus.ERROR,
                            error = AssistantUiError.CONFIRMATION_DEFERRED,
                            canRetry = false,
                            activeConfirmationId = null,
                            activeAssistantMessageId = null,
                        )
                    }
                }
            }
        }
    }

    fun onCancelWrite() {
        cancelOpenConfirmationSegments()
    }

    fun onRestartConversation() {
        val shouldCancelTurn = _uiState.value.isTurnActive
        val previousConversationId = conversationId
        activeTurn?.let { finalizeTurn(it, AiAssistantTurnOutcomeValue.CancelledByUser, errorKind = null) }
        turnJob?.cancel()
        turnJob = null
        activeAssistantMessageId = null
        conversationId = assistantIdGenerator.nextId()
        conversationStartedTracked = false
        history = emptyList()
        lastTurnBaseHistory = emptyList()
        lastUserMessage = null
        activeCardKeys.clear()
        messageTurnContext.clear()
        suppressedRuntimeTelemetryRequestIds.clear()
        _uiState.value = AssistantUiState()

        if (shouldCancelTurn) {
            viewModelScope.launch {
                runtime.cancelTurn(previousConversationId)
            }
        }
    }

    fun onCardTapped(
        card: AssistantCard,
        action: AssistantCardAction,
        sourceMessageId: String,
    ) {
        val context = messageTurnContext[sourceMessageId] ?: return
        assistantTelemetryTracker.track(
            AssistantTelemetryEventFactory.cardTapped(
                context = context,
                cardFamily = CardTelemetryFamilyMapper.familyOf(card),
                actionFamily = CardTelemetryFamilyMapper.actionOf(action),
            )
        )
        _pendingCardNavigation.tryEmit(action)
    }

    @Suppress("LongMethod")
    private fun startTurn(message: String, isRetry: Boolean) {
        activeTurn?.let { finalizeTurn(it, AiAssistantTurnOutcomeValue.CancelledByUser, errorKind = null) }
        turnJob?.cancel()
        activeCardKeys.clear()
        if (!isRetry) {
            lastTurnBaseHistory = history
        }
        val userMessage = if (isRetry) {
            null
        } else {
            AssistantUiMessage(assistantIdGenerator.nextId(), AssistantUiMessage.Role.USER, message)
        }
        val assistantMessageId = assistantIdGenerator.nextId()
        val telemetryContext = AssistantTelemetryContext(
            conversationId = conversationId,
            requestId = assistantIdGenerator.nextId(),
            messageId = assistantMessageId,
        )
        messageTurnContext[assistantMessageId] = telemetryContext
        activeTurn = ActiveTurn(
            context = telemetryContext,
            isRetry = isRetry,
            startedAtMs = systemClock.nowMs(),
        )

        _uiState.update { state ->
            activeAssistantMessageId = assistantMessageId
            val turnMessages = if (isRetry) {
                listOf(AssistantUiMessage(assistantMessageId, AssistantUiMessage.Role.ASSISTANT, ""))
            } else {
                listOf(
                    checkNotNull(userMessage),
                    AssistantUiMessage(assistantMessageId, AssistantUiMessage.Role.ASSISTANT, ""),
                )
            }
            state.copy(
                messages = state.messages.withoutTransientActivity().withoutRetryActions() + turnMessages,
                status = AssistantUiStatus.STREAMING,
                error = null,
                canRetry = false,
                activeConfirmationId = null,
                activeAssistantMessageId = assistantMessageId,
            )
        }

        val request = AssistantTurnRequest(
            conversationId = conversationId,
            telemetryContext = telemetryContext,
            siteId = selectedSite.get().siteId,
            toolScope = ToolScope.GLOBAL,
            userMessage = message,
            history = lastTurnBaseHistory,
        )
        val events = if (isRetry) runtime.retryTurn(request) else runtime.startTurn(request)
        turnJob = viewModelScope.launch {
            events.collect(::reduceRuntimeEvent)
        }
        if (!conversationStartedTracked) {
            conversationStartedTracked = true
            assistantTelemetryTracker.track(AssistantTelemetryEventFactory.conversationStarted(telemetryContext))
        }
        assistantTelemetryTracker.track(
            AssistantTelemetryEventFactory.turnStarted(
                context = telemetryContext,
                isRetry = isRetry,
                completionStack = AssistantConfig.COMPLETION_STACK,
                promptVersion = AssistantConfig.PROMPT_VERSION,
                toolCatalogVersion = AssistantConfig.TOOL_CATALOG_VERSION,
            )
        )
    }

    @Suppress("LongMethod")
    private fun reduceRuntimeEvent(event: AssistantRuntimeEvent) {
        when (event) {
            is AssistantRuntimeEvent.AssistantTextDelta -> appendAssistantText(event.text)
            is AssistantRuntimeEvent.ToolCallStarted -> showToolActivity(event)
            is AssistantRuntimeEvent.ToolCallFinished -> {
                markToolActivityCompleted(event.toolCallId)
                trackToolCallCompleted(event)
            }
            is AssistantRuntimeEvent.AwaitingConfirmation -> {
                _uiState.update {
                    it.copy(
                        messages = it.messages.withConfirmationCard(
                            activeMessageId = activeAssistantMessageId,
                            confirmation = event.confirmation,
                            nextId = assistantIdGenerator::nextId,
                        ),
                        status = AssistantUiStatus.AWAITING_CONFIRMATION,
                        error = null,
                        canRetry = false,
                        activeConfirmationId = event.confirmation.confirmationId,
                    )
                }
            }
            is AssistantRuntimeEvent.ConfirmationResolved -> {
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.withUpdatedConfirmationCard(
                            confirmationId = event.result.requestId,
                            state = event.result.decision.toCardState(),
                        )
                    )
                }
            }
            is AssistantRuntimeEvent.CardsResolved -> appendAssistantCards(event.cards)
            is AssistantRuntimeEvent.ShowCardsProcessed -> trackShowCardsProcessed(event)
            is AssistantRuntimeEvent.Finished -> {
                val activeMessageId = activeAssistantMessageId
                val normalizedError = event.normalizedAssistantError()
                val canRetry = event.canRetry()
                val turnOutcome = event.toTurnOutcome()
                val errorKind = normalizedError
                    ?.takeIf { turnOutcome == AiAssistantTurnOutcomeValue.Failed }
                    ?.let(AssistantErrorKindMapper::map)
                activeTurn?.let {
                    finalizeTurn(
                        turn = it,
                        outcome = turnOutcome,
                        errorKind = errorKind,
                    )
                }
                activeAssistantMessageId = null
                activeCardKeys.clear()
                history = event.updatedHistory
                _uiState.update {
                    it.copy(
                        messages = it.messages
                            .withoutTransientActivity()
                            .withAssistantError(
                                activeMessageId = activeMessageId,
                                error = normalizedError,
                                canRetry = canRetry,
                                nextId = assistantIdGenerator::nextId,
                            ),
                        status = event.toAssistantUiStatus(),
                        error = event.toAssistantUiError(),
                        canRetry = canRetry,
                        activeConfirmationId = null,
                        activeAssistantMessageId = null,
                    )
                }
            }
        }
    }

    private fun showToolActivity(event: AssistantRuntimeEvent.ToolCallStarted) {
        if (event.toolName == SHOW_CARDS_TOOL_NAME) return

        val messageId = activeAssistantMessageId ?: return
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == messageId) {
                        message.withToolActivity(
                            AssistantToolActivity(
                                toolCallId = event.toolCallId,
                                toolName = event.toolName,
                            )
                        )
                    } else {
                        message
                    }
                }
            )
        }
    }

    private fun markToolActivityCompleted(toolCallId: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.withToolActivityStatus(toolCallId, AssistantToolActivity.Status.COMPLETED)
            )
        }
    }

    private fun trackToolCallCompleted(event: AssistantRuntimeEvent.ToolCallFinished) {
        if (!event.emitTelemetry) return
        val context = event.telemetryContext
        if (!isTrackableRuntimeContext(context)) return
        assistantTelemetryTracker.track(
            AssistantTelemetryEventFactory.toolCallCompleted(
                context = context,
                toolName = event.toolName,
                status = event.status,
                errorKind = event.errorKind,
                durationMs = event.durationMs,
            )
        )
    }

    private fun trackShowCardsProcessed(event: AssistantRuntimeEvent.ShowCardsProcessed) {
        val context = event.telemetryContext
        if (!isTrackableRuntimeContext(context)) return
        assistantTelemetryTracker.track(
            AssistantTelemetryEventFactory.showCardsProcessed(
                context = context,
                requestedCount = event.counts.requestedCount,
                renderedCount = event.counts.renderedCount,
                missingCount = event.counts.missingCount,
                rejectedCount = event.counts.rejectedCount,
            )
        )
    }

    private fun isTrackableRuntimeContext(context: AssistantTelemetryContext): Boolean =
        messageTurnContext[context.messageId] != null &&
            context.requestId !in suppressedRuntimeTelemetryRequestIds

    private fun preserveCancelledTurnInHistory() {
        val userMessage = lastUserMessage ?: return
        val assistantMessageId = activeAssistantMessageId
        val assistantText = _uiState.value.messages
            .firstOrNull { it.id == assistantMessageId }
            ?.text
            .orEmpty()
        val cancelledTurnHistory = buildList {
            add(AssistantMessage.User(userMessage))
            assistantText.takeIf { it.isNotEmpty() }?.let {
                add(AssistantMessage.Assistant(content = it))
            }
        }
        history = lastTurnBaseHistory + cancelledTurnHistory
    }

    private fun cancelOpenConfirmationSegments() {
        val confirmationId = _uiState.value.activeConfirmationId ?: return
        viewModelScope.launch {
            when (
                runtime.resolveConfirmation(
                    ConfirmationResult(confirmationId, ConfirmationDecision.CANCELLED)
                )
            ) {
                AssistantRuntimeConfirmationDispatchResult.Accepted -> {
                    _uiState.update {
                        it.copy(
                            error = null,
                            canRetry = false,
                            activeConfirmationId = null,
                        )
                    }
                }
                AssistantRuntimeConfirmationDispatchResult.Deferred -> {
                    activeAssistantMessageId = null
                    _uiState.update {
                        it.copy(
                            messages = it.messages.withoutTransientActivity(),
                            status = AssistantUiStatus.ERROR,
                            error = AssistantUiError.CONFIRMATION_DEFERRED,
                            canRetry = false,
                            activeConfirmationId = null,
                            activeAssistantMessageId = null,
                        )
                    }
                }
            }
        }
    }

    private fun appendAssistantCards(cards: List<AssistantCard>) {
        val messageId = activeAssistantMessageId ?: return
        val newCards = cards
            .filter { activeCardKeys.add(it.toCardKey()) }
        if (newCards.isEmpty()) return

        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == messageId) {
                        message.appendCardGroup(newCards)
                    } else {
                        message
                    }
                }
            )
        }
    }

    private fun AssistantCard.toCardKey(): AssistantCardKey =
        when (this) {
            is AssistantCard.Order -> AssistantCardKey(family = "order", id = remoteOrderId.toString())
            is AssistantCard.Product -> AssistantCardKey(family = "product", id = remoteProductId.toString())
            is AssistantCard.Variation -> AssistantCardKey(
                family = "variation",
                id = "$parentProductId/$variationId",
            )
            is AssistantCard.Customer -> AssistantCardKey(family = "customer", id = remoteCustomerId.toString())
            is AssistantCard.Stats -> AssistantCardKey(family = "analytics_stats", id = id)
        }

    private fun appendAssistantText(delta: String) {
        val messageId = activeAssistantMessageId ?: return
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == messageId) {
                        message.appendText(delta)
                    } else {
                        message
                    }
                }
            )
        }
    }

    private fun LoopOutcome.toAssistantUiStatus(): AssistantUiStatus = when (this) {
        LoopOutcome.COMPLETED,
        LoopOutcome.STOPPED -> AssistantUiStatus.IDLE
        LoopOutcome.FAILED,
        LoopOutcome.MAX_ITERATIONS -> AssistantUiStatus.ERROR
    }

    private fun AssistantRuntimeEvent.Finished.toAssistantUiStatus(): AssistantUiStatus = when {
        error is AssistantError.Cancelled -> AssistantUiStatus.ERROR
        else -> outcome.toAssistantUiStatus()
    }

    private fun AssistantRuntimeEvent.Finished.toAssistantUiError(): AssistantUiError? =
        error?.toAssistantUiError() ?: when (outcome) {
            LoopOutcome.COMPLETED,
            LoopOutcome.STOPPED -> null
            LoopOutcome.FAILED -> AssistantUiError.UNKNOWN
            LoopOutcome.MAX_ITERATIONS -> AssistantUiError.MAX_ITERATIONS
        }

    private fun AssistantRuntimeEvent.Finished.normalizedAssistantError(): AssistantError? =
        error ?: if (outcome == LoopOutcome.FAILED) AssistantError.Unknown() else null

    private fun AssistantRuntimeEvent.Finished.canRetry(): Boolean =
        outcome == LoopOutcome.FAILED &&
            retryAffordance == RetryAffordance.Manual &&
            error?.supportsRetryAction() == true

    private fun AssistantRuntimeEvent.Finished.toTurnOutcome(): AiAssistantTurnOutcomeValue = when (outcome) {
        LoopOutcome.COMPLETED -> AiAssistantTurnOutcomeValue.Success
        LoopOutcome.FAILED -> AiAssistantTurnOutcomeValue.Failed
        LoopOutcome.STOPPED -> AiAssistantTurnOutcomeValue.CancelledByUser
        LoopOutcome.MAX_ITERATIONS -> AiAssistantTurnOutcomeValue.MaxIterations
    }

    private fun finalizeTurn(
        turn: ActiveTurn,
        outcome: AiAssistantTurnOutcomeValue,
        errorKind: AiAssistantErrorKindValue?,
        suppressLateRuntimeTelemetry: Boolean = outcome == AiAssistantTurnOutcomeValue.CancelledByUser,
    ) {
        if (turn.completedTracked) return
        turn.completedTracked = true
        if (suppressLateRuntimeTelemetry) {
            suppressedRuntimeTelemetryRequestIds += turn.context.requestId
        }
        assistantTelemetryTracker.track(
            AssistantTelemetryEventFactory.turnCompleted(
                context = turn.context,
                outcome = outcome,
                errorKind = errorKind,
                durationMs = (systemClock.nowMs() - turn.startedAtMs).coerceAtLeast(0L),
                isRetry = turn.isRetry,
                completionStack = AssistantConfig.COMPLETION_STACK,
                promptVersion = AssistantConfig.PROMPT_VERSION,
                toolCatalogVersion = AssistantConfig.TOOL_CATALOG_VERSION,
            )
        )
        if (activeTurn === turn) {
            activeTurn = null
        }
    }

    private fun List<AssistantUiMessage>.withoutRetryActions(): List<AssistantUiMessage> =
        map { message ->
            val error = message.error
            if (error?.canRetry == true) {
                message.copy(error = error.copy(canRetry = false))
            } else {
                message
            }
        }

    private fun List<AssistantUiMessage>.withoutTransientActivity(): List<AssistantUiMessage> =
        map { message ->
            message.copy(
                segments = message.segments.filterNot { segment ->
                    segment is AssistantUiSegment.ToolActivity &&
                        segment.activity.status == AssistantToolActivity.Status.RUNNING
                }
            )
        }

    private fun List<AssistantUiMessage>.withAssistantError(
        activeMessageId: String?,
        error: AssistantError?,
        canRetry: Boolean,
        nextId: () -> String,
    ): List<AssistantUiMessage> {
        if (error == null) return this

        val messageError = AssistantMessageError(error = error, canRetry = canRetry)
        val targetId = activeMessageId
        if (targetId == null) {
            return this + AssistantUiMessage(
                id = nextId(),
                role = AssistantUiMessage.Role.ASSISTANT,
                segments = listOf(AssistantUiSegment.Text("")),
                error = messageError,
            )
        }

        return map { message ->
            if (message.id == targetId) {
                message.copy(error = messageError)
            } else {
                message
            }
        }
    }

    private fun List<AssistantUiMessage>.withConfirmationCard(
        activeMessageId: String?,
        confirmation: AssistantConfirmationCard,
        nextId: () -> String,
    ): List<AssistantUiMessage> {
        val targetId = activeMessageId
        if (targetId == null) {
            return this + AssistantUiMessage(
                id = nextId(),
                role = AssistantUiMessage.Role.ASSISTANT,
                segments = listOf(
                    AssistantUiSegment.Text(""),
                    AssistantUiSegment.ConfirmationCard(confirmation),
                ),
            )
        }

        return map { message ->
            if (message.id == targetId) {
                message.appendConfirmationCard(confirmation)
            } else {
                message
            }
        }
    }

    private fun AssistantUiMessage.appendText(delta: String): AssistantUiMessage {
        val lastSegment = segments.lastOrNull()
        if (lastSegment !is AssistantUiSegment.Text) {
            return copy(segments = segments + AssistantUiSegment.Text(delta))
        }

        val updatedSegments = segments.toMutableList()
        updatedSegments[updatedSegments.lastIndex] = lastSegment.copy(text = lastSegment.text + delta)
        return copy(segments = updatedSegments)
    }

    private fun AssistantUiMessage.appendCardGroup(cards: List<AssistantCard>): AssistantUiMessage {
        val latestGroupIndex = segments.indexOfLast { it is AssistantUiSegment.CardGroup }
        val shouldMergeWithLatestGroup = latestGroupIndex != -1 &&
            segments.drop(latestGroupIndex + 1).none { it.breaksCardGrouping() }

        if (!shouldMergeWithLatestGroup) {
            return copy(segments = segments + AssistantUiSegment.CardGroup(cards))
        }

        val updatedSegments = segments.toMutableList()
        val latestGroup = updatedSegments[latestGroupIndex] as AssistantUiSegment.CardGroup
        updatedSegments[latestGroupIndex] = latestGroup.copy(cards = latestGroup.cards + cards)
        return copy(segments = updatedSegments)
    }

    private fun AssistantUiSegment.breaksCardGrouping(): Boolean = when (this) {
        is AssistantUiSegment.Text -> text.isNotEmpty()
        is AssistantUiSegment.ConfirmationCard -> true
        is AssistantUiSegment.CardGroup -> false
        is AssistantUiSegment.ToolActivity -> false
    }

    private fun AssistantUiMessage.withToolActivity(activity: AssistantToolActivity): AssistantUiMessage =
        copy(
            segments = segments.filterNot {
                it is AssistantUiSegment.ToolActivity && it.activity.toolCallId == activity.toolCallId
            } + AssistantUiSegment.ToolActivity(activity)
        )

    private fun List<AssistantUiMessage>.withToolActivityStatus(
        toolCallId: String,
        status: AssistantToolActivity.Status,
    ): List<AssistantUiMessage> =
        map { message ->
            message.copy(
                segments = message.segments.map { segment ->
                    if (segment is AssistantUiSegment.ToolActivity && segment.activity.toolCallId == toolCallId) {
                        AssistantUiSegment.ToolActivity(segment.activity.copy(status = status))
                    } else {
                        segment
                    }
                }
            )
        }

    private fun AssistantUiMessage.appendConfirmationCard(
        confirmation: AssistantConfirmationCard,
    ): AssistantUiMessage {
        val updatedSegments = segments.filterNot {
            it is AssistantUiSegment.ConfirmationCard &&
                it.model.confirmationId == confirmation.confirmationId
        } + AssistantUiSegment.ConfirmationCard(confirmation)
        return copy(segments = updatedSegments)
    }

    private fun List<AssistantUiMessage>.withUpdatedConfirmationCard(
        confirmationId: String,
        state: AssistantConfirmationCardState,
    ): List<AssistantUiMessage> = map { message ->
        message.copy(
            segments = message.segments.map { segment ->
                if (segment is AssistantUiSegment.ConfirmationCard &&
                    segment.model.confirmationId == confirmationId
                ) {
                    segment.copy(model = segment.model.copy(state = state))
                } else {
                    segment
                }
            }
        )
    }

    private fun ConfirmationDecision.toCardState(): AssistantConfirmationCardState = when (this) {
        ConfirmationDecision.CONFIRMED -> AssistantConfirmationCardState.CONFIRMED
        ConfirmationDecision.CANCELLED -> AssistantConfirmationCardState.CANCELLED
    }

    @AssistedFactory
    interface Factory {
        fun create(): AssistantViewModel
    }

    private data class ActiveTurn(
        val context: AssistantTelemetryContext,
        val isRetry: Boolean,
        val startedAtMs: Long,
        var completedTracked: Boolean = false,
    )
}
