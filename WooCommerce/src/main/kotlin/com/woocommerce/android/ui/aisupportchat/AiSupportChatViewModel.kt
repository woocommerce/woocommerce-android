package com.woocommerce.android.ui.aisupportchat

import androidx.lifecycle.SavedStateHandle
import com.google.gson.JsonObject
import com.woocommerce.android.R
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticResult
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticStatus
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportDiagnosticsService
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportIssueType
import com.woocommerce.android.ui.aisupportchat.diagnostics.TestStatus
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportAreaType
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

@Suppress("LargeClass")
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
    private var resumeLaunchMode: AiSupportChatLaunchMode.Resume? = null

    fun onLaunchModeLoaded(launchMode: AiSupportChatLaunchMode) {
        if (launchModeLoaded) return
        launchModeLoaded = true

        when (launchMode) {
            AiSupportChatLaunchMode.Help -> Unit
            AiSupportChatLaunchMode.PreLogin -> startFromPreLogin()
            is AiSupportChatLaunchMode.ConnectivityTool -> startFromConnectivityTool(launchMode.checks)
            is AiSupportChatLaunchMode.Resume -> resumeChat(launchMode)
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

    fun onRetryLoadHistoryClicked() {
        resumeLaunchMode?.let(::resumeChat)
    }

    fun onFeedbackClicked(messageId: Long, rating: AiSupportChatFeedbackRating) {
        val state = _viewState.value
        val chatId = state.chatId ?: return
        val sessionId = state.sessionId ?: return
        if (messageId in state.messageRatings) return

        _viewState.update {
            val updatedRatings = it.messageRatings + (messageId to rating)
            it.copy(
                messageRatings = updatedRatings,
                messages = if (
                    rating == AiSupportChatFeedbackRating.UP &&
                    it.messages.latestBotResponse?.messageId == messageId
                ) {
                    it.messages.appendResolvedPromptIfNeeded()
                } else {
                    it.messages
                }
            )
        }

        launch {
            repository.submitFeedback(
                botSlug = state.botSlug,
                chatId = chatId,
                messageId = messageId,
                sessionId = sessionId,
                upvoted = rating == AiSupportChatFeedbackRating.UP
            ).onFailure { error ->
                WooLog.e(WooLog.T.AI, "Submitting AI support chat feedback failed", error)
            }
        }
    }

    fun onMarkResolvedClicked() {
        _viewState.update { it.copy(showMarkResolvedConfirmation = true) }
    }

    fun onMarkResolvedDismissed() {
        _viewState.update { it.copy(showMarkResolvedConfirmation = false) }
    }

    fun onMarkResolvedConfirmed() {
        val chatId = _viewState.value.chatId
        _viewState.update {
            it.copy(
                isChatResolved = true,
                showMarkResolvedConfirmation = false,
                showHumanSupportPrompt = false,
                showSendError = false,
                input = ""
            )
        }

        if (chatId != null) {
            launch {
                runCatching {
                    repository.markChatAsResolved(chatId)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    WooLog.e(WooLog.T.AI, "Marking AI support chat as resolved failed", error)
                }
            }
        }
    }

    fun onContactSupportClicked(source: HumanSupportContactSource, canCreateTicketDirectly: Boolean) {
        val state = _viewState.value
        if (state.hasCreatedTicket || state.isSending) return

        if (source == HumanSupportContactSource.ERROR_DIALOG) {
            _viewState.update { it.copy(showSendError = false) }
        }
        triggerEvent(
            createContactHumanSupportEvent(
                chatId = state.chatId,
                transcript = state.messages.toTranscript(draftUserMessage = state.input.takeIf { state.showSendError }),
                source = source,
                supportArea = state.latestSupportArea,
                canCreateTicketDirectly = canCreateTicketDirectly
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

    private fun resumeChat(launchMode: AiSupportChatLaunchMode.Resume) {
        resumeLaunchMode = launchMode
        _viewState.update {
            it.copy(
                input = "",
                messages = emptyList(),
                chatId = launchMode.chatId,
                sessionId = launchMode.sessionId,
                botSlug = launchMode.botSlug,
                hasProceededToChat = true,
                hasStartedChat = true,
                isChatResolved = launchMode.isResolved,
                isLoadingHistory = true,
                showSendError = false,
                showLoadHistoryError = false
            )
        }

        launch {
            repository.fetchChat(
                botSlug = launchMode.botSlug,
                chatId = launchMode.chatId,
                sessionId = launchMode.sessionId
            ).onSuccess { response ->
                handleResumeSuccess(response)
            }.onFailure { error ->
                WooLog.e(WooLog.T.AI, "Fetching AI support chat history failed", error)
                _viewState.update {
                    it.copy(
                        isLoadingHistory = false,
                        showLoadHistoryError = true
                    )
                }
            }
        }
    }

    private suspend fun handleResumeSuccess(response: SupportChatResponse) {
        _viewState.update {
            val remoteMessages = response.messages.toUiMessages(isNewInSession = false)
            val shouldPromptHumanSupport = response.messages.shouldPromptHumanSupport()
            it.copy(
                chatId = response.chatId,
                sessionId = response.sessionId,
                botSlug = response.botSlug,
                hasSentChatMessage = response.messages.any { message -> message.role == SupportChatRole.USER },
                completedUserMessageResponseCount = response.messages.count { message ->
                    message.role == SupportChatRole.BOT && !message.isBotEscalationPrompt()
                },
                latestSupportArea = response.messages.latestSupportArea() ?: it.latestSupportArea,
                showHumanSupportPrompt = shouldPromptHumanSupport && !it.hasCreatedTicket,
                messages = remoteMessages.toLoadedChatMessages(shouldPromptHumanSupport),
                isLoadingHistory = false,
                showSendError = false,
                showLoadHistoryError = false
            )
        }
        markChatAsUpdated(response.chatId, response.sessionId)
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
        if (message.isBlank() || state.isSending || !state.canSendMessages) return

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
            val remoteMessages = response.messages.toUiMessages(isNewInSession = true)
            val latestSupportArea = response.messages.latestSupportArea() ?: it.latestSupportArea
            val shouldPromptHumanSupport = response.messages.shouldPromptHumanSupport()
            val completedUserMessageResponseCount = it.completedUserMessageResponseCount +
                if (response.messages.hasBotResponse()) 1 else 0
            val messages = if (remoteMessages.isEmpty()) {
                it.messages
            } else {
                it.messages.supportMessages() + it.messages.threadMessages().mergeWithRemoteMessages(
                    remoteMessages = remoteMessages,
                    optimisticMessage = optimisticMessage
                )
            }.let { messages ->
                if (!shouldPromptHumanSupport && messages.latestBotResponse?.isResolved == true) {
                    messages.appendResolvedPromptIfNeeded()
                } else {
                    messages
                }
            }
            it.copy(
                chatId = response.chatId,
                sessionId = response.sessionId,
                botSlug = response.botSlug,
                hasStartedChat = true,
                hasSentChatMessage = true,
                completedUserMessageResponseCount = completedUserMessageResponseCount,
                latestSupportArea = latestSupportArea,
                showHumanSupportPrompt = shouldPromptHumanSupport && !it.hasCreatedTicket,
                messages = messages,
                isSending = false,
                showSendError = false
            )
        }

        if (_viewState.value.canPersistChatHistory) {
            if (wasInitialMessage) {
                runCatching {
                    repository.registerChat(
                        chatId = response.chatId,
                        botSlug = response.botSlug,
                        sessionId = response.sessionId,
                        firstUserMessage = sentMessage
                    )
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    WooLog.e(WooLog.T.AI, "Registering AI support chat failed", error)
                }
            } else {
                markChatAsUpdated(response.chatId, response.sessionId)
            }
        }
    }

    private suspend fun markChatAsUpdated(chatId: Long, sessionId: String?) {
        runCatching {
            repository.markChatAsUpdated(chatId, sessionId)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            WooLog.e(WooLog.T.AI, "Marking AI support chat as updated failed", error)
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

    private fun List<SupportChatMessage>.toUiMessages(isNewInSession: Boolean): List<AiSupportChatMessage> =
        filterNot { it.isBotEscalationPrompt() }
            .map { message ->
                AiSupportChatMessage(
                    id = "${message.role.wireValue}-${message.messageId}",
                    messageId = if (message.role == SupportChatRole.BOT) message.messageId else null,
                    role = message.role.toUiRole(),
                    isResolved = message.context?.isResolved == true,
                    isNewInSession = isNewInSession,
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
                AiSupportChatMessageContent.ResolvedPrompt,
                is AiSupportChatMessageContent.Text -> false
            }
        }

    private fun List<AiSupportChatMessage>.threadMessages(): List<AiSupportChatMessage> =
        filter {
            it.content is AiSupportChatMessageContent.Text ||
                it.content == AiSupportChatMessageContent.ResolvedPrompt
        }

    private val List<AiSupportChatMessage>.latestBotResponse: AiSupportChatMessage?
        get() = lastOrNull { it.role == AiSupportChatMessageRole.BOT && it.messageId != null }

    private fun List<AiSupportChatMessage>.appendResolvedPromptIfNeeded(): List<AiSupportChatMessage> {
        val messagesWithoutPrompt = filterNot { it.content == AiSupportChatMessageContent.ResolvedPrompt }
        val latestBotResponseIndex = messagesWithoutPrompt.indexOfLast {
            it.role == AiSupportChatMessageRole.BOT && it.messageId != null
        }
        if (latestBotResponseIndex == -1) return messagesWithoutPrompt

        val resolvedPrompt = AiSupportChatMessage(
            id = RESOLVED_PROMPT_MESSAGE_ID,
            role = AiSupportChatMessageRole.BOT,
            content = AiSupportChatMessageContent.ResolvedPrompt
        )
        return messagesWithoutPrompt.toMutableList().apply {
            add(latestBotResponseIndex + 1, resolvedPrompt)
        }
    }

    private fun List<AiSupportChatMessage>.toLoadedChatMessages(
        shouldPromptHumanSupport: Boolean
    ): List<AiSupportChatMessage> =
        if (!shouldPromptHumanSupport && latestBotResponse?.isResolved == true) {
            appendResolvedPromptIfNeeded()
        } else {
            this
        }

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
            AiSupportChatMessageContent.ResolvedPrompt ->
                "Please mark the chat as resolved if your problem is resolved, " +
                    "or leave a message if you have other questions."
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

    private fun createContactHumanSupportEvent(
        chatId: Long?,
        transcript: String,
        source: HumanSupportContactSource,
        supportArea: SupportChatSupportArea?,
        canCreateTicketDirectly: Boolean
    ): ContactHumanSupport {
        val mode = if (supportArea != null && supportArea.isHighConfidence && canCreateTicketDirectly) {
            HumanSupportContactMode.DIRECT_CREATE
        } else {
            HumanSupportContactMode.OPEN_FORM
        }
        return ContactHumanSupport(
            chatId = chatId,
            transcript = transcript,
            source = source,
            mode = mode,
            ticketType = supportArea?.ticketType,
            subjectResId = supportArea?.subjectResId,
            extraTags = supportArea.extraTags()
        )
    }

    private fun SupportChatSupportArea?.extraTags(): List<String> =
        buildList {
            add(SOURCE_TAG)
            add(AI_SKIP_TAG)
            this@extraTags?.topic?.takeIf { it.isNotBlank() }?.let { add(it) }
        }

    private val SupportChatSupportArea.ticketType: TicketType
        get() = when (areaType) {
            SupportAreaType.MOBILE_APP -> TicketType.MobileApp
            SupportAreaType.CARD_READER -> TicketType.InPersonPayments
            SupportAreaType.WOO_PAYMENTS -> TicketType.Payments
            SupportAreaType.WOO_COMMERCE_PLUGIN -> TicketType.WooPlugin
            SupportAreaType.OTHER_EXTENSION_PLUGIN -> TicketType.OtherPlugins
        }

    private val SupportChatSupportArea.subjectResId: Int
        get() = when (areaType) {
            SupportAreaType.MOBILE_APP -> R.string.ai_support_chat_support_request_subject_mobile_app
            SupportAreaType.CARD_READER -> R.string.ai_support_chat_support_request_subject_card_reader
            SupportAreaType.WOO_PAYMENTS -> R.string.ai_support_chat_support_request_subject_woo_payments
            SupportAreaType.WOO_COMMERCE_PLUGIN -> R.string.ai_support_chat_support_request_subject_woo_plugin
            SupportAreaType.OTHER_EXTENSION_PLUGIN -> R.string.ai_support_chat_support_request_subject_other_plugin
        }

    companion object {
        const val DEFAULT_BOT_SLUG = "woo-workflow-support_mobile_inapp_all_users"

        private const val POST_DIAGNOSTICS_GREETING_MESSAGE_ID = "post-diagnostics-greeting"
        private const val RESOLVED_PROMPT_MESSAGE_ID = "resolved-prompt"
        private const val MAX_TRANSCRIPT_MESSAGES = 20
        private const val SOURCE_TAG = "in_app_support_escalate"
        private const val AI_SKIP_TAG = "ai_skip"
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
    val isLoadingHistory: Boolean = false,
    val isSending: Boolean = false,
    val canPersistChatHistory: Boolean = true,
    val showSendError: Boolean = false,
    val showLoadHistoryError: Boolean = false,
    val showHumanSupportPrompt: Boolean = false,
    val hasCreatedTicket: Boolean = false,
    val isChatResolved: Boolean = false,
    val showMarkResolvedConfirmation: Boolean = false,
    val hasSentChatMessage: Boolean = false,
    val completedUserMessageResponseCount: Int = 0,
    val latestSupportArea: SupportChatSupportArea? = null,
    val messageRatings: Map<Long, AiSupportChatFeedbackRating> = emptyMap()
) {
    val canUseDiagnosticActions: Boolean
        get() = !hasProceededToChat && !isSending

    val canSendMessages: Boolean
        get() = hasProceededToChat &&
            !isLoadingHistory &&
            !showLoadHistoryError &&
            !hasCreatedTicket &&
            !isChatResolved

    val showInputBar: Boolean
        get() = canSendMessages && !showHumanSupportPrompt

    val showDiagnosticActions: Boolean
        get() {
            val result = diagnosticResult ?: return false
            return canUseDiagnosticActions && (result.firstFailure != null || result.isComplete)
        }

    val canContactHumanSupportFromToolbar: Boolean
        get() = canSendMessages && completedUserMessageResponseCount >= MIN_USER_MESSAGE_RESPONSES_FOR_TOOLBAR

    val shouldShowResolvedButton: Boolean
        get() {
            if (!canSendMessages || showHumanSupportPrompt) return false
            val botResponses = messages.filter {
                it.role == AiSupportChatMessageRole.BOT && it.messageId != null
            }
            val latestBotResponse = botResponses.lastOrNull() ?: return false
            val latestResponseResolved = latestBotResponse.isResolved
            val latestResponseUpvoted = messageRatings[latestBotResponse.messageId] == AiSupportChatFeedbackRating.UP
            val hasEnoughBotResponses = botResponses.size >= MIN_BOT_RESPONSES_FOR_RESOLUTION_ACTION
            return latestResponseResolved || latestResponseUpvoted || hasEnoughBotResponses
        }

    private companion object {
        const val MIN_USER_MESSAGE_RESPONSES_FOR_TOOLBAR = 2
        const val MIN_BOT_RESPONSES_FOR_RESOLUTION_ACTION = 2
    }
}

enum class HumanSupportContactSource {
    TOOLBAR,
    BANNER,
    ERROR_DIALOG
}

enum class AiSupportChatFeedbackRating {
    UP,
    DOWN
}

enum class HumanSupportContactMode {
    DIRECT_CREATE,
    OPEN_FORM
}

data class ContactHumanSupport(
    val chatId: Long?,
    val transcript: String,
    val source: HumanSupportContactSource,
    val mode: HumanSupportContactMode,
    val ticketType: TicketType?,
    val subjectResId: Int?,
    val extraTags: List<String>
) : Event()

data class AiSupportChatMessage(
    val id: String,
    val messageId: Long? = null,
    val role: AiSupportChatMessageRole,
    val isResolved: Boolean = false,
    val isNewInSession: Boolean = false,
    val content: AiSupportChatMessageContent
) {
    val shouldShowFeedback: Boolean
        get() = role == AiSupportChatMessageRole.BOT &&
            isNewInSession &&
            !isResolved &&
            messageId != null &&
            content is AiSupportChatMessageContent.Text
}

enum class AiSupportChatMessageRole {
    USER,
    BOT
}

sealed interface AiSupportChatMessageContent {
    data object Greeting : AiSupportChatMessageContent
    data object IssuePicker : AiSupportChatMessageContent
    data object PostDiagnosticsGreeting : AiSupportChatMessageContent
    data object ResolvedPrompt : AiSupportChatMessageContent
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
