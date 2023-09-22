package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.getStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map

class AboutProductSubViewModel(
    savedStateHandle: SavedStateHandle,
    override val onDone: (String) -> Unit
) : AddProductWithAISubViewModel<String> {
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
        // TODO
    }

    override fun close() {
        viewModelScope.cancel()
    }

    data class UiState(
        val productFeatures: String
    )
}
