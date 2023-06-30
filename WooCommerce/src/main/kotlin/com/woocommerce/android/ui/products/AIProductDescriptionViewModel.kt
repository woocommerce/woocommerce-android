package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Celebration
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Generated
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Generating
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Regenerating
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Start
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
            generateDescription()
        }
    }

    private suspend fun generateDescription() {
        val result = aiRepository.generateProductDescription(
            site = selectedSite.get(),
            productName = navArgs.productTitle ?: "",
            features = _viewState.value.features
        )
        result.fold(
            onSuccess = { completions ->
                handleCompletionsSuccess(completions)
            },
            onFailure = { exception ->
                handleCompletionsFailure(exception as JetpackAICompletionsException)
            }
        )
    }

    private fun handleCompletionsSuccess(completions: String) {
        _viewState.update {
            _viewState.value.copy(
                description = completions,
                generationState = Generated()
            )
        }
    }

    private fun handleCompletionsFailure(error: JetpackAICompletionsException) {
        // This is to return the previous state before generating.
        val previousState = if (_viewState.value.generationState == Generating) {
            Start(showError = true)
        } else {
            Generated(showError = true)
        }
        _viewState.update {
            _viewState.value.copy(generationState = previousState)
        }
    }

    fun onRegenerateButtonClicked() {
        _viewState.update { _viewState.value.copy(generationState = Regenerating) }

        launch {
            generateDescription()
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
        triggerEvent(CopyDescriptionToClipboard(_viewState.value.description))
    }

    fun onCelebrationButtonClicked() {
        triggerEvent(Exit)
    }

    fun onDescriptionFeedbackReceived(isPositive: Boolean) {
    }

    data class ViewState(
        val productTitle: String? = null,
        val features: String = "",
        val description: String = "",
        val generationState: GenerationState = Start()
    ) {
        sealed class GenerationState {
            data class Start(val showError: Boolean = false) : GenerationState()
            object Generating : GenerationState()
            data class Generated(val showError: Boolean = false) : GenerationState()
            object Regenerating : GenerationState()
            object Celebration : GenerationState()
        }
    }

    data class CopyDescriptionToClipboard(val description: String) : Event()
}
