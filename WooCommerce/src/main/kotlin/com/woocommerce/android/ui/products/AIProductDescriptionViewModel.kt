package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.GenerationState.Generated
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.GenerationState.Regenerating
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.GenerationState.Start
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.Celebration
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.Dismissed
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationFlow
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("EmptyFunctionBlock", "MagicNumber", "UnusedPrivateMember", "UNUSED_PARAMETER")
@HiltViewModel
class AIProductDescriptionViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val tracker: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    val navArgs = AIProductDescriptionBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _viewState = MutableStateFlow<ViewState>(GenerationFlow(productTitle = navArgs.productTitle))
    val viewState = _viewState.asLiveData()

    private val generationFlow
        get() = _viewState.value as? GenerationFlow ?: GenerationFlow()

    fun onGenerateButtonClicked() {
        _viewState.update { generationFlow.copy(generationState = GenerationState.Generating) }

        launch {
            delay(3000)
            _viewState.update { generationFlow.copy(generationState = Generated) }
        }
    }

    fun onRegenerateButtonClicked() {
        _viewState.update { generationFlow.copy(generationState = Regenerating) }

        launch {
            delay(3000)
            _viewState.update { generationFlow.copy(generationState = Generated) }
        }
    }

    fun onFeaturesChanged(features: String) {
        _viewState.update { generationFlow.copy(features = features) }
    }

    fun onApplyButtonClicked() {
        if (appPrefsWrapper.wasAIProductDescriptionCelebrationShown) {
            _viewState.update { Dismissed }
        } else {
            _viewState.update { Celebration }
            appPrefsWrapper.wasAIProductDescriptionCelebrationShown = true
        }
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
            val productTitle: String? = null,
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
