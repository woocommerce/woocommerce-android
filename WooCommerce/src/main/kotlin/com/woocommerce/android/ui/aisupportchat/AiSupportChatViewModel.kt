package com.woocommerce.android.ui.aisupportchat

import androidx.lifecycle.SavedStateHandle
import com.google.gson.JsonObject
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatMessage
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatRole
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSupportChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SupportChatRepository,
    private val contextProvider: SupportChatContextProvider
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(AiSupportChatViewState())
    val viewState = _viewState.asStateFlow()

    private var localMessageId = 0L

    fun onInputChanged(input: String) {
        _viewState.update { it.copy(input = input) }
    }

    fun onSendClicked() {
        val state = _viewState.value
        val message = state.input.trim()
        if (message.isBlank() || state.isSending) return

        val optimisticMessage = AiSupportChatMessage(
            id = nextLocalMessageId(),
            role = AiSupportChatMessageRole.USER,
            content = message
        )
        _viewState.update {
            it.copy(
                input = "",
                messages = it.messages + optimisticMessage,
                isSending = true,
                showSendError = false
            )
        }

        launch {
            val chatId = _viewState.value.chatId
            val context = if (chatId == null) contextProvider.buildInitialContext() else JsonObject()

            repository.sendMessage(
                botSlug = DEFAULT_BOT_SLUG,
                message = message,
                context = context,
                chatId = chatId
            ).onSuccess { response ->
                handleSendSuccess(
                    response = response,
                    sentMessage = message,
                    wasInitialMessage = chatId == null
                )
            }.onFailure {
                handleSendFailure(message = message, optimisticMessage = optimisticMessage)
            }
        }
    }

    private suspend fun handleSendSuccess(
        response: SupportChatResponse,
        sentMessage: String,
        wasInitialMessage: Boolean
    ) {
        if (wasInitialMessage) {
            repository.registerChat(
                chatId = response.chatId,
                botSlug = response.botSlug,
                firstUserMessage = sentMessage
            )
        } else {
            repository.markChatAsUpdated(response.chatId)
        }

        _viewState.update {
            it.copy(
                chatId = response.chatId,
                messages = response.messages.toUiMessages().ifEmpty { it.messages },
                isSending = false,
                showSendError = false
            )
        }
    }

    private fun handleSendFailure(message: String, optimisticMessage: AiSupportChatMessage) {
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
                content = message.content
            )
        }

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
    }
}

data class AiSupportChatViewState(
    val input: String = "",
    val messages: List<AiSupportChatMessage> = emptyList(),
    val chatId: Long? = null,
    val isSending: Boolean = false,
    val showSendError: Boolean = false
)

data class AiSupportChatMessage(
    val id: String,
    val role: AiSupportChatMessageRole,
    val content: String
)

enum class AiSupportChatMessageRole {
    USER,
    BOT
}
