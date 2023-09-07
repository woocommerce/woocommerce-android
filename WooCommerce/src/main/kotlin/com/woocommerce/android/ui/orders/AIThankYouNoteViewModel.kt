package com.woocommerce.android.ui.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIThankYouNoteViewModel @Inject constructor(
    private val site: SelectedSite,
    private val aiRepository: AIRepository,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    val navArgs = AIThankYouNoteBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asLiveData()

    init {
        launch {
            createThankYouNote()
        }
    }

    private suspend fun createThankYouNote() {
        val result = aiRepository.generateOrderThankYouNote(
            site = site.get(),
            customerName = navArgs.customerName,
            productName = navArgs.productName,
            productDescription = navArgs.productDescription
        )

        result.fold(
            onSuccess = { completions ->
                handleCompletionsSuccess(completions)
            },
            onFailure = { exception ->
                handleCompletionsFailure(exception as AIRepository.JetpackAICompletionsException)
            }
        )
    }

    private fun handleCompletionsSuccess(completions: String) {
        _viewState.update {
            _viewState.value.copy(
                generatedThankYouNote = completions
            )
        }
    }

    private fun handleCompletionsFailure(error: AIRepository.JetpackAICompletionsException) {
        // do something about it
        WooLog.e(WooLog.T.AI, "Failed to generate thank you note", error)
    }
    data class ViewState(
        val generatedThankYouNote: String = "",
        val generationState: GenerationState = GenerationState.Start(),
    )

    sealed class GenerationState {
        data class Start(val showError: Boolean = false) : GenerationState()
        object Generating : GenerationState()
        data class Generated(val showError: Boolean = false) : GenerationState()
        object Regenerating : GenerationState()
    }
}
