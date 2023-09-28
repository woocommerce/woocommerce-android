package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.getStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProductNameSubViewModel(
    savedStateHandle: SavedStateHandle,
    override val onDone: (String) -> Unit
) : AddProductWithAISubViewModel<String> {
    companion object {
        private const val KEY_SUBSCREEN_NAME = "product_name"
    }

    private val _events = MutableSharedFlow<Event>()
    override val events: Flow<Event> get() = _events

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val name = savedStateHandle.getStateFlow(viewModelScope, "", KEY_SUBSCREEN_NAME)

    val state = name.map {
        UiState(it)
    }.asLiveData()

    fun onProductNameChanged(enteredName: String) {
        name.value = enteredName
    }

    fun onDoneClick() {
        onDone(name.value)
    }

    fun onSuggestNameClicked() {
        viewModelScope.launch {
            _events.emit(NavigateToAIProductNameBottomSheet)
        }
    }

    override fun close() {
        viewModelScope.cancel()
    }

    data class UiState(
        val name: String
    )

    object NavigateToAIProductNameBottomSheet : Event()
}
