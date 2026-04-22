package com.woocommerce.android.ui.ai

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.ai.model.AIAssistantEvent
import com.woocommerce.android.ui.ai.model.ChatMessage
import com.woocommerce.android.ui.ai.model.ChatMessage.Role
import com.woocommerce.android.ui.ai.model.MessageContent
import com.woocommerce.android.ui.ai.parser.AssistantResponseParser
import com.woocommerce.android.ui.ai.repository.AIAssistantRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val repository: AIAssistantRepository,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val _uiState = MutableStateFlow(AIAssistantUiState())
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<ChatMessage>()

    init {
        connectToStore()
    }

    private fun connectToStore() {
        _uiState.update { it.copy(isConnecting = true, error = null) }
        launch {
            // POC: Hardcoded WC REST API credentials
            val result = repository.connectToStore(
                consumerKey = POC_CONSUMER_KEY,
                consumerSecret = POC_CONSUMER_SECRET
            )

            result
                .onSuccess { toolCount ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            mcpConnected = true,
                            availableTools = toolCount
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            mcpConnected = false,
                            error = "Failed to connect: ${error.message}"
                        )
                    }
                }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(role = Role.USER, content = text)
        addMessageToUi(userMessage)
        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        launch {
            var assistantText = ""
            repository.sendMessage(conversationHistory, text).collect { event ->
                when (event) {
                    is AIAssistantEvent.StreamingText -> {
                        assistantText += event.chunk
                        updateLastAssistantMessage(assistantText)
                    }

                    is AIAssistantEvent.ToolCallStarted -> {
                        addStatusMessage("Calling tool: ${event.toolName}...")
                    }

                    is AIAssistantEvent.ToolCallCompleted -> {
                        updateLastStatusMessage("Tool ${event.toolName} completed")
                    }

                    is AIAssistantEvent.FinalResponse -> {
                        removeStatusMessages()
                        val assistantMessage = ChatMessage(
                            role = Role.ASSISTANT,
                            content = event.fullText
                        )
                        addParsedAssistantMessageToUi(assistantMessage)
                        conversationHistory.add(userMessage)
                        conversationHistory.add(assistantMessage)
                        _uiState.update { it.copy(isLoading = false) }
                    }

                    is AIAssistantEvent.Error -> {
                        removeStatusMessages()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = event.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun onRetry() {
        if (_uiState.value.mcpConnected) {
            _uiState.update { it.copy(error = null) }
        } else {
            connectToStore()
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onOrderClicked(orderId: Long) {
        // TODO: Navigate to order detail
    }

    fun onProductClicked(productId: Long) {
        // TODO: Navigate to product detail
    }

    private fun addMessageToUi(message: ChatMessage) {
        _uiState.update { it.copy(messages = it.messages + UiChatMessage.fromChatMessage(message)) }
    }

    private fun addParsedAssistantMessageToUi(message: ChatMessage) {
        val segments = AssistantResponseParser.parse(message.content ?: "")
        val uiMessage = UiChatMessage(
            text = message.content ?: "",
            contentSegments = segments,
            isAssistant = true
        )
        _uiState.update { it.copy(messages = it.messages + uiMessage) }
    }

    private fun addStatusMessage(text: String) {
        _uiState.update {
            it.copy(messages = it.messages + UiChatMessage(text = text, isStatus = true))
        }
    }

    private fun updateLastStatusMessage(text: String) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val lastIndex = messages.indexOfLast { it.isStatus }
            if (lastIndex >= 0) {
                messages[lastIndex] = messages[lastIndex].copy(text = text)
            }
            state.copy(messages = messages)
        }
    }

    private fun updateLastAssistantMessage(text: String) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val lastIndex = messages.indexOfLast { it.isAssistant && !it.isStatus }
            if (lastIndex >= 0) {
                messages[lastIndex] = messages[lastIndex].copy(text = text)
            } else {
                messages.add(UiChatMessage(text = text, isAssistant = true))
            }
            state.copy(messages = messages)
        }
    }

    private fun removeStatusMessages() {
        _uiState.update { state ->
            state.copy(messages = state.messages.filter { !it.isStatus })
        }
    }

    data class AIAssistantUiState(
        val messages: List<UiChatMessage> = emptyList(),
        val inputText: String = "",
        val isLoading: Boolean = false,
        val isConnecting: Boolean = false,
        val mcpConnected: Boolean = false,
        val availableTools: Int = 0,
        val error: String? = null
    )

    data class UiChatMessage(
        val text: String,
        val contentSegments: List<MessageContent> = listOf(MessageContent.Text(text)),
        val isAssistant: Boolean = false,
        val isStatus: Boolean = false
    ) {
        companion object {
            fun fromChatMessage(message: ChatMessage): UiChatMessage {
                val content = message.content ?: ""
                return UiChatMessage(
                    text = content,
                    contentSegments = listOf(MessageContent.Text(content)),
                    isAssistant = message.role == Role.ASSISTANT
                )
            }
        }
    }

    companion object {
        // POC: Replace with actual credentials
        private const val POC_CONSUMER_KEY = "ck_REPLACE_ME"
        private const val POC_CONSUMER_SECRET = "cs_REPLACE_ME"
    }
}
