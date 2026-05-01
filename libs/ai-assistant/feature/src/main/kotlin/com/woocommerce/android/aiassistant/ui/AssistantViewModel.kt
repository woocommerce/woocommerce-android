package com.woocommerce.android.aiassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.runtime.AssistantRuntime
import com.woocommerce.android.aiassistant.runtime.AssistantRuntimeConfirmationResult
import com.woocommerce.android.aiassistant.runtime.AssistantRuntimeEvent
import com.woocommerce.android.aiassistant.runtime.AssistantTurnRequest
import com.woocommerce.android.tools.SelectedSite
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AssistantViewModel.Factory::class)
class AssistantViewModel @AssistedInject constructor(
    @Assisted private val conversationId: String,
    private val runtime: AssistantRuntime,
    private val selectedSite: SelectedSite,
    private val idGenerator: AssistantMessageIdGenerator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var turnJob: Job? = null
    private var activeAssistantMessageId: String? = null
    private var history: List<AssistantMessage> = emptyList()
    private var lastTurnBaseHistory: List<AssistantMessage> = emptyList()
    private var lastUserMessage: String? = null

    fun onSendMessage(message: String) {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) return

        lastUserMessage = trimmedMessage
        startTurn(trimmedMessage, isRetry = false)
    }

    fun onRetry() {
        val message = lastUserMessage ?: return
        startTurn(message, isRetry = true)
    }

    fun onCancelTurn() {
        turnJob?.cancel()
        turnJob = null
        activeAssistantMessageId = null
        viewModelScope.launch {
            runtime.cancelTurn(conversationId)
        }
        _uiState.update {
            it.copy(
                status = AssistantUiStatus.IDLE,
                error = null,
                canRetry = false,
                pendingConfirmation = null,
            )
        }
    }

    fun onConfirmWrite() {
        val confirmationId = _uiState.value.pendingConfirmation?.id ?: return
        viewModelScope.launch {
            when (runtime.confirmWrite(confirmationId)) {
                AssistantRuntimeConfirmationResult.Accepted -> {
                    _uiState.update {
                        it.copy(
                            status = AssistantUiStatus.STREAMING,
                            error = null,
                            canRetry = false,
                            pendingConfirmation = null,
                        )
                    }
                }
                AssistantRuntimeConfirmationResult.Deferred -> {
                    activeAssistantMessageId = null
                    _uiState.update {
                        it.copy(
                            status = AssistantUiStatus.ERROR,
                            error = AssistantUiError.CONFIRMATION_DEFERRED,
                            canRetry = false,
                            pendingConfirmation = null,
                        )
                    }
                }
            }
        }
    }

    fun onCancelWrite() {
        val confirmationId = _uiState.value.pendingConfirmation?.id ?: return
        viewModelScope.launch {
            runtime.cancelWrite(confirmationId)
            _uiState.update {
                it.copy(
                    status = AssistantUiStatus.IDLE,
                    error = null,
                    canRetry = false,
                    pendingConfirmation = null,
                )
            }
        }
    }

    private fun startTurn(message: String, isRetry: Boolean) {
        turnJob?.cancel()
        if (!isRetry) {
            lastTurnBaseHistory = history
        }

        _uiState.update { state ->
            val userMessage = if (isRetry) {
                null
            } else {
                AssistantUiMessage(idGenerator.nextId(), AssistantUiMessage.Role.USER, message)
            }
            val assistantMessageId = idGenerator.nextId()
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
                messages = state.messages + turnMessages,
                status = AssistantUiStatus.STREAMING,
                error = null,
                canRetry = false,
                pendingConfirmation = null,
            )
        }

        val request = AssistantTurnRequest(
            conversationId = conversationId,
            siteId = selectedSite.get().siteId,
            toolScope = ToolScope.GLOBAL,
            userMessage = message,
            history = lastTurnBaseHistory,
        )
        val events = if (isRetry) runtime.retryTurn(request) else runtime.startTurn(request)
        turnJob = viewModelScope.launch {
            events.collect(::reduceRuntimeEvent)
        }
    }

    private fun reduceRuntimeEvent(event: AssistantRuntimeEvent) {
        when (event) {
            is AssistantRuntimeEvent.AssistantTextDelta -> appendAssistantText(event.text)
            is AssistantRuntimeEvent.AwaitingConfirmation -> {
                _uiState.update {
                    it.copy(
                        status = AssistantUiStatus.AWAITING_CONFIRMATION,
                        error = null,
                        canRetry = false,
                        pendingConfirmation = event.confirmation,
                    )
                }
            }
            is AssistantRuntimeEvent.Finished -> {
                activeAssistantMessageId = null
                history = event.updatedHistory
                _uiState.update {
                    it.copy(
                        status = event.outcome.toAssistantUiStatus(),
                        error = event.toAssistantUiError(),
                        canRetry = event.outcome == LoopOutcome.FAILED && event.retryAvailable,
                        pendingConfirmation = null,
                    )
                }
            }
        }
    }

    private fun appendAssistantText(delta: String) {
        val messageId = activeAssistantMessageId ?: return
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == messageId) {
                        message.copy(text = message.text + delta)
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

    private fun AssistantRuntimeEvent.Finished.toAssistantUiError(): AssistantUiError? = when (outcome) {
        LoopOutcome.COMPLETED,
        LoopOutcome.STOPPED -> null
        LoopOutcome.FAILED -> error?.toAssistantUiError() ?: AssistantUiError.UNKNOWN
        LoopOutcome.MAX_ITERATIONS -> error?.toAssistantUiError() ?: AssistantUiError.MAX_ITERATIONS
    }

    @AssistedFactory
    interface Factory {
        fun create(conversationId: String): AssistantViewModel
    }
}
