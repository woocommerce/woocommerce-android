package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductAIDescriptionViewModel.GenerationState.Generated
import com.woocommerce.android.ui.products.ProductAIDescriptionViewModel.GenerationState.Initial
import com.woocommerce.android.ui.products.ProductAIDescriptionViewModel.GenerationState.Regenerating
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("EmptyFunctionBlock", "MagicNumber", "UnusedPrivateMember", "UNUSED_PARAMETER")
@HiltViewModel
class ProductAIDescriptionViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val tracker: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asLiveData()

    fun onGenerateButtonClicked() {
        _viewState.update { it.copy(generationState = GenerationState.Generating) }

        launch {
            delay(3000)
            _viewState.update { it.copy(generationState = Generated) }
        }
    }

    fun onRegenerateButtonClicked() {
        _viewState.update { it.copy(generationState = Regenerating) }

        launch {
            delay(3000)
            _viewState.update { it.copy(generationState = Generated) }
        }
    }

    fun onFeaturesChanged(features: String) {
        _viewState.update {
            it.copy(features = features)
        }
    }

    fun onApplyButtonClicked() {
    }

    fun onCopyButtonClicked() {
    }

    fun onDescriptionFeedbackReceived(isPositive: Boolean) {
    }

    data class ViewState(
        val productTitle: String = "",
        val features: String = "",
        val description: String = "This stylish and comfortable set is designed to enhance your performance and " +
            "keep you looking and feeling great during your workouts. Upgrade your fitness game and " +
            "make a statement with the \"Fit Fashionista\" activewear set.",
        val isDismissed: Boolean = false,
        val generationState: GenerationState = Initial
    )

    sealed class GenerationState {
        object Initial : GenerationState()
        object Generating : GenerationState()
        object Generated : GenerationState()
        object Regenerating : GenerationState()
    }
}
