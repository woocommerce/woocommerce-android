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
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatMessage
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatRole
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckCardData
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckType
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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
    private val resourceProvider: ResourceProvider = mock()

    private lateinit var viewModel: AiSupportChatViewModel

    @Before
    fun setUp() {
        viewModel = AiSupportChatViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            contextProvider = contextProvider,
            diagnosticsService = diagnosticsService,
            resourceProvider = resourceProvider
        )
    }

    @Test
    fun `when initialized, then greeting and issue picker are shown`() {
        val state = viewModel.viewState.value

        assertThat(state.hasStartedChat).isFalse()
        assertThat(state.messages.map { it.content }).containsExactly(
            AiSupportChatMessageContent.Greeting,
            AiSupportChatMessageContent.IssuePicker
        )
    }

    @Test
    fun `given pre-login launch mode, when loaded, then chat starts without issue picker`() =
        testBlocking {
            viewModel.onLaunchModeLoaded(AiSupportChatLaunchMode.PreLogin)

            val state = viewModel.viewState.value
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
    fun `given diagnostics pass, when issue selected, then chat starts with diagnostic context`() =
        testBlocking {
            val result = createSuccessDiagnosticResult()
            val response = createResponse(
                messages = listOf(
                    createMessage(
                        messageId = 1L,
                        role = SupportChatRole.USER,
                        content = ISSUE_LABEL
                    ),
                    createMessage(messageId = 2L, role = SupportChatRole.BOT, content = BOT_RESPONSE)
                )
            )
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(SupportIssueType.LOADING_ORDERS, result)).thenReturn(CONTEXT)
            whenever(
                repository.sendMessage(
                    DEFAULT_BOT_SLUG,
                    ISSUE_LABEL,
                    CONTEXT,
                    null,
                    null
                )
            ).thenReturn(Result.success(response))

            viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)

            val state = viewModel.viewState.value
            assertThat(state.input).isEmpty()
            assertThat(state.chatId).isEqualTo(CHAT_ID)
            assertThat(state.hasStartedChat).isTrue()
            assertThat(state.isSending).isFalse()
            assertThat(state.showSendError).isFalse()
            assertThat(state.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting,
                AiSupportChatMessageContent.DiagnosticsProgress(result),
                AiSupportChatMessageContent.Text(ISSUE_LABEL),
                AiSupportChatMessageContent.Text(BOT_RESPONSE)
            )
            verify(repository).registerChat(
                CHAT_ID,
                DEFAULT_BOT_SLUG,
                ISSUE_LABEL
            )
            verify(repository, never()).markChatAsUpdated(any())
        }

    @Test
    fun `given existing chat, when sending follow up, then message is sent with chat id and bookmark is touched`() =
        testBlocking {
            startChat()
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, FOLLOW_UP_MESSAGE, JsonObject(), CHAT_ID, SESSION_ID))
                .thenReturn(Result.success(createResponse(messages = listOf(createMessage(3L, SupportChatRole.BOT)))))

            viewModel.onInputChanged(FOLLOW_UP_MESSAGE)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.chatId).isEqualTo(CHAT_ID)
            assertThat(state.isSending).isFalse()
            assertThat(state.showSendError).isFalse()
            verify(repository).sendMessage(DEFAULT_BOT_SLUG, FOLLOW_UP_MESSAGE, JsonObject(), CHAT_ID, SESSION_ID)
            verify(repository).markChatAsUpdated(CHAT_ID)
        }

    @Test
    fun `given connectivity launch mode, when loaded, then chat starts with connectivity context`() = testBlocking {
        whenever(
            resourceProvider.getString(
                com.woocommerce.android.R.string.ai_support_chat_connectivity_initial_message
            )
        )
            .thenReturn(CONNECTIVITY_MESSAGE)
        whenever(contextProvider.buildInitialContext(eq(SupportIssueType.OTHER), any())).thenReturn(CONTEXT)
        whenever(repository.sendMessage(DEFAULT_BOT_SLUG, CONNECTIVITY_MESSAGE, CONTEXT, null, null))
            .thenReturn(Result.success(createResponse()))

        viewModel.onLaunchModeLoaded(
            AiSupportChatLaunchMode.ConnectivityTool(
                checks = listOf(
                    ConnectivityCheckCardData(ConnectivityCheckType.INTERNET, ConnectivityCheckStatus.Success()),
                    ConnectivityCheckCardData(ConnectivityCheckType.WP_COM, ConnectivityCheckStatus.Failure())
                )
            )
        )

        val state = viewModel.viewState.value
        assertThat(state.hasStartedChat).isTrue()
        assertThat(state.diagnosticResult?.statuses).hasSize(2)
        verify(repository).sendMessage(DEFAULT_BOT_SLUG, CONNECTIVITY_MESSAGE, CONTEXT, null, null)
    }

    @Test
    fun `given diagnostics fail, when issue selected, then failure is shown and chat does not start`() =
        testBlocking {
            val result = createFailedDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))

            viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)

            val state = viewModel.viewState.value
            assertThat(state.hasStartedChat).isFalse()
            assertThat(state.selectedIssueType).isEqualTo(SupportIssueType.LOADING_ORDERS)
            assertThat(state.diagnosticResult).isEqualTo(result)
            assertThat(state.messages.map { it.content }).containsExactly(
                AiSupportChatMessageContent.Greeting,
                AiSupportChatMessageContent.DiagnosticsFailure(result)
            )
            verify(repository, never()).sendMessage(any(), any(), any(), any(), any())
        }

    @Test
    fun `given diagnostics fail, when continuing anyway, then chat starts with failure context`() =
        testBlocking {
            val result = createFailedDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(SupportIssueType.LOADING_ORDERS, result)).thenReturn(CONTEXT)
            whenever(
                repository.sendMessage(
                    DEFAULT_BOT_SLUG,
                    ISSUE_LABEL,
                    CONTEXT,
                    null,
                    null
                )
            ).thenReturn(Result.success(createResponse()))

            viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)
            viewModel.onContinueAfterDiagnosticsClicked()

            assertThat(viewModel.viewState.value.hasStartedChat).isTrue()
            assertThat(viewModel.viewState.value.showSendError).isFalse()
            verify(repository).sendMessage(
                DEFAULT_BOT_SLUG,
                ISSUE_LABEL,
                CONTEXT,
                null,
                null
            )
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
        val result = createSuccessDiagnosticResult()
        whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))

        viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)
        viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)

        verify(diagnosticsService).runDiagnostics(SupportIssueType.LOADING_ORDERS)
    }

    @Test
    fun `given initial message fails, when continuing after diagnostics, then draft is restored and error is shown`() =
        testBlocking {
            val result = createFailedDiagnosticResult()
            whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
            whenever(contextProvider.buildInitialContext(SupportIssueType.LOADING_ORDERS, result)).thenReturn(CONTEXT)
            whenever(
                repository.sendMessage(
                    DEFAULT_BOT_SLUG,
                    ISSUE_LABEL,
                    CONTEXT,
                    null,
                    null
                )
            ).thenReturn(Result.failure(Exception()))

            viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)
            viewModel.onContinueAfterDiagnosticsClicked()

            val state = viewModel.viewState.value
            assertThat(state.input).isEqualTo(ISSUE_LABEL)
            assertThat(state.chatId).isNull()
            assertThat(state.hasStartedChat).isTrue()
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

    private suspend fun startChat() {
        val result = createSuccessDiagnosticResult()
        whenever(diagnosticsService.runDiagnostics(SupportIssueType.LOADING_ORDERS)).thenReturn(flowOf(result))
        whenever(contextProvider.buildInitialContext(SupportIssueType.LOADING_ORDERS, result)).thenReturn(CONTEXT)
        whenever(
            repository.sendMessage(
                DEFAULT_BOT_SLUG,
                ISSUE_LABEL,
                CONTEXT,
                null,
                null
            )
        ).thenReturn(Result.success(createResponse()))

        viewModel.onIssueSelected(SupportIssueType.LOADING_ORDERS, ISSUE_LABEL)
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
        content: String = BOT_RESPONSE
    ): SupportChatMessage = SupportChatMessage(
        messageId = messageId,
        role = role,
        content = content
    )

    private companion object {
        const val CHAT_ID = 1234L
        const val SESSION_ID = "session-id"
        const val CONNECTIVITY_MESSAGE = "Help me troubleshoot my store connection."
        const val ISSUE_LABEL = "I can't see my orders"
        const val FOLLOW_UP_MESSAGE = "Still broken"
        const val BOT_RESPONSE = "Let's troubleshoot orders."

        val CONTEXT = JsonObject().apply {
            addProperty("site_id", 20L)
        }
    }
}
