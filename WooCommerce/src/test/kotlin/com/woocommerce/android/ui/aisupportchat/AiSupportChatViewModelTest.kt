package com.woocommerce.android.ui.aisupportchat

import androidx.lifecycle.SavedStateHandle
import com.google.gson.JsonObject
import com.woocommerce.android.ui.aisupportchat.AiSupportChatViewModel.Companion.DEFAULT_BOT_SLUG
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticResult
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticStatus
import com.woocommerce.android.ui.aisupportchat.diagnostics.DiagnosticTest
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportDiagnosticsService
import com.woocommerce.android.ui.aisupportchat.diagnostics.SupportIssueType
import com.woocommerce.android.ui.aisupportchat.diagnostics.TestStatus
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatFlags
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatMessage
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatMessageContext
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatRole
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatSupportArea
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckCardData
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckType
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AiSupportChatViewModelTest : BaseUnitTest() {
    private val repository: SupportChatRepository = mock()
    private val contextProvider: SupportChatContextProvider = mock()
    private val diagnosticsService: SupportDiagnosticsService = mock()
    private val accountRepository: AccountRepository = mock()

    private lateinit var viewModel: AiSupportChatViewModel

    @Before
    fun setUp() {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        createViewModel()
    }

    private fun createViewModel() {
        viewModel = AiSupportChatViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            contextProvider = contextProvider,
            diagnosticsService = diagnosticsService,
            accountRepository = accountRepository
        )
    }

    @Test
    fun `when initialized, then greeting and issue picker are shown`() {
        val state = viewModel.viewState.value

        assertThat(state.hasProceededToChat).isFalse()
        assertThat(state.hasStartedChat).isFalse()
        assertThat(state.canUseDiagnosticActions).isTrue()
        assertThat(state.showInputBar).isFalse()
        assertThat(state.showDiagnosticActions).isFalse()
        assertThat(state.messages.map { it.content }).containsExactly(
            AiSupportChatMessageContent.Greeting,
            AiSupportChatMessageContent.IssuePicker
        )
    }

    @Test
    fun `given pre-login launch mode, when loaded, then chat starts without issue picker`() =
        testBlocking {
            viewModel.onInputChanged(FOLLOW_UP_MESSAGE)

            viewModel.onLaunchModeLoaded(AiSupportChatLaunchMode.PreLogin)

            val state = viewModel.viewState.value
            assertThat(state.input).isEmpty()
            assertThat(state.hasStartedChat).isTrue()
            assertThat(state.showSendError).isFalse()
            assertThat(state.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting
            )
            verify(repository, never()).sendMessage(any(), any(), any(), any(), any())
        }

    @Test
    fun `given pre-login launch mode, when message sent, then chat starts with generic context`() =
        testBlocking {
            whenever(contextProvider.buildInitialContext()).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, FOLLOW_UP_MESSAGE, CONTEXT, null, null))
                .thenReturn(Result.success(createResponse(messages = listOf(createMessage(2L, SupportChatRole.BOT)))))

            viewModel.onLaunchModeLoaded(AiSupportChatLaunchMode.PreLogin)
            viewModel.onInputChanged(FOLLOW_UP_MESSAGE)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.chatId).isEqualTo(CHAT_ID)
            assertThat(state.isSending).isFalse()
            assertThat(state.showSendError).isFalse()
            assertThat(state.selectedIssueType).isNull()
            assertThat(state.diagnosticResult).isNull()
            verify(contextProvider).buildInitialContext()
            verify(repository, never()).registerChat(any(), any(), any())
            verify(repository, never()).markChatAsUpdated(any())
        }

    @Test
    fun `given pre-login chat exists, when follow up message sent, then bookmark is not touched`() =
        testBlocking {
            whenever(contextProvider.buildInitialContext()).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, FOLLOW_UP_MESSAGE, CONTEXT, null, null))
                .thenReturn(Result.success(createResponse()))
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, BOT_RESPONSE, JsonObject(), CHAT_ID, SESSION_ID))
                .thenReturn(Result.success(createResponse(messages = listOf(createMessage(3L, SupportChatRole.BOT)))))

            viewModel.onLaunchModeLoaded(AiSupportChatLaunchMode.PreLogin)
            viewModel.onInputChanged(FOLLOW_UP_MESSAGE)
            viewModel.onSendClicked()
            viewModel.onInputChanged(BOT_RESPONSE)
            viewModel.onSendClicked()

            verify(repository, never()).registerChat(any(), any(), any())
            verify(repository, never()).markChatAsUpdated(any())
        }

    @Test
    fun `given diagnostics pass, when issue selected, then success is shown and chat does not start`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))

            viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)

            val state = viewModel.viewState.value
            assertThat(state.input).isEmpty()
            assertThat(state.chatId).isNull()
            assertThat(state.hasProceededToChat).isFalse()
            assertThat(state.hasStartedChat).isFalse()
            assertThat(state.isRunningDiagnostics).isFalse()
            assertThat(state.canUseDiagnosticActions).isTrue()
            assertThat(state.showDiagnosticActions).isTrue()
            assertThat(state.showSendError).isFalse()
            assertThat(state.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting,
                AiSupportChatMessageContent.DiagnosticsProgress(result)
            )
            verify(repository, never()).sendMessage(any(), any(), any(), any(), any())
        }

    @Test
    fun `given diagnostics pass, when continuing, then chat starts with post diagnostics greeting`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))

            viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)
            viewModel.onContinueAfterDiagnosticsClicked()

            val state = viewModel.viewState.value
            assertThat(state.hasProceededToChat).isTrue()
            assertThat(state.hasStartedChat).isFalse()
            assertThat(state.canUseDiagnosticActions).isFalse()
            assertThat(state.showInputBar).isTrue()
            assertThat(state.showDiagnosticActions).isFalse()
            assertThat(state.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting,
                AiSupportChatMessageContent.DiagnosticsProgress(result),
                AiSupportChatMessageContent.PostDiagnosticsGreeting
            )
            verify(repository, never()).sendMessage(any(), any(), any(), any(), any())
        }

    @Test
    fun `given chat continued, when first message succeeds, then diagnostic context is sent and chat is registered`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            val response = createResponse(
                messages = listOf(
                    createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                    createMessage(messageId = 2L, role = SupportChatRole.BOT, content = BOT_RESPONSE)
                )
            )
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.success(response))

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.input).isEmpty()
            assertThat(state.chatId).isEqualTo(CHAT_ID)
            assertThat(state.sessionId).isEqualTo(SESSION_ID)
            assertThat(state.hasProceededToChat).isTrue()
            assertThat(state.hasStartedChat).isTrue()
            assertThat(state.isSending).isFalse()
            assertThat(state.showSendError).isFalse()
            assertThat(state.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting,
                AiSupportChatMessageContent.DiagnosticsProgress(result),
                AiSupportChatMessageContent.PostDiagnosticsGreeting,
                AiSupportChatMessageContent.Text(ISSUE_DETAILS),
                AiSupportChatMessageContent.Text(BOT_RESPONSE)
            )
            verify(repository).registerChat(CHAT_ID, DEFAULT_BOT_SLUG, ISSUE_DETAILS)
            verify(repository, never()).markChatAsUpdated(any())
        }

    @Test
    fun `given unauthenticated user, when sending succeeds, then bookmark is not touched`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(false)
            createViewModel()
            val result = createSuccessDiagnosticResult()
            val response = createResponse(
                messages = listOf(
                    createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                    createMessage(messageId = 2L, role = SupportChatRole.BOT, content = BOT_RESPONSE)
                )
            )
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.success(response))

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()

            assertThat(viewModel.viewState.value.chatId).isEqualTo(CHAT_ID)
            verify(repository, never()).registerChat(any(), any(), any())
            verify(repository, never()).markChatAsUpdated(any())
        }

    @Test
    fun `given bookmark registration fails, when sending succeeds, then thread is shown without error`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            val response = createResponse(
                messages = listOf(
                    createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                    createMessage(messageId = 2L, role = SupportChatRole.BOT, content = BOT_RESPONSE)
                )
            )
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.success(response))
            whenever(repository.registerChat(CHAT_ID, DEFAULT_BOT_SLUG, ISSUE_DETAILS))
                .thenThrow(RuntimeException("Bookmark write failed"))

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.input).isEmpty()
            assertThat(state.chatId).isEqualTo(CHAT_ID)
            assertThat(state.isSending).isFalse()
            assertThat(state.showSendError).isFalse()
            assertThat(state.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting,
                AiSupportChatMessageContent.DiagnosticsProgress(result),
                AiSupportChatMessageContent.PostDiagnosticsGreeting,
                AiSupportChatMessageContent.Text(ISSUE_DETAILS),
                AiSupportChatMessageContent.Text(BOT_RESPONSE)
            )
        }

    @Test
    fun `given existing chat, when sending follow up, then session id is sent and bookmark is touched`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            startChat(result)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, FOLLOW_UP_MESSAGE, JsonObject(), CHAT_ID, SESSION_ID))
                .thenReturn(
                    Result.success(
                        createResponse(
                            messages = listOf(
                                createMessage(
                                    messageId = 3L,
                                    role = SupportChatRole.BOT,
                                    content = FOLLOW_UP_BOT_RESPONSE
                                )
                            )
                        )
                    )
                )

            viewModel.onInputChanged(FOLLOW_UP_MESSAGE)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.chatId).isEqualTo(CHAT_ID)
            assertThat(state.sessionId).isEqualTo(SESSION_ID)
            assertThat(state.isSending).isFalse()
            assertThat(state.showSendError).isFalse()
            assertThat(state.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting,
                AiSupportChatMessageContent.DiagnosticsProgress(result),
                AiSupportChatMessageContent.PostDiagnosticsGreeting,
                AiSupportChatMessageContent.Text(ISSUE_DETAILS),
                AiSupportChatMessageContent.Text(FOLLOW_UP_MESSAGE),
                AiSupportChatMessageContent.Text(FOLLOW_UP_BOT_RESPONSE)
            )
            verify(repository).sendMessage(DEFAULT_BOT_SLUG, FOLLOW_UP_MESSAGE, JsonObject(), CHAT_ID, SESSION_ID)
            verify(repository).markChatAsUpdated(CHAT_ID)
        }

    @Test
    fun `given bot forwards to human support, when message succeeds, then human support prompt is shown`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            val supportArea = SupportChatSupportArea(
                area = "card-reader",
                topic = "woo_mobile_issue_card_reader",
                confidence = "high"
            )
            val response = createResponse(
                messages = listOf(
                    createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                    createMessage(
                        messageId = 2L,
                        role = SupportChatRole.BOT,
                        content = BOT_RESPONSE,
                        context = SupportChatMessageContext(
                            flags = SupportChatFlags(forwardToHumanSupport = true),
                            supportArea = supportArea
                        )
                    )
                )
            )
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.success(response))

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.showHumanSupportPrompt).isTrue
            assertThat(state.latestSupportArea).isEqualTo(supportArea)
            assertThat(state.canContactHumanSupportFromToolbar).isFalse
            assertThat(state.showInputBar).isFalse
            assertThat(state.completedUserMessageResponseCount).isEqualTo(1)
            assertThat(state.messages.map { it.content }).doesNotContain(AiSupportChatMessageContent.Text(BOT_RESPONSE))
        }

    @Test
    fun `given first user message has bot response, when chat updates, then toolbar support is unavailable`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            val response = createResponse(
                messages = listOf(
                    createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                    createMessage(messageId = 2L, role = SupportChatRole.BOT, content = BOT_RESPONSE)
                )
            )
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.success(response))

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.hasSentChatMessage).isTrue
            assertThat(state.completedUserMessageResponseCount).isEqualTo(1)
            assertThat(state.canContactHumanSupportFromToolbar).isFalse
        }

    @Test
    fun `given bot response has server message id, when chat updates, then bot UI message keeps message id`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            val response = createResponse(
                messages = listOf(
                    createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                    createMessage(messageId = BOT_MESSAGE_ID, role = SupportChatRole.BOT, content = BOT_RESPONSE)
                )
            )
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.success(response))

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()

            val botMessage = viewModel.viewState.value.messages.single {
                it.content == AiSupportChatMessageContent.Text(BOT_RESPONSE)
            }
            assertThat(botMessage.messageId).isEqualTo(BOT_MESSAGE_ID)
        }

    @Test
    fun `given unrated bot response, when thumbs up clicked, then rating is stored and submitted`() =
        testBlocking {
            startChatWithBotResponse()
            whenever(
                repository.submitFeedback(
                    DEFAULT_BOT_SLUG,
                    CHAT_ID,
                    BOT_MESSAGE_ID,
                    SESSION_ID,
                    true
                )
            ).thenReturn(Result.success(Unit))

            viewModel.onFeedbackClicked(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.UP)

            assertThat(viewModel.viewState.value.messageRatings)
                .containsEntry(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.UP)
            verify(repository).submitFeedback(DEFAULT_BOT_SLUG, CHAT_ID, BOT_MESSAGE_ID, SESSION_ID, true)
        }

    @Test
    fun `given latest bot response is upvoted, when feedback clicked, then resolved prompt is appended`() =
        testBlocking {
            startChatWithBotResponse()
            whenever(repository.submitFeedback(DEFAULT_BOT_SLUG, CHAT_ID, BOT_MESSAGE_ID, SESSION_ID, true))
                .thenReturn(Result.success(Unit))

            viewModel.onFeedbackClicked(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.UP)

            assertThat(viewModel.viewState.value.shouldShowResolvedButton).isTrue
            assertThat(viewModel.viewState.value.messages.map { it.content }).endsWith(
                AiSupportChatMessageContent.Text(BOT_RESPONSE),
                AiSupportChatMessageContent.ResolvedPrompt
            )
        }

    @Test
    fun `given unrated bot response, when thumbs down clicked, then rating is stored and submitted`() =
        testBlocking {
            startChatWithBotResponse()
            whenever(
                repository.submitFeedback(
                    DEFAULT_BOT_SLUG,
                    CHAT_ID,
                    BOT_MESSAGE_ID,
                    SESSION_ID,
                    false
                )
            ).thenReturn(Result.success(Unit))

            viewModel.onFeedbackClicked(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.DOWN)

            assertThat(viewModel.viewState.value.messageRatings)
                .containsEntry(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.DOWN)
            verify(repository).submitFeedback(DEFAULT_BOT_SLUG, CHAT_ID, BOT_MESSAGE_ID, SESSION_ID, false)
        }

    @Test
    fun `given latest bot response is resolved, when message succeeds, then resolved prompt is appended`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            val response = createResponse(
                messages = listOf(
                    createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                    createMessage(
                        messageId = BOT_MESSAGE_ID,
                        role = SupportChatRole.BOT,
                        content = BOT_RESPONSE,
                        isResolved = true
                    )
                )
            )
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.success(response))

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.shouldShowResolvedButton).isTrue
            assertThat(state.messages.map { it.content }).endsWith(
                AiSupportChatMessageContent.Text(BOT_RESPONSE),
                AiSupportChatMessageContent.ResolvedPrompt
            )
            val botMessage = state.messages.single { it.content == AiSupportChatMessageContent.Text(BOT_RESPONSE) }
            assertThat(botMessage.isResolved)
                .isTrue
        }

    @Test
    fun `given rated bot response, when feedback clicked again, then feedback is not submitted twice`() =
        testBlocking {
            startChatWithBotResponse()
            whenever(repository.submitFeedback(DEFAULT_BOT_SLUG, CHAT_ID, BOT_MESSAGE_ID, SESSION_ID, true))
                .thenReturn(Result.success(Unit))

            viewModel.onFeedbackClicked(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.UP)
            viewModel.onFeedbackClicked(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.DOWN)

            assertThat(viewModel.viewState.value.messageRatings)
                .containsEntry(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.UP)
            verify(repository).submitFeedback(DEFAULT_BOT_SLUG, CHAT_ID, BOT_MESSAGE_ID, SESSION_ID, true)
            verify(repository, never()).submitFeedback(DEFAULT_BOT_SLUG, CHAT_ID, BOT_MESSAGE_ID, SESSION_ID, false)
        }

    @Test
    fun `given resolved prompt exists, when latest bot response is upvoted again, then prompt is not duplicated`() =
        testBlocking {
            startChatWithBotResponse()
            whenever(repository.submitFeedback(DEFAULT_BOT_SLUG, CHAT_ID, BOT_MESSAGE_ID, SESSION_ID, true))
                .thenReturn(Result.success(Unit))

            viewModel.onFeedbackClicked(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.UP)
            viewModel.onFeedbackClicked(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.DOWN)

            assertThat(
                viewModel.viewState.value.messages.count { it.content == AiSupportChatMessageContent.ResolvedPrompt }
            ).isEqualTo(1)
        }

    @Test
    fun `given chat identifiers are missing, when feedback clicked, then feedback is not submitted`() =
        testBlocking {
            viewModel.onFeedbackClicked(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.UP)

            assertThat(viewModel.viewState.value.messageRatings).isEmpty()
            verify(repository, never()).submitFeedback(any(), any(), any(), any(), any())
        }

    @Test
    fun `given second user message has bot response, when chat updates, then toolbar support is available`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(
                    Result.success(
                        createResponse(
                            messages = listOf(
                                createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                                createMessage(messageId = 2L, role = SupportChatRole.BOT, content = BOT_RESPONSE)
                            )
                        )
                    )
                )
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, FOLLOW_UP_MESSAGE, JsonObject(), CHAT_ID, SESSION_ID))
                .thenReturn(
                    Result.success(
                        createResponse(
                            messages = listOf(
                                createMessage(messageId = 3L, role = SupportChatRole.USER, content = FOLLOW_UP_MESSAGE),
                                createMessage(
                                    messageId = 4L,
                                    role = SupportChatRole.BOT,
                                    content = FOLLOW_UP_BOT_RESPONSE
                                )
                            )
                        )
                    )
                )

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()
            viewModel.onInputChanged(FOLLOW_UP_MESSAGE)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.completedUserMessageResponseCount).isEqualTo(2)
            assertThat(state.canContactHumanSupportFromToolbar).isTrue
        }

    @Test
    fun `given two bot responses, when chat updates, then resolved button is available`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(
                    Result.success(
                        createResponse(
                            messages = listOf(
                                createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                                createMessage(messageId = 2L, role = SupportChatRole.BOT, content = BOT_RESPONSE)
                            )
                        )
                    )
                )
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, FOLLOW_UP_MESSAGE, JsonObject(), CHAT_ID, SESSION_ID))
                .thenReturn(
                    Result.success(
                        createResponse(
                            messages = listOf(
                                createMessage(messageId = 3L, role = SupportChatRole.USER, content = FOLLOW_UP_MESSAGE),
                                createMessage(
                                    messageId = 4L,
                                    role = SupportChatRole.BOT,
                                    content = FOLLOW_UP_BOT_RESPONSE
                                )
                            )
                        )
                    )
                )

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()
            viewModel.onInputChanged(FOLLOW_UP_MESSAGE)
            viewModel.onSendClicked()

            assertThat(viewModel.viewState.value.shouldShowResolvedButton).isTrue
        }

    @Test
    fun `given resolved button shown, when mark resolved confirmed, then chat actions are hidden`() =
        testBlocking {
            startChatWithBotResponse()
            whenever(repository.submitFeedback(DEFAULT_BOT_SLUG, CHAT_ID, BOT_MESSAGE_ID, SESSION_ID, true))
                .thenReturn(Result.success(Unit))

            viewModel.onFeedbackClicked(BOT_MESSAGE_ID, AiSupportChatFeedbackRating.UP)
            viewModel.onMarkResolvedClicked()
            viewModel.onMarkResolvedConfirmed()

            val state = viewModel.viewState.value
            assertThat(state.isChatResolved).isTrue
            assertThat(state.showMarkResolvedConfirmation).isFalse
            assertThat(state.canSendMessages).isFalse
            assertThat(state.showInputBar).isFalse
            assertThat(state.shouldShowResolvedButton).isFalse
            assertThat(state.canContactHumanSupportFromToolbar).isFalse
        }

    @Test
    fun `given send fails, when contact support from error is clicked, then escalation event is emitted`() =
        testBlocking {
            val result = createFailedDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.failure(Exception()))
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }

            viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)
            viewModel.onContinueAfterDiagnosticsClicked()
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()
            viewModel.onContactSupportClicked(HumanSupportContactSource.ERROR_DIALOG)

            val event = events.single() as ContactHumanSupport
            assertThat(event.chatId).isNull()
            assertThat(event.source).isEqualTo(HumanSupportContactSource.ERROR_DIALOG)
            assertThat(event.transcript).contains("User: $ISSUE_DETAILS")
        }

    @Test
    fun `given bot response has multiple paragraphs, when contact support is clicked, then transcript keeps first paragraph`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(
                    Result.success(
                        createResponse(
                            messages = listOf(
                                createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                                createMessage(
                                    messageId = 2L,
                                    role = SupportChatRole.BOT,
                                    content = "$BOT_RESPONSE\n\nSecond paragraph with extra details."
                                )
                            )
                        )
                    )
                )
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()
            viewModel.onContactSupportClicked(HumanSupportContactSource.TOOLBAR)

            val event = events.single() as ContactHumanSupport
            assertThat(event.transcript).contains("Bot: $BOT_RESPONSE\n[AI response trimmed]")
            assertThat(event.transcript).doesNotContain("Second paragraph with extra details.")
        }

    @Test
    fun `given long chat history, when contact support is clicked, then transcript keeps recent messages`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            val response = createResponse(
                messages = (1L..25L).map { messageId ->
                    createMessage(
                        messageId = messageId,
                        role = if (messageId % 2 == 0L) SupportChatRole.BOT else SupportChatRole.USER,
                        content = "message-$messageId"
                    )
                }
            )
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.success(response))
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }

            continueToChatAfterSuccessfulDiagnostics(result)
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()
            viewModel.onContactSupportClicked(HumanSupportContactSource.TOOLBAR)

            val event = events.single() as ContactHumanSupport
            assertThat(event.transcript).contains("[Earlier messages trimmed]")
            assertThat(event.transcript.lines()).doesNotContain("User: message-1")
            assertThat(event.transcript).contains("message-25")
        }

    @Test
    fun `given ticket is created, when state updates, then escalation controls are hidden`() = testBlocking {
        val result = createSuccessDiagnosticResult()
        startChat(result)

        viewModel.onSupportTicketCreated()

        val state = viewModel.viewState.value
        assertThat(state.hasCreatedTicket).isTrue
        assertThat(state.canSendMessages).isFalse
        assertThat(state.canContactHumanSupportFromToolbar).isFalse
        assertThat(state.showSendError).isFalse
        assertThat(state.showHumanSupportPrompt).isFalse
    }

    @Test
    fun `given connectivity launch mode, when loaded, then chat waits for user input with connectivity context`() =
        testBlocking {
            val checks = listOf(
                ConnectivityCheckCardData(ConnectivityCheckType.INTERNET, ConnectivityCheckStatus.Success()),
                ConnectivityCheckCardData(ConnectivityCheckType.WP_COM, ConnectivityCheckStatus.Failure()),
                ConnectivityCheckCardData(ConnectivityCheckType.STORE, ConnectivityCheckStatus.NotStarted),
                ConnectivityCheckCardData(ConnectivityCheckType.ORDERS, ConnectivityCheckStatus.InProgress)
            )
            whenever(contextProvider.buildInitialContext(any())).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.success(createResponse()))

            viewModel.onLaunchModeLoaded(AiSupportChatLaunchMode.ConnectivityTool(checks))

            val state = viewModel.viewState.value
            assertThat(state.hasProceededToChat).isTrue()
            assertThat(state.hasStartedChat).isFalse()
            assertThat(state.sessionId).isNull()
            assertThat(state.diagnosticResult?.statuses).hasSize(2)
            assertThat(state.diagnosticResult?.statuses?.map { it.test }).containsExactly(
                DiagnosticTest.INTERNET_CONNECTION,
                DiagnosticTest.WPCOM_SERVERS
            )
            assertThat(state.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting
            )
            verify(contextProvider, never()).buildInitialContext(any())
            verify(repository, never()).sendMessage(any(), any(), any(), any(), any())

            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()

            val updatedState = viewModel.viewState.value
            assertThat(updatedState.hasStartedChat).isTrue()
            assertThat(updatedState.sessionId).isEqualTo(SESSION_ID)
            val diagnosticResultCaptor = argumentCaptor<DiagnosticResult>()
            verify(contextProvider).buildInitialContext(diagnosticResultCaptor.capture())
            assertThat(diagnosticResultCaptor.firstValue.statuses.map { it.status }).containsExactly(
                TestStatus.Passed,
                TestStatus.Failed()
            )
            verify(repository).sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null)
            verify(repository).registerChat(CHAT_ID, DEFAULT_BOT_SLUG, ISSUE_DETAILS)
        }

    @Test
    fun `given launch mode was already loaded, when loaded again, then chat is not started twice`() =
        testBlocking {
            val checks = listOf(
                ConnectivityCheckCardData(ConnectivityCheckType.INTERNET, ConnectivityCheckStatus.Success())
            )

            viewModel.onLaunchModeLoaded(AiSupportChatLaunchMode.ConnectivityTool(checks))
            viewModel.onLaunchModeLoaded(AiSupportChatLaunchMode.ConnectivityTool(checks))

            assertThat(viewModel.viewState.value.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting
            )
            verify(repository, never()).sendMessage(any(), any(), any(), any(), any())
        }

    @Test
    fun `given diagnostics fail, when issue selected, then failure is shown and chat does not start`() =
        testBlocking {
            val result = createFailedDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))

            viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)

            val state = viewModel.viewState.value
            assertThat(state.hasProceededToChat).isFalse()
            assertThat(state.hasStartedChat).isFalse()
            assertThat(state.selectedIssueType).isEqualTo(SupportIssueType.LOADING_ORDERS)
            assertThat(state.diagnosticResult).isEqualTo(result)
            assertThat(state.showDiagnosticActions).isTrue()
            assertThat(state.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting,
                AiSupportChatMessageContent.DiagnosticsFailure(result)
            )
            verify(repository, never()).sendMessage(any(), any(), any(), any(), any())
        }

    @Test
    fun `given diagnostics fail, when continuing anyway, then chat starts without sending message`() =
        testBlocking {
            val result = createFailedDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))

            viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)
            viewModel.onContinueAfterDiagnosticsClicked()

            assertThat(viewModel.viewState.value.hasProceededToChat).isTrue()
            assertThat(viewModel.viewState.value.hasStartedChat).isFalse()
            assertThat(viewModel.viewState.value.showSendError).isFalse()
            assertThat(viewModel.viewState.value.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting,
                AiSupportChatMessageContent.DiagnosticsFailure(result),
                AiSupportChatMessageContent.PostDiagnosticsGreeting
            )
            verify(repository, never()).sendMessage(any(), any(), any(), any(), any())
        }

    @Test
    fun `given diagnostics fail, when retry tapped, then diagnostics run again`() = testBlocking {
        val result = createFailedDiagnosticResult()
        whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS))
            .thenReturn(flowOf(result), flowOf(result))

        viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)
        viewModel.onRetryDiagnosticsClicked()

        verify(diagnosticsService, times(2)).runDiagnostics(SupportIssueType.LOADING_ORDERS)
    }

    @Test
    fun `given diagnostics are running, when issue tapped again, then diagnostics do not run twice`() = testBlocking {
        whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(emptyFlow())

        viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)
        viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)

        assertThat(viewModel.viewState.value.showDiagnosticActions).isFalse()
        verify(diagnosticsService).runDiagnostics(SupportIssueType.LOADING_ORDERS)
    }

    @Test
    fun `given diagnostics flow throws, when issue selected, then failure is shown and loading stops`() = testBlocking {
        whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(
            flow {
                emit(
                    DiagnosticResult(
                        issueType = SupportIssueType.LOADING_ORDERS,
                        statuses = listOf(
                            DiagnosticStatus(DiagnosticTest.INTERNET_CONNECTION, TestStatus.Running)
                        )
                    )
                )
                error("Diagnostics unavailable")
            }
        )

        viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)

        val state = viewModel.viewState.value
        val diagnosticResult = requireNotNull(state.diagnosticResult)
        assertThat(state.isRunningDiagnostics).isFalse()
        assertThat(state.hasStartedChat).isFalse()
        assertThat(state.showSendError).isFalse()
        assertThat(state.showDiagnosticActions).isTrue()
        assertThat(diagnosticResult.firstFailure?.status).isEqualTo(
            TestStatus.Failed(technicalDetails = "Diagnostics unavailable")
        )
        assertThat(state.messages.map { it.content }).containsExactly(
            AiSupportChatMessageContent.Greeting,
            AiSupportChatMessageContent.DiagnosticsFailure(diagnosticResult)
        )
        verify(repository, never()).sendMessage(any(), any(), any(), any(), any())
    }

    @Test
    fun `given first chat message fails, when sending after diagnostics, then draft is restored and error is shown`() =
        testBlocking {
            val result = createFailedDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
                .thenReturn(Result.failure(Exception()))

            viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)
            viewModel.onContinueAfterDiagnosticsClicked()
            viewModel.onInputChanged(ISSUE_DETAILS)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.input).isEqualTo(ISSUE_DETAILS)
            assertThat(state.chatId).isNull()
            assertThat(state.hasProceededToChat).isTrue()
            assertThat(state.hasStartedChat).isFalse()
            assertThat(state.isSending).isFalse()
            assertThat(state.showSendError).isTrue()
            verify(repository, never()).registerChat(any(), any(), any())
        }

    @Test
    fun `given blank input, when sending before chat starts, then repository is not called`() = testBlocking {
        viewModel.onInputChanged("   ")
        viewModel.onSendClicked()

        assertThat(viewModel.viewState.value.messages.map { it.content }).containsExactly(
            AiSupportChatMessageContent.Greeting,
            AiSupportChatMessageContent.IssuePicker
        )
        verify(repository, never()).sendMessage(any(), any(), any(), any(), any())
    }

    private suspend fun continueToChatAfterSuccessfulDiagnostics(result: DiagnosticResult) {
        viewModel.onIssueSelected(result.issueType, ISSUE_LABEL)
        viewModel.onContinueAfterDiagnosticsClicked()
    }

    private suspend fun startChat(result: DiagnosticResult) {
        whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
        whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
        whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
            .thenReturn(Result.success(createResponse()))

        continueToChatAfterSuccessfulDiagnostics(result)
        viewModel.onInputChanged(ISSUE_DETAILS)
        viewModel.onSendClicked()
    }

    private suspend fun startChatWithBotResponse() {
        val result = createSuccessDiagnosticResult()
        whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
        whenever(contextProvider.buildInitialContext(diagnosticResult = result)).thenReturn(CONTEXT)
        whenever(repository.sendMessage(DEFAULT_BOT_SLUG, ISSUE_DETAILS, CONTEXT, null, null))
            .thenReturn(
                Result.success(
                    createResponse(
                        messages = listOf(
                            createMessage(messageId = 1L, role = SupportChatRole.USER, content = ISSUE_DETAILS),
                            createMessage(
                                messageId = BOT_MESSAGE_ID,
                                role = SupportChatRole.BOT,
                                content = BOT_RESPONSE
                            )
                        )
                    )
                )
            )

        continueToChatAfterSuccessfulDiagnostics(result)
        viewModel.onInputChanged(ISSUE_DETAILS)
        viewModel.onSendClicked()
    }

    private fun createSuccessDiagnosticResult(issueType: SupportIssueType = SupportIssueType.LOADING_ORDERS) =
        DiagnosticResult(
            issueType = issueType,
            statuses = listOf(
                DiagnosticStatus(DiagnosticTest.INTERNET_CONNECTION, TestStatus.Passed),
                DiagnosticStatus(DiagnosticTest.WPCOM_SERVERS, TestStatus.Passed)
            )
        )

    private fun createFailedDiagnosticResult(issueType: SupportIssueType = SupportIssueType.LOADING_ORDERS) =
        DiagnosticResult(
            issueType = issueType,
            statuses = listOf(
                DiagnosticStatus(DiagnosticTest.INTERNET_CONNECTION, TestStatus.Passed),
                DiagnosticStatus(
                    DiagnosticTest.WPCOM_SERVERS,
                    TestStatus.Failed(technicalDetails = "WPCom 503", durationMs = 250L)
                ),
                DiagnosticStatus(DiagnosticTest.STORE_CONNECTION, TestStatus.Pending)
            )
        )

    private fun createResponse(
        chatId: Long = CHAT_ID,
        botSlug: String = DEFAULT_BOT_SLUG,
        messages: List<SupportChatMessage> = emptyList()
    ): SupportChatResponse = SupportChatResponse(
        chatId = chatId,
        sessionId = SESSION_ID,
        botSlug = botSlug,
        botVersion = "1.0.0",
        messages = messages
    )

    private fun createMessage(
        messageId: Long,
        role: SupportChatRole,
        content: String = BOT_RESPONSE,
        context: SupportChatMessageContext? = null,
        isResolved: Boolean = false
    ): SupportChatMessage = SupportChatMessage(
        messageId = messageId,
        role = role,
        content = content,
        context = context ?: if (isResolved) SupportChatMessageContext(isResolved = true) else null
    )

    private companion object {
        const val CHAT_ID = 1234L
        const val SESSION_ID = "session-id"
        const val ISSUE_LABEL = "I can't see my orders"
        const val ISSUE_DETAILS = "My latest orders are missing"
        const val FOLLOW_UP_MESSAGE = "Still broken"
        const val BOT_MESSAGE_ID = 2L
        const val BOT_RESPONSE = "Let's troubleshoot orders."
        const val FOLLOW_UP_BOT_RESPONSE = "Let's keep troubleshooting."

        val CONTEXT = JsonObject().apply {
            addProperty("selectedSiteId", 20L)
        }
    }
}
