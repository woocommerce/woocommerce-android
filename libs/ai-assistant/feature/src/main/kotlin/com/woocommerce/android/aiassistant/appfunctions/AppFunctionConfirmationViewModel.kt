package com.woocommerce.android.aiassistant.appfunctions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewContext
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProviderRegistry
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewRenderer
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCard
import com.woocommerce.android.aiassistant.ui.AssistantConfirmationCardState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

internal sealed interface AppFunctionConfirmationState {
    data object Loading : AppFunctionConfirmationState
    data class Pending(
        val storeName: String,
        val store: StoreIdentity,
        val card: AssistantConfirmationCard,
    ) : AppFunctionConfirmationState
    data class Saving(val storeName: String) : AppFunctionConfirmationState
    data class Done(val storeName: String, val card: AssistantConfirmationCard) : AppFunctionConfirmationState
    data object CheckInApp : AppFunctionConfirmationState
    data class StoreChanged(val requestedStoreName: String) : AppFunctionConfirmationState
    data object Unavailable : AppFunctionConfirmationState
}

@HiltViewModel
internal class AppFunctionConfirmationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val toolRegistry: ToolRegistry,
    private val storeAccess: AppFunctionsStoreAccess,
    private val previewProviders: ConfirmationPreviewProviderRegistry,
    private val previewRenderer: ConfirmationPreviewRenderer,
    @AiAssistantJson private val json: Json,
) : ViewModel() {
    private val storeName: String = savedStateHandle[EXTRA_STORE_NAME] ?: ""

    private val _state = MutableStateFlow<AppFunctionConfirmationState>(AppFunctionConfirmationState.Loading)
    val state: StateFlow<AppFunctionConfirmationState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.value = initialState() }
    }

    fun onConfirm() {
        val pending = _state.value as? AppFunctionConfirmationState.Pending ?: return
        when (storeAccess.check(pending.store)) {
            StoreCheck.NO_ACCESS -> _state.value = AppFunctionConfirmationState.Unavailable
            StoreCheck.DIFFERENT_STORE -> _state.value = AppFunctionConfirmationState.StoreChanged(storeName)
            StoreCheck.SAME_STORE -> save(pending)
        }
    }

    private fun save(pending: AppFunctionConfirmationState.Pending) {
        savedStateHandle[KEY_REQUEST_CONSUMED] = true
        _state.value = AppFunctionConfirmationState.Saving(storeName)
        viewModelScope.launch {
            _state.value = when (toolRegistry.execute(pending.card.toolCall)) {
                is ToolResult.Success -> AppFunctionConfirmationState.Done(
                    storeName = storeName,
                    card = pending.card.copy(state = AssistantConfirmationCardState.CONFIRMED),
                )
                is ToolResult.ValidationError,
                is ToolResult.TransportError,
                is ToolResult.RejectedBySafety -> AppFunctionConfirmationState.CheckInApp
            }
        }
    }

    private suspend fun initialState(): AppFunctionConfirmationState {
        val store = savedStateHandle.requestedStore()
        val call = toolCall()
        val descriptor = call?.let { target -> toolRegistry.descriptors().firstOrNull { it.name == target.name } }
        return when {
            savedStateHandle.get<Boolean>(KEY_REQUEST_CONSUMED) == true -> AppFunctionConfirmationState.CheckInApp
            store == null || call == null || descriptor == null -> AppFunctionConfirmationState.Unavailable
            else -> when (storeAccess.check(store)) {
                StoreCheck.NO_ACCESS -> AppFunctionConfirmationState.Unavailable
                StoreCheck.DIFFERENT_STORE -> AppFunctionConfirmationState.StoreChanged(storeName)
                StoreCheck.SAME_STORE -> AppFunctionConfirmationState.Pending(
                    storeName = storeName,
                    store = store,
                    card = pendingCard(call, descriptor),
                )
            }
        }
    }

    private suspend fun pendingCard(call: ToolCall, descriptor: ToolDescriptor): AssistantConfirmationCard {
        val request = ConfirmationRequest(
            id = call.id,
            toolCallId = call.id,
            toolName = call.name,
            arguments = call.arguments,
            safetyLevel = descriptor.safetyLevel,
        )
        val preview = previewProviders.buildPreview(ConfirmationPreviewContext(request, descriptor))
        return AssistantConfirmationCard(
            confirmationId = call.id,
            toolCall = call,
            state = AssistantConfirmationCardState.PENDING,
            preview = previewRenderer.render(preview),
        )
    }

    private fun toolCall(): ToolCall? {
        val requestId: String? = savedStateHandle[EXTRA_REQUEST_ID]
        val toolName: String? = savedStateHandle[EXTRA_TOOL_NAME]
        val toolArguments: String? = savedStateHandle[EXTRA_TOOL_ARGUMENTS]
        return if (requestId == null || toolName == null || toolArguments == null) {
            null
        } else {
            ToolCall(id = requestId, name = toolName, arguments = json.parseToJsonElement(toolArguments).jsonObject)
        }
    }

    companion object {
        internal const val KEY_REQUEST_CONSUMED = "appfunctions_request_consumed"
    }
}
