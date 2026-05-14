package com.woocommerce.android.ui.aisupportchat

import androidx.lifecycle.SavedStateHandle
import com.google.gson.JsonObject
import com.woocommerce.android.ui.aisupportchat.AiSupportChatViewModel.Companion.DEFAULT_BOT_SLUG
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatMessage
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatRole
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AiSupportChatViewModelTest : BaseUnitTest() {
    private val repository: SupportChatRepository = mock()
    private val contextProvider: SupportChatContextProvider = mock()

    private lateinit var viewModel: AiSupportChatViewModel

    @Before
    fun setUp() {
        whenever(contextProvider.buildInitialContext()).thenReturn(CONTEXT)
        viewModel = AiSupportChatViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            contextProvider = contextProvider
        )
    }

    @Test
    fun `given initial message succeeds, when sending, then thread is shown and chat is registered`() =
        testBlocking {
            val response = createResponse(
                messages = listOf(
                    createMessage(messageId = 1L, role = SupportChatRole.USER, content = MESSAGE),
                    createMessage(messageId = 2L, role = SupportChatRole.BOT, content = BOT_RESPONSE)
                )
            )
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, MESSAGE, CONTEXT, null))
                .thenReturn(Result.success(response))

            viewModel.onInputChanged("  $MESSAGE  ")
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.input).isEmpty()
            assertThat(state.chatId).isEqualTo(CHAT_ID)
            assertThat(state.isSending).isFalse()
            assertThat(state.showSendError).isFalse()
            assertThat(state.messages).containsExactly(
                AiSupportChatMessage("user-1", AiSupportChatMessageRole.USER, MESSAGE),
                AiSupportChatMessage("bot-2", AiSupportChatMessageRole.BOT, BOT_RESPONSE)
            )
            verify(repository).registerChat(CHAT_ID, DEFAULT_BOT_SLUG, MESSAGE)
            verify(repository, never()).markChatAsUpdated(any())
        }

    @Test
    fun `given bookmark registration fails, when sending succeeds, then thread is shown without error`() =
        testBlocking {
            val response = createResponse(
                messages = listOf(
                    createMessage(messageId = 1L, role = SupportChatRole.USER, content = MESSAGE),
                    createMessage(messageId = 2L, role = SupportChatRole.BOT, content = BOT_RESPONSE)
                )
            )
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, MESSAGE, CONTEXT, null))
                .thenReturn(Result.success(response))
            whenever(repository.registerChat(CHAT_ID, DEFAULT_BOT_SLUG, MESSAGE))
                .thenThrow(RuntimeException("Bookmark write failed"))

            viewModel.onInputChanged(MESSAGE)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.input).isEmpty()
            assertThat(state.chatId).isEqualTo(CHAT_ID)
            assertThat(state.isSending).isFalse()
            assertThat(state.showSendError).isFalse()
            assertThat(state.messages).containsExactly(
                AiSupportChatMessage("user-1", AiSupportChatMessageRole.USER, MESSAGE),
                AiSupportChatMessage("bot-2", AiSupportChatMessageRole.BOT, BOT_RESPONSE)
            )
        }

    @Test
    fun `given existing chat, when sending follow up, then message is sent with chat id and bookmark is touched`() =
        testBlocking {
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, MESSAGE, CONTEXT, null))
                .thenReturn(Result.success(createResponse()))
            whenever(repository.sendMessage(DEFAULT_BOT_SLUG, FOLLOW_UP_MESSAGE, JsonObject(), CHAT_ID))
                .thenReturn(
                    Result.success(
                        createResponse(
                            messages = listOf(
                                createMessage(messageId = 3L, role = SupportChatRole.BOT, content = FOLLOW_UP_BOT_RESPONSE)
                            )
                        )
                    )
                )

            viewModel.onInputChanged(MESSAGE)
            viewModel.onSendClicked()
            viewModel.onInputChanged(FOLLOW_UP_MESSAGE)
            viewModel.onSendClicked()

            val state = viewModel.viewState.value
            assertThat(state.chatId).isEqualTo(CHAT_ID)
            assertThat(state.isSending).isFalse()
            assertThat(state.showSendError).isFalse()
            assertThat(state.messages).containsExactly(
                AiSupportChatMessage("local-1", AiSupportChatMessageRole.USER, MESSAGE),
                AiSupportChatMessage("local-2", AiSupportChatMessageRole.USER, FOLLOW_UP_MESSAGE),
                AiSupportChatMessage("bot-3", AiSupportChatMessageRole.BOT, FOLLOW_UP_BOT_RESPONSE)
            )
            verify(repository).sendMessage(DEFAULT_BOT_SLUG, FOLLOW_UP_MESSAGE, JsonObject(), CHAT_ID)
            verify(repository).markChatAsUpdated(CHAT_ID)
        }

    @Test
    fun `given initial message fails, when sending, then draft is restored and error is shown`() = testBlocking {
        whenever(repository.sendMessage(DEFAULT_BOT_SLUG, MESSAGE, CONTEXT, null))
            .thenReturn(Result.failure(Exception()))

        viewModel.onInputChanged(MESSAGE)
        viewModel.onSendClicked()

        val state = viewModel.viewState.value
        assertThat(state.input).isEqualTo(MESSAGE)
        assertThat(state.messages).isEmpty()
        assertThat(state.chatId).isNull()
        assertThat(state.isSending).isFalse()
        assertThat(state.showSendError).isTrue()
        verify(repository, never()).registerChat(any(), any(), any())
    }

    @Test
    fun `given blank input, when sending, then repository is not called`() = testBlocking {
        viewModel.onInputChanged("   ")
        viewModel.onSendClicked()

        assertThat(viewModel.viewState.value.messages).isEmpty()
        verify(repository, never()).sendMessage(any(), any(), any(), any())
    }

    private fun createResponse(
        chatId: Long = CHAT_ID,
        botSlug: String = DEFAULT_BOT_SLUG,
        messages: List<SupportChatMessage> = emptyList()
    ): SupportChatResponse = SupportChatResponse(
        chatId = chatId,
        sessionId = "session-id",
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
        const val MESSAGE = "I need help with orders"
        const val FOLLOW_UP_MESSAGE = "Still broken"
        const val BOT_RESPONSE = "Let's troubleshoot orders."
        const val FOLLOW_UP_BOT_RESPONSE = "Let's keep troubleshooting."

        val CONTEXT = JsonObject().apply {
            addProperty("site_id", 20L)
        }
    }
}
