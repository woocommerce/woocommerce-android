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

class AboutProductSubViewModel(
    savedStateHandle: SavedStateHandle,
    override val onDone: (String) -> Unit
) : AddProductWithAISubViewModel<String> {
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    override val events: Flow<Event> get() = _events

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val productFeatures = savedStateHandle.getStateFlow(viewModelScope, "")

    val state = productFeatures.map {
        UiState(it)
    }.asLiveData()

    fun onDoneClick() {
        onDone(productFeatures.value)
    }

    fun onProductFeaturesUpdated(features: String) {
        productFeatures.value = features
    }

    fun onChangeToneClicked() {
        _events.tryEmit(NavigateToSetToneBottomSheet)
    }

    override fun close() {
        viewModelScope.cancel()
    }

    data class UiState(
        val productFeatures: String
    )

    object NavigateToSetToneBottomSheet : Event()
}
