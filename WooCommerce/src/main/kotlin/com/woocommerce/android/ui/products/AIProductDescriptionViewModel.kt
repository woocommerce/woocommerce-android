package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.FlowState.Celebration
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.FlowState.Generated
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.FlowState.Generating
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.FlowState.Regenerating
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.FlowState.Start
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
        _viewState.update { _viewState.value.copy(flowState = Generating) }

        launch {
            delay(3000)
            _viewState.update { _viewState.value.copy(flowState = Generated) }
        }
    }

    fun onRegenerateButtonClicked() {
        _viewState.update { _viewState.value.copy(flowState = Regenerating) }

        launch {
            delay(3000)
            _viewState.update { _viewState.value.copy(flowState = Generated) }
        }
    }

    fun onFeaturesChanged(features: String) {
        _viewState.update { _viewState.value.copy(features = features) }
    }

    fun onApplyButtonClicked() {
        if (appPrefsWrapper.wasAIProductDescriptionCelebrationShown) {
            triggerEvent(Exit)
        } else {
            _viewState.update { _viewState.value.copy(flowState = Celebration) }
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
        val flowState: FlowState = Start
    ) {
        sealed class FlowState {
            object Start : FlowState()
            object Generating : FlowState()
            object Generated : FlowState()
            object Regenerating : FlowState()
            object Celebration : FlowState()
        }
    }
}
