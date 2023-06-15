package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ai.AIPrompts
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductSharingViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: ProductSharingDialogArgs by savedStateHandle.navArgs()

    private val _viewState = MutableStateFlow(
        ProductSharingViewState(
            productTitle = navArgs.productName,
            buttonState = AIButtonState.WriteWithAI(
                resourceProvider.getString(R.string.product_sharing_write_with_ai)
            )
        )
    )
    val viewState = _viewState.asLiveData()

    fun onGenerateButtonClicked() {
        _viewState.update {
            it.copy(
                buttonState = AIButtonState.Generating(
                    resourceProvider.getString(R.string.product_sharing_generating)
                ),
                isGenerating = true
            )
        }

        launch {
            val result = aiRepository.fetchJetpackAICompletionsForSite(
                site = selectedSite.get(),
                prompt = AIPrompts.generateProductSharingPrompt(
                    navArgs.productName,
                    navArgs.permalink,
                    navArgs.productDescription.orEmpty()
                )
            )
            result.fold(
                onSuccess = { completions ->
                    _viewState.update {
                        it.copy(
                            buttonState = AIButtonState.Regenerate(
                                resourceProvider.getString(R.string.product_sharing_regenerate)
                            ),
                            shareMessage = completions,
                            isGenerating = false
                        )
                    }
                },
                onFailure = {
                    // error handling
                }
            )
        }
    }

    fun onShareMessageEdited(message: String) {
        _viewState.update { state ->
            state.copy(shareMessage = message)
        }
    }
    data class ProductSharingViewState(
        val productTitle: String,
        val shareMessage: String = "",
        val buttonState: AIButtonState,
        val isGenerating: Boolean = false
    )

    sealed class AIButtonState(val label: String) {
        data class WriteWithAI(val text: String) : AIButtonState(text)
        data class Regenerate(val text: String) : AIButtonState(text)
        data class Generating(val text: String) : AIButtonState(text)
    }
}
