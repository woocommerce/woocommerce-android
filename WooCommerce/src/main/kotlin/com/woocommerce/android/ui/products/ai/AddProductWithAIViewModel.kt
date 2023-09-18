package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class AddProductWithAIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedState = savedStateHandle) {
    private val step = savedStateHandle.getStateFlow(viewModelScope, Step.ProductName)
    private val saveButtonState = MutableStateFlow(SaveButtonState.Hidden)

    private val subViewModels = listOf<AddProductWithAISubViewModel<*>>(
        ProductNameSubViewModel(
            savedStateHandle = savedStateHandle,
            onDone = {
                // Pass the name to next ViewModel if needed
                goToNextStep()
            }
        ),
    )

    val state = combine(step, saveButtonState) { step, saveButtonState ->
        State(
            progress = (step.ordinal + 1).toFloat() / Step.values().size,
            subViewModel = subViewModels[step.ordinal],
            isFirstStep = step.ordinal == 0,
            saveButtonState = saveButtonState
        )
    }.asLiveData()

    init {
        wireSubViewModels()
    }

    fun onBackButtonClick() {
        if (step.value.ordinal == 0) {
            triggerEvent(Exit)
        } else {
            goToPreviousStep()
        }
    }

    private fun goToNextStep() {
        require(step.value.ordinal < Step.values().size - 1)
        step.value = Step.values()[step.value.ordinal + 1]
    }

    private fun goToPreviousStep() {
        require(step.value.ordinal > 0)
        step.value = Step.values()[step.value.ordinal - 1]
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
        val subViewModel: AddProductWithAISubViewModel<*>,
        val isFirstStep: Boolean,
        val saveButtonState: SaveButtonState
    )

    enum class SaveButtonState {
        Hidden, Shown, Loading
    }
}

private enum class Step {
    ProductName, AboutProduct, Preview
}
