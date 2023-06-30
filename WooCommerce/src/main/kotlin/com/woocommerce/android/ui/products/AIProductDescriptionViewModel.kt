package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Celebration
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Generated
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Generating
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Regenerating
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Start
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
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

    private val _viewState = MutableStateFlow(ViewState(productTitle = navArgs.productTitle))
    val viewState = _viewState.asLiveData()

    fun onGenerateButtonClicked() {
        _viewState.update { _viewState.value.copy(generationState = Generating) }

        launch {
            delay(3000)
            _viewState.update { _viewState.value.copy(generationState = Generated) }
        }
    }

    fun onRegenerateButtonClicked() {
        _viewState.update { _viewState.value.copy(generationState = Regenerating) }

        launch {
            delay(3000)
            _viewState.update { _viewState.value.copy(generationState = Generated) }
        }
    }

    fun onFeaturesChanged(features: String) {
        _viewState.update { _viewState.value.copy(features = features) }
    }

    fun onApplyButtonClicked() {
        if (appPrefsWrapper.wasAIProductDescriptionCelebrationShown) {
            triggerEvent(Exit)
        } else {
            _viewState.update { _viewState.value.copy(generationState = Celebration) }
            appPrefsWrapper.wasAIProductDescriptionCelebrationShown = true
        }
    }

    fun onCopyButtonClicked() {
    }

    fun onCelebrationButtonClicked() {
        triggerEvent(Exit)
    }

    fun onDescriptionFeedbackReceived(isPositive: Boolean) {
    }

    data class ViewState(
        val productTitle: String? = null,
        val features: String = "",
        val description: String = "This stylish and comfortable set is designed to enhance your performance and " +
            "keep you looking and feeling great during your workouts. Upgrade your fitness game and " +
            "make a statement with the \"Fit Fashionista\" activewear set.",
        val generationState: GenerationState = Start
    ) {
        sealed class GenerationState {
            object Start : GenerationState()
            object Generating : GenerationState()
            object Generated : GenerationState()
            object Regenerating : GenerationState()
            object Celebration : GenerationState()
        }
    }
}
