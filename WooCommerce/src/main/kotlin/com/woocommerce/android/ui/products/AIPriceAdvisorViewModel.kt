package com.woocommerce.android.ui.products

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
class AIPriceAdvisorViewModel @Inject constructor(
    private val site: SelectedSite,
    private val aiRepository: AIRepository,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    val navArgs = AIPriceAdvisorFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asLiveData()

    init {
        launch {
            startPriceAdviceGeneration()
        }
    }

    private suspend fun startPriceAdviceGeneration() {
        val adviceType = navArgs.adviceTypeValue

        if (adviceType == AdviceType.SALE_PRICE.value) {
            generateSalePriceAdvice()
        }
    }

    private suspend fun generateSalePriceAdvice() {
        val result = aiRepository.generateSalesPriceAdvice(
            site = site.get(),
            currentPrice = navArgs.currentPrice,
            currency = navArgs.currency,
            productName = navArgs.productName,
            productDescription = navArgs.productDescription,
            countryCode = navArgs.countryCode,
            stateCode = navArgs.stateCode
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
            it.copy(
                generatedAdvice = completions,
                generationState = GenerationState.Generated
            )
        }
    }

    private fun handleCompletionsFailure(exception: AIRepository.JetpackAICompletionsException) {
        WooLog.e(WooLog.T.AI, "Failed to generate price advice: ${exception.message}")

        _viewState.update {
            it.copy(
                generationState = GenerationState.Failed
            )
        }
    }

    fun onRetryButtonClick() {
        _viewState.update {
            it.copy(
                generationState = GenerationState.Regenerating
            )
        }
        launch {
            startPriceAdviceGeneration()
        }
    }

    enum class AdviceType(val value: Int) {
        REGULAR_PRICE(0),
        SALE_PRICE(1)
    }
    data class ViewState(
        val generatedAdvice: String = "",
        val generationState: GenerationState = GenerationState.Generating
    )

    sealed class GenerationState {
        object Generating : GenerationState()
        object Generated : GenerationState()
        object Regenerating : GenerationState()
        object Failed : GenerationState()
    }
}
