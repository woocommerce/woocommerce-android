package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class AIPriceAdvisorViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asLiveData()

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
        data class Generated(val showError: Boolean = false) : GenerationState()
        object Regenerating : GenerationState()
        object Failed : GenerationState()
    }
}
