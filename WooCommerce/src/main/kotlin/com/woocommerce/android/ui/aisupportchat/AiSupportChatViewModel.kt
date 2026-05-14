package com.woocommerce.android.ui.aisupportchat

import androidx.lifecycle.SavedStateHandle
import com.google.gson.JsonObject
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticResult
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticStatus
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportDiagnosticsService
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportIssueType
import com.woocommerce.android.ui.aisupportchat.diagnostics.TestStatus
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatMessage
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatRole
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSupportChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SupportChatRepository,
    private val contextProvider: SupportChatContextProvider,
    private val diagnosticsService: SupportDiagnosticsService
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(AiSupportChatViewState(messages = initialMessages()))
    val viewState = _viewState.asStateFlow()

    private var localMessageId = 0L

    fun onInputChanged(input: String) {
        _viewState.update { it.copy(input = input) }
    }

    fun onSendClicked() {
        val state = _viewState.value
        val message = state.input.trim()
        if (message.isBlank() || state.isSending || !state.hasProceededToChat) return

        launch { sendMessage(message) }
    }

    fun onIssueSelected(issueType: SupportIssueType, issueLabel: String) {
        val state = _viewState.value
        if (state.hasProceededToChat || state.isSending || state.isRunningDiagnostics) return

        _viewState.update {
            it.copy(
                input = "",
                selectedIssueType = issueType,
                selectedIssueLabel = issueLabel,
                isRunningDiagnostics = true,
                showSendError = false
            )
        }

        launch {
            diagnosticsService.runDiagnostics(issueType)
                .catch { error ->
                    if (error is CancellationException) throw error
                    handleDiagnosticsFailure(issueType, issueLabel, error)
                }
                .collect { result ->
                    val hasFailure = result.firstFailure != null
                    _viewState.update {
                        it.copy(
                            input = "",
                            selectedIssueType = issueType,
                            selectedIssueLabel = issueLabel,
                            diagnosticResult = result,
                            isRunningDiagnostics = !result.isComplete && !hasFailure,
                            showSendError = false,
                            messages = diagnosticsMessages(result)
                        )
                    }
                }
        }
    }

    fun onRetryDiagnosticsClicked() {
        val state = _viewState.value
        val issueType = state.selectedIssueType ?: return
        val issueLabel = state.selectedIssueLabel ?: return
        onIssueSelected(issueType, issueLabel)
    }

    fun onContinueAfterDiagnosticsClicked() {
        val state = _viewState.value
        if (state.hasProceededToChat || state.isSending || state.isRunningDiagnostics) return

        _viewState.update {
            it.copy(
                input = "",
                hasProceededToChat = true,
                isRunningDiagnostics = false,
                showSendError = false,
                messages = it.messages.appendPostDiagnosticsGreeting()
            )
        }
    }

    private fun handleDiagnosticsFailure(issueType: SupportIssueType, issueLabel: String, error: Throwable) {
        val result = DiagnosticResult(
            issueType = issueType,
            statuses = listOf(
                DiagnosticStatus(
                    test = DiagnosticTest.INTERNET_CONNECTION,
                    status = TestStatus.Failed(
                        technicalDetails = error.message ?: error::class.java.simpleName
                    )
                )
            )
        )
        _viewState.update {
            it.copy(
                input = "",
                selectedIssueType = issueType,
                selectedIssueLabel = issueLabel,
                diagnosticResult = result,
                isRunningDiagnostics = false,
                showSendError = false,
                messages = diagnosticsMessages(result)
            )
        }
    }

    private suspend fun sendMessage(message: String) {
        val state = _viewState.value
        if (message.isBlank() || state.isSending) return

        val optimisticMessage = AiSupportChatMessage(
            id = nextLocalMessageId(),
            role = AiSupportChatMessageRole.USER,
            content = AiSupportChatMessageContent.Text(message)
        )
        _viewState.update {
            it.copy(
                input = "",
                messages = it.messages + optimisticMessage,
                isSending = true,
                showSendError = false
            )
        }

        val chatId = state.chatId
        val context = if (chatId == null) {
            contextProvider.buildInitialContext(diagnosticResult = state.diagnosticResult)
        } else {
            JsonObject()
        }

        repository.sendMessage(
            botSlug = DEFAULT_BOT_SLUG,
            message = message,
            context = context,
            chatId = chatId
        ).onSuccess { response ->
            handleSendSuccess(
                response = response,
                sentMessage = message,
                optimisticMessage = optimisticMessage,
                wasInitialMessage = chatId == null
            )
        }.onFailure { error ->
            handleSendFailure(message = message, optimisticMessage = optimisticMessage, error = error)
        }
    }

    private suspend fun handleSendSuccess(
        response: SupportChatResponse,
        sentMessage: String,
        optimisticMessage: AiSupportChatMessage,
        wasInitialMessage: Boolean
    ) {
        _viewState.update {
            val remoteMessages = response.messages.toUiMessages()
            it.copy(
                chatId = response.chatId,
                hasStartedChat = true,
                messages = if (remoteMessages.isEmpty()) {
                    it.messages
                } else {
                    it.messages.supportMessages() + it.messages.threadMessages().mergeWithRemoteMessages(
                        remoteMessages = remoteMessages,
                        optimisticMessage = optimisticMessage
                    )
                },
                isSending = false,
                showSendError = false
            )
        }

        runCatching {
            if (wasInitialMessage) {
                repository.registerChat(
                    chatId = response.chatId,
                    botSlug = response.botSlug,
                    firstUserMessage = sentMessage
                )
            } else {
                repository.markChatAsUpdated(response.chatId)
            }
        }
    }

    private fun List<AiSupportChatMessage>.mergeWithRemoteMessages(
        remoteMessages: List<AiSupportChatMessage>,
        optimisticMessage: AiSupportChatMessage
    ): List<AiSupportChatMessage> {
        if (remoteMessages.isEmpty()) return this

        val optimisticText = optimisticMessage.content as? AiSupportChatMessageContent.Text
        val messagesWithoutRemoteOptimisticDuplicate = if (
            optimisticText != null && remoteMessages.containsUserMessage(optimisticText.text)
        ) {
            filterNot { it.id == optimisticMessage.id }
        } else {
            this
        }
        val existingMessageIds = messagesWithoutRemoteOptimisticDuplicate.map { it.id }.toSet()
        return messagesWithoutRemoteOptimisticDuplicate + remoteMessages.filterNot { it.id in existingMessageIds }
    }

    private fun List<AiSupportChatMessage>.containsUserMessage(text: String): Boolean =
        any { message ->
            message.role == AiSupportChatMessageRole.USER &&
                (message.content as? AiSupportChatMessageContent.Text)?.text == text
        }

    private fun handleSendFailure(message: String, optimisticMessage: AiSupportChatMessage, error: Throwable) {
        WooLog.e(WooLog.T.AI, "Sending AI support chat message failed", error)

        _viewState.update {
            it.copy(
                input = message,
                messages = it.messages - optimisticMessage,
                isSending = false,
                showSendError = true
            )
        }
    }

    private fun List<SupportChatMessage>.toUiMessages(): List<AiSupportChatMessage> =
        map { message ->
            AiSupportChatMessage(
                id = "${message.role.wireValue}-${message.messageId}",
                role = message.role.toUiRole(),
                content = AiSupportChatMessageContent.Text(message.content)
            )
        }

    private fun List<AiSupportChatMessage>.supportMessages(): List<AiSupportChatMessage> =
        filter { message ->
            when (message.content) {
                AiSupportChatMessageContent.Greeting,
                AiSupportChatMessageContent.PostDiagnosticsGreeting,
                is AiSupportChatMessageContent.DiagnosticsFailure,
                is AiSupportChatMessageContent.DiagnosticsProgress -> true
                AiSupportChatMessageContent.IssuePicker,
                is AiSupportChatMessageContent.Text -> false
            }
        }

    private fun List<AiSupportChatMessage>.threadMessages(): List<AiSupportChatMessage> =
        filter { it.content is AiSupportChatMessageContent.Text }

    private fun List<AiSupportChatMessage>.appendPostDiagnosticsGreeting(): List<AiSupportChatMessage> =
        if (any { it.id == POST_DIAGNOSTICS_GREETING_MESSAGE_ID }) {
            this
        } else {
            this + AiSupportChatMessage(
                id = POST_DIAGNOSTICS_GREETING_MESSAGE_ID,
                role = AiSupportChatMessageRole.BOT,
                content = AiSupportChatMessageContent.PostDiagnosticsGreeting
            )
        }

    private fun diagnosticsMessages(result: DiagnosticResult): List<AiSupportChatMessage> =
        listOf(
            greetingMessage(),
            AiSupportChatMessage(
                id = "diagnostics-${result.issueType.name}",
                role = AiSupportChatMessageRole.BOT,
                content = if (result.firstFailure == null) {
                    AiSupportChatMessageContent.DiagnosticsProgress(result)
                } else {
                    AiSupportChatMessageContent.DiagnosticsFailure(result)
                }
            )
        )

    private fun SupportChatRole.toUiRole(): AiSupportChatMessageRole =
        when (this) {
            SupportChatRole.USER -> AiSupportChatMessageRole.USER
            SupportChatRole.BOT, SupportChatRole.UNKNOWN -> AiSupportChatMessageRole.BOT
        }

    private fun nextLocalMessageId(): String {
        localMessageId += 1
        return "local-$localMessageId"
    }

    companion object {
        const val DEFAULT_BOT_SLUG = "woo-workflow-support_mobile_inapp_all_users"

        private const val POST_DIAGNOSTICS_GREETING_MESSAGE_ID = "post-diagnostics-greeting"
    }
}

