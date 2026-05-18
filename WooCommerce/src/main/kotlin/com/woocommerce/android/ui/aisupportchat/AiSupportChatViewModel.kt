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
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatSupportArea
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckCardData
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckType
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
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
    private val diagnosticsService: SupportDiagnosticsService,
    private val accountRepository: AccountRepository
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(
        AiSupportChatViewState(
            messages = initialMessages(),
            canPersistChatHistory = accountRepository.isUserLoggedIn()
        )
    )
    val viewState = _viewState.asStateFlow()

    private var localMessageId = 0L
    private var launchModeLoaded = false

    fun onLaunchModeLoaded(launchMode: AiSupportChatLaunchMode) {
        if (launchModeLoaded) return
        launchModeLoaded = true

        when (launchMode) {
            AiSupportChatLaunchMode.Help -> Unit
            AiSupportChatLaunchMode.PreLogin -> startFromPreLogin()
            is AiSupportChatLaunchMode.ConnectivityTool -> startFromConnectivityTool(launchMode.checks)
        }
    }

    fun onInputChanged(input: String) {
        _viewState.update { it.copy(input = input) }
    }

    fun onSendClicked() {
        val state = _viewState.value
        val message = state.input.trim()
        if (message.isBlank() || state.isSending || !state.canSendMessages) return

        launch { sendMessage(message) }
    }

    fun onSendErrorDismissed() {
        _viewState.update { it.copy(showSendError = false) }
    }

    fun onContactSupportClicked(source: HumanSupportContactSource) {
        val state = _viewState.value
        if (state.hasCreatedTicket || state.isSending) return

        if (source == HumanSupportContactSource.ERROR_DIALOG) {
            _viewState.update { it.copy(showSendError = false) }
        }
        triggerEvent(
            ContactHumanSupport(
                chatId = state.chatId,
                transcript = state.messages.toTranscript(draftUserMessage = state.input.takeIf { state.showSendError }),
                supportArea = state.latestSupportArea,
                source = source
            )
        )
    }

    fun onSupportTicketCreated() {
        _viewState.update {
            it.copy(
                input = "",
                hasCreatedTicket = true,
                showHumanSupportPrompt = false,
                showSendError = false
            )
        }
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

    private fun startFromPreLogin() {
        _viewState.update {
            it.copy(
                input = "",
                messages = listOf(greetingMessage()),
                hasStartedChat = true,
                hasProceededToChat = true,
                canPersistChatHistory = false,
                showSendError = false
            )
        }
    }

    private fun startFromConnectivityTool(checks: List<ConnectivityCheckCardData>) {
        val result = checks.toDiagnosticResult()
        _viewState.update {
            it.copy(
                input = "",
                messages = listOf(greetingMessage()),
                selectedIssueType = SupportIssueType.OTHER,
                diagnosticResult = result,
                hasProceededToChat = true,
                canPersistChatHistory = accountRepository.isUserLoggedIn(),
                showSendError = false
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
            botSlug = state.botSlug,
            message = message,
            context = context,
            chatId = chatId,
            sessionId = state.sessionId
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
            val latestSupportArea = response.messages.latestSupportArea() ?: it.latestSupportArea
            val shouldPromptHumanSupport = response.messages.shouldPromptHumanSupport()
            val completedUserMessageResponseCount = it.completedUserMessageResponseCount +
                if (response.messages.hasBotResponse()) 1 else 0
            it.copy(
                chatId = response.chatId,
                sessionId = response.sessionId,
                botSlug = response.botSlug,
                hasStartedChat = true,
                hasSentChatMessage = true,
                completedUserMessageResponseCount = completedUserMessageResponseCount,
                latestSupportArea = latestSupportArea,
                showHumanSupportPrompt = shouldPromptHumanSupport && !it.hasCreatedTicket,
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

        if (_viewState.value.canPersistChatHistory) {
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
    }

    private fun List<SupportChatMessage>.latestSupportArea(): SupportChatSupportArea? =
        asReversed()
            .firstOrNull { it.role == SupportChatRole.BOT && it.context?.supportArea != null }
            ?.context
            ?.supportArea

    private fun List<SupportChatMessage>.shouldPromptHumanSupport(): Boolean =
        any { it.role == SupportChatRole.BOT && it.context?.flags?.forwardToHumanSupport == true }

    private fun List<SupportChatMessage>.hasBotResponse(): Boolean =
        any { it.role == SupportChatRole.BOT }

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
        filterNot { it.isBotEscalationPrompt() }
            .map { message ->
                AiSupportChatMessage(
                    id = "${message.role.wireValue}-${message.messageId}",
                    role = message.role.toUiRole(),
                    content = AiSupportChatMessageContent.Text(message.content)
                )
            }

    private fun SupportChatMessage.isBotEscalationPrompt(): Boolean =
        role == SupportChatRole.BOT && context?.flags?.forwardToHumanSupport == true

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

    private fun diagnosticsMessages(
        result: DiagnosticResult,
        showFailureActions: Boolean = true
    ): List<AiSupportChatMessage> =
        listOf(
            greetingMessage(),
            AiSupportChatMessage(
                id = "diagnostics-${result.issueType.name}",
                role = AiSupportChatMessageRole.BOT,
                content = if (result.firstFailure == null || !showFailureActions) {
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

    private fun List<AiSupportChatMessage>.toTranscript(draftUserMessage: String? = null): String =
        buildList {
            addAll(this@toTranscript.filter { it.content !is AiSupportChatMessageContent.Greeting })
            draftUserMessage?.takeIf { it.isNotBlank() }?.let { draft ->
                add(
                    AiSupportChatMessage(
                        id = "draft-user-message",
                        role = AiSupportChatMessageRole.USER,
                        content = AiSupportChatMessageContent.Text(draft)
                    )
                )
            }
        }.toBoundedTranscript()

    private fun List<AiSupportChatMessage>.toBoundedTranscript(): String {
        val messages = takeLast(MAX_TRANSCRIPT_MESSAGES)
        return buildString {
            if (this@toBoundedTranscript.size > MAX_TRANSCRIPT_MESSAGES) {
                append("[Earlier messages trimmed]")
                append("\n\n")
            }
            append(
                messages.joinToString(separator = "\n\n") { message ->
                    val role = when (message.role) {
                        AiSupportChatMessageRole.USER -> "User"
                        AiSupportChatMessageRole.BOT -> "Bot"
                    }
                    "$role: ${message.transcriptText()}"
                }
            )
        }
    }

    private fun AiSupportChatMessage.transcriptText(): String =
        when (content) {
            AiSupportChatMessageContent.Greeting -> ""
            AiSupportChatMessageContent.IssuePicker -> "[Issue picker shown]"
            AiSupportChatMessageContent.PostDiagnosticsGreeting -> "Please describe your issue in more detail."
            is AiSupportChatMessageContent.Text -> when (role) {
                AiSupportChatMessageRole.USER -> content.text
                AiSupportChatMessageRole.BOT -> content.text.trimToFirstParagraph()
            }
            is AiSupportChatMessageContent.DiagnosticsProgress ->
                "[Diagnostics: ${content.result.statuses.toTranscriptText()}]"
            is AiSupportChatMessageContent.DiagnosticsFailure ->
                "[Diagnostics failed: ${content.result.statuses.toTranscriptText()}]"
        }

    private fun String.trimToFirstParagraph(): String {
        val paragraphs = trim().split(Regex("\\n\\s*\\n"))
        val firstParagraph = paragraphs.firstOrNull().orEmpty()
        return if (paragraphs.size > 1) {
            "$firstParagraph\n[AI response trimmed]"
        } else {
            firstParagraph
        }
    }

    private fun List<DiagnosticStatus>.toTranscriptText(): String =
        joinToString(separator = ", ") { status ->
            "${status.test.name}: ${status.status::class.java.simpleName}"
        }

    companion object {
        const val DEFAULT_BOT_SLUG = "woo-workflow-support_mobile_inapp_all_users"

        private const val POST_DIAGNOSTICS_GREETING_MESSAGE_ID = "post-diagnostics-greeting"
        private const val MAX_TRANSCRIPT_MESSAGES = 20
    }
}

data class AiSupportChatViewState(
    val input: String = "",
    val messages: List<AiSupportChatMessage> = emptyList(),
    val chatId: Long? = null,
    val sessionId: String? = null,
    val botSlug: String = AiSupportChatViewModel.DEFAULT_BOT_SLUG,
    val hasProceededToChat: Boolean = false,
    val hasStartedChat: Boolean = false,
    val selectedIssueType: SupportIssueType? = null,
    val selectedIssueLabel: String? = null,
    val diagnosticResult: DiagnosticResult? = null,
    val isRunningDiagnostics: Boolean = false,
    val isSending: Boolean = false,
    val canPersistChatHistory: Boolean = true,
    val showSendError: Boolean = false,
    val showHumanSupportPrompt: Boolean = false,
    val hasCreatedTicket: Boolean = false,
    val hasSentChatMessage: Boolean = false,
    val completedUserMessageResponseCount: Int = 0,
    val latestSupportArea: SupportChatSupportArea? = null
) {
    val canUseDiagnosticActions: Boolean
        get() = !hasProceededToChat && !isSending

    val canSendMessages: Boolean
        get() = hasProceededToChat && !hasCreatedTicket

    val showDiagnosticActions: Boolean
        get() {
            val result = diagnosticResult ?: return false
            return canUseDiagnosticActions && (result.firstFailure != null || result.isComplete)
        }

    val canContactHumanSupportFromToolbar: Boolean
        get() = canSendMessages && completedUserMessageResponseCount >= MIN_USER_MESSAGE_RESPONSES_FOR_TOOLBAR

    private companion object {
        const val MIN_USER_MESSAGE_RESPONSES_FOR_TOOLBAR = 2
    }
}

enum class HumanSupportContactSource {
    TOOLBAR,
    BANNER,
    ERROR_DIALOG
}

data class ContactHumanSupport(
    val chatId: Long?,
    val transcript: String,
    val supportArea: SupportChatSupportArea?,
    val source: HumanSupportContactSource
) : Event()

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

private fun List<ConnectivityCheckCardData>.toDiagnosticResult(): DiagnosticResult =
    DiagnosticResult(
        issueType = SupportIssueType.OTHER,
        statuses = filter { it.status.isComplete }
            .map { check ->
                DiagnosticStatus(
                    test = check.type.toDiagnosticTest(),
                    status = check.status.toTestStatus()
                )
            }
    )

private val ConnectivityCheckStatus.isComplete: Boolean
    get() = this is ConnectivityCheckStatus.Success || this is ConnectivityCheckStatus.Failure

private fun ConnectivityCheckType.toDiagnosticTest(): DiagnosticTest =
    when (this) {
        ConnectivityCheckType.INTERNET -> DiagnosticTest.INTERNET_CONNECTION
        ConnectivityCheckType.WP_COM -> DiagnosticTest.WPCOM_SERVERS
        ConnectivityCheckType.STORE -> DiagnosticTest.STORE_CONNECTION
        ConnectivityCheckType.ORDERS -> DiagnosticTest.STORE_ORDERS
        ConnectivityCheckType.PRODUCTS -> DiagnosticTest.STORE_PRODUCTS
    }

private fun ConnectivityCheckStatus.toTestStatus(): TestStatus =
    when (this) {
        ConnectivityCheckStatus.NotStarted -> TestStatus.Pending
        ConnectivityCheckStatus.InProgress -> TestStatus.Running
        is ConnectivityCheckStatus.Success -> TestStatus.Passed
        is ConnectivityCheckStatus.Failure -> TestStatus.Failed(
            failureType = error,
            technicalDetails = technicalDetails,
            durationMs = durationMs
        )
    }
