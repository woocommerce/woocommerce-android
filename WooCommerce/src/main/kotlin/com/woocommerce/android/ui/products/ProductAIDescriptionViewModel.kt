package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductAIDescriptionViewModel.GenerationState.Generated
import com.woocommerce.android.ui.products.ProductAIDescriptionViewModel.GenerationState.Regenerating
import com.woocommerce.android.ui.products.ProductAIDescriptionViewModel.GenerationState.Start
import com.woocommerce.android.ui.products.ProductAIDescriptionViewModel.ViewState.Celebration
import com.woocommerce.android.ui.products.ProductAIDescriptionViewModel.ViewState.Dismissed
import com.woocommerce.android.ui.products.ProductAIDescriptionViewModel.ViewState.GenerationFlow
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _viewState = MutableStateFlow<ViewState>(GenerationFlow())
    val viewState = _viewState.asStateFlow()

    fun onGenerateButtonClicked() {
        _viewState.update { GenerationFlow().copy(generationState = GenerationState.Generating) }

        launch {
            delay(3000)
            _viewState.update { GenerationFlow().copy(generationState = Generated) }
        }
    }

    fun onRegenerateButtonClicked() {
        _viewState.update { GenerationFlow().copy(generationState = Regenerating) }

        launch {
            delay(3000)
            _viewState.update { GenerationFlow().copy(generationState = Generated) }
        }
    }

    fun onFeaturesChanged(features: String) {
        _viewState.update {
            GenerationFlow().copy(features = features)
        }
    }

    fun onApplyButtonClicked() {
        _viewState.update { Celebration }
    }

    fun onCopyButtonClicked() {
    }

    fun onCelebrationConfirmClicked() {
        _viewState.update { Dismissed }
    }

    fun onDescriptionFeedbackReceived(isPositive: Boolean) {
    }

    sealed class ViewState {
        data class GenerationFlow(
            val productTitle: String = "",
            val features: String = "",
            val description: String = "This stylish and comfortable set is designed to enhance your performance and " +
                "keep you looking and feeling great during your workouts. Upgrade your fitness game and " +
                "make a statement with the \"Fit Fashionista\" activewear set.",
            val generationState: GenerationState = Start
        ) : ViewState()

        object Celebration : ViewState()

        object Dismissed : ViewState()
    }

    sealed class GenerationState {
        object Start : GenerationState()
        object Generating : GenerationState()
        object Generated : GenerationState()
        object Regenerating : GenerationState()
    }
}