data class AiSupportChatViewState(
    val input: String = "",
    val messages: List<AiSupportChatMessage> = emptyList(),
    val chatId: Long? = null,
    val hasProceededToChat: Boolean = false,
    val hasStartedChat: Boolean = false,
    val selectedIssueType: SupportIssueType? = null,
    val selectedIssueLabel: String? = null,
    val diagnosticResult: DiagnosticResult? = null,
    val isRunningDiagnostics: Boolean = false,
    val isSending: Boolean = false,
    val showSendError: Boolean = false
) {
    val canUseDiagnosticActions: Boolean
        get() = !hasProceededToChat && !isSending

    val showDiagnosticActions: Boolean
        get() {
            val result = diagnosticResult ?: return false
            return canUseDiagnosticActions && (result.firstFailure != null || result.isComplete)
        }
}

data class AiSupportChatMessage(
    val id: String,
    val role: AiSupportChatMessageRole,
    val content: AiSupportChatMessageContent
)

enum class AiSupportChatMessageRole {
    USER,
    BOT
}

sealed interface AiSupportChatMessageContent {
    data object Greeting : AiSupportChatMessageContent
    data object IssuePicker : AiSupportChatMessageContent
    data object PostDiagnosticsGreeting : AiSupportChatMessageContent
    data class Text(val text: String) : AiSupportChatMessageContent
    data class DiagnosticsProgress(val result: DiagnosticResult) : AiSupportChatMessageContent
    data class DiagnosticsFailure(val result: DiagnosticResult) : AiSupportChatMessageContent
}

private fun initialMessages(): List<AiSupportChatMessage> =
    listOf(
        greetingMessage(),
        AiSupportChatMessage(
            id = "issue-picker",
            role = AiSupportChatMessageRole.BOT,
            content = AiSupportChatMessageContent.IssuePicker
        )
    )

private fun greetingMessage(): AiSupportChatMessage =
    AiSupportChatMessage(
        id = "greeting",
        role = AiSupportChatMessageRole.BOT,
        content = AiSupportChatMessageContent.Greeting
    )
