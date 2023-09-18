package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.Closeable
import javax.inject.Inject

@HiltViewModel
class AddProductWithAIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedState = savedStateHandle) {
    private val step = savedStateHandle.getStateFlow(viewModelScope, Step.ProductName)
    private val subViewModels = listOf<AddProductWithAISubViewModel<*>>(
        ProductNameSubViewModel(
            savedStateHandle = savedStateHandle,
            onDone = {
                // Pass the name to next ViewModel if needed
                step.value = Step.AboutProduct
            }
        ),
    )

    val state = step.map {
        State(
            progress = (it.ordinal + 1).toFloat() / Step.values().size,
            subViewModel = subViewModels[it.ordinal]
        )
    }.asLiveData()

    init {
        wireSubViewModels()
    }

    fun onDismissClick() {
        triggerEvent(Exit)
    }

    private fun wireSubViewModels() {
        subViewModels.forEach { subViewModel ->
            addCloseable(subViewModel)

            subViewModel.events
                .onEach {
                    triggerEvent(it)
                }.launchIn(viewModelScope)
        }
    }

    data class State(
        val progress: Float,
        val subViewModel: AddProductWithAISubViewModel<*>
    )
}

private enum class Step {
    ProductName, AboutProduct, Preview
}

sealed interface AddProductWithAISubViewModel<T> : Closeable {
    val events: Flow<MultiLiveEvent.Event>
        get() = emptyFlow()
    val onDone: (T) -> Unit
}
