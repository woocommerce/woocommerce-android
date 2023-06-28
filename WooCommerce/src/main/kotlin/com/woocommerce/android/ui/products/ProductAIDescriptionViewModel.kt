package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
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

@HiltViewModel
class ProductAIDescriptionViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val tracker: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asLiveData()

    init {
//        tracker.track(AnalyticsEvent.PRODUCT_SHARING_AI_DISPLAYED)
    }

    private fun generateDescription() {
        launch {
            val result = aiRepository.generateProductDescription(
                site = selectedSite.get(),
                _viewState.value.productTitle,
                _viewState.value.features
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
    }

    fun onGenerateButtonClicked() {
        _viewState.update { it.copy(generationState = GenerationState.Generating) }

        launch {
            delay(3000)
            _viewState.update { it.copy(generationState = Generated) }
        }

//        generateDescription()

//        tracker.track(
//            AnalyticsEvent.PRODUCT_SHARING_AI_GENERATE_TAPPED,
//            mapOf(
//                AnalyticsTracker.KEY_IS_RETRY to isRetry
//            )
//        )
    }

    fun onRegenerateButtonClicked() {
        _viewState.update { it.copy(generationState = Regenerating) }

        launch {
            delay(3000)
            _viewState.update { it.copy(generationState = Generated) }
        }

//        generateDescription()
    }

    private fun handleCompletionsSuccess(description: String) {
//        tracker.track(AnalyticsEvent.PRODUCT_SHARING_AI_MESSAGE_GENERATED)

        _viewState.update {
            it.copy(
                description = description,
                generationState = Generated
            )
        }
    }

    private fun handleCompletionsFailure(error: JetpackAICompletionsException) {
        tracker.track(
            AnalyticsEvent.PRODUCT_SHARING_AI_MESSAGE_GENERATION_FAILED,
            mapOf(
                AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                AnalyticsTracker.KEY_ERROR_TYPE to error.errorType,
                AnalyticsTracker.KEY_ERROR_DESC to error.errorMessage
            )
        )

        if (_viewState.value.generationState is GenerationState.Generating) {
            _viewState.update {
                // This is to return the previous button's state before generating.
                it.copy(
                    generationState = Initial
                )
            }
        } else if (_viewState.value.generationState is Regenerating) {
            _viewState.update {
                it.copy(
                    generationState = Generated
                )
            }
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

    fun onDialogDismissed() {
//        tracker.track(AnalyticsEvent.PRODUCT_SHARING_AI_DISMISSED)
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
