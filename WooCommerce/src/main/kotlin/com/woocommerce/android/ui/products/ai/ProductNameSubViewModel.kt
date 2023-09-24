package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.getStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map

class ProductNameSubViewModel(
    savedStateHandle: SavedStateHandle,
    override val onDone: (String) -> Unit
) : AddProductWithAISubViewModel<String> {
    companion object {
        private const val KEY_SUBSCREEN_NAME = "product_name"
    }

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

    override fun close() {
        viewModelScope.cancel()
    }

    data class UiState(
        val name: String
    )
}
