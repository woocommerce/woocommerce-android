package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_AI_FEEDBACK
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_APPLY_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_COPY_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_GENERATE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_GENERATION_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_GENERATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_IS_RETRY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_IS_USEFUL
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PRODUCT_DESCRIPTION
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Celebration
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Generated
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Generating
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Regenerating
import com.woocommerce.android.ui.products.AIProductDescriptionViewModel.ViewState.GenerationState.Start
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("EmptyFunctionBlock", "MagicNumber", "UnusedPrivateMember")
@HiltViewModel
class AIProductDescriptionViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val tracker: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    val navArgs = AIProductDescriptionBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _viewState = MutableStateFlow(
        ViewState(
            productTitle = navArgs.productTitle,
            features = navArgs.productDescription ?: ""
        )
    )
    val viewState = _viewState.asLiveData()

    fun onGenerateButtonClicked() {
        tracker.track(
            stat = PRODUCT_DESCRIPTION_AI_GENERATE_BUTTON_TAPPED,
            properties = mapOf(
                KEY_IS_RETRY to false
            )
        )

        _viewState.update { _viewState.value.copy(generationState = Generating) }

        launch {
            generateDescription()
        }
    }

    private suspend fun generateDescription() {
        aiRepository.identifyISOLanguageCode(
            site = selectedSite.get(),
            text = "${navArgs.productTitle} ${_viewState.value.features}"
        ).fold(
            onSuccess = { languageISOCode ->
                val result = aiRepository.generateProductDescription(
                    site = selectedSite.get(),
                    productName = navArgs.productTitle ?: "",
                    features = _viewState.value.features,
                    languageISOCode = languageISOCode
                )
                result.fold(
                    onSuccess = { completions ->
                        handleCompletionsSuccess(completions)
                    },
                    onFailure = { exception ->
                        handleCompletionsFailure(exception as JetpackAICompletionsException)
                    }
                )
            },
            onFailure = { exception ->
                handleCompletionsFailure(exception as JetpackAICompletionsException)
            }
        )
    }

    private fun handleCompletionsSuccess(completions: String) {
        tracker.track(PRODUCT_DESCRIPTION_AI_GENERATION_SUCCESS)

        _viewState.update {
            _viewState.value.copy(
                description = completions,
                generationState = Generated()
            )
        }
    }

    private fun handleCompletionsFailure(error: JetpackAICompletionsException) {
        tracker.track(
            stat = PRODUCT_DESCRIPTION_AI_GENERATION_FAILED,
            properties = mapOf(KEY_ERROR to error.message)
        )

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
        tracker.track(
            stat = PRODUCT_DESCRIPTION_AI_GENERATE_BUTTON_TAPPED,
            properties = mapOf(
                KEY_IS_RETRY to true
            )
        )

        _viewState.update { _viewState.value.copy(generationState = Regenerating) }

        launch {
            generateDescription()
        }
    }

    fun onFeaturesChanged(features: String) {
        _viewState.update { _viewState.value.copy(features = features) }
    }

    fun onApplyButtonClicked() {
        tracker.track(PRODUCT_DESCRIPTION_AI_APPLY_BUTTON_TAPPED)

        if (appPrefsWrapper.wasAIProductDescriptionCelebrationShown) {
            triggerEvent(ExitWithResult(_viewState.value.description))
        } else {
            _viewState.update { _viewState.value.copy(generationState = Celebration) }
            appPrefsWrapper.wasAIProductDescriptionCelebrationShown = true
        }
    }

    fun onCopyButtonClicked() {
        tracker.track(PRODUCT_DESCRIPTION_AI_COPY_BUTTON_TAPPED)

        triggerEvent(CopyDescriptionToClipboard(_viewState.value.description))
    }

    fun onCelebrationButtonClicked() {
        triggerEvent(ExitWithResult(_viewState.value.description))
    }

    fun onDescriptionFeedbackReceived(isUseful: Boolean) {
        tracker.track(
            stat = PRODUCT_AI_FEEDBACK,
            properties = mapOf(
                KEY_SOURCE to VALUE_PRODUCT_DESCRIPTION,
                KEY_IS_USEFUL to isUseful
            )
        )
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
