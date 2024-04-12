package com.woocommerce.android.ui.products.ai.description

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.Companion.PRODUCT_DESCRIPTION_FEATURE
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.AI_IDENTIFY_LANGUAGE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_AI_FEEDBACK
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_APPLY_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_COPY_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_GENERATE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_GENERATION_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_GENERATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_IS_RETRY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_IS_USEFUL
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PRODUCT_DESCRIPTION
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState.GenerationState.Celebration
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState.GenerationState.Generated
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState.GenerationState.Generating
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState.GenerationState.Regenerating
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState.GenerationState.Start
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("EmptyFunctionBlock", "MagicNumber", "UnusedPrivateMember", "TooManyFunctions")
@HiltViewModel
class AIProductDescriptionViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val tracker: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    val navArgs = AIProductDescriptionBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _viewState = MutableStateFlow(
        ViewState(
            productTitle = navArgs.productTitle,
            features = navArgs.productDescription ?: "",
            isProductTitleInitiallyPresent = navArgs.productTitle.isNotEmpty()
        )
    )
    val viewState = _viewState.asLiveData()

    private suspend fun identifyLanguage(): Result<String> {
        return aiRepository.identifyISOLanguageCode(
            text = "${navArgs.productTitle} ${_viewState.value.features}",
            feature = PRODUCT_DESCRIPTION_FEATURE
        ).fold(
            onSuccess = { languageISOCode ->
                handleIdentificationSuccess(languageISOCode)
                Result.success(languageISOCode)
            },
            onFailure = { exception ->
                handleIdentificationFailure(exception as JetpackAICompletionsException)
                Result.failure(exception)
            }
        )
    }

    private fun handleIdentificationSuccess(languageISOCode: String) {
        _viewState.update {
            _viewState.value.copy(
                identifiedLanguageISOCode = languageISOCode
            )
        }

        tracker.track(
            stat = AI_IDENTIFY_LANGUAGE_SUCCESS,
            properties = mapOf(
                KEY_SOURCE to VALUE_PRODUCT_DESCRIPTION
            )
        )
    }

    private fun handleIdentificationFailure(error: JetpackAICompletionsException) {
        tracker.track(
            AnalyticsEvent.AI_IDENTIFY_LANGUAGE_FAILED,
            mapOf(
                KEY_ERROR_CONTEXT to this::class.java.simpleName,
                KEY_ERROR_TYPE to error.errorType,
                KEY_ERROR_DESC to error.errorMessage,
                KEY_SOURCE to VALUE_PRODUCT_DESCRIPTION
            )
        )

        resetGenerationState()
    }

    private suspend fun generateProductDescriptionText(languageISOCode: String) {
        val result = aiRepository.generateProductDescription(
            productName = navArgs.productTitle,
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

        resetGenerationState()
    }

    private fun resetGenerationState() {
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
        handleGenerateButtonClick(postClickUIState = Regenerating)
    }

    fun onGenerateButtonClicked() {
        handleGenerateButtonClick(postClickUIState = Generating)
    }

    // For now function only handles `postClickUIState` of `Regenerating` and `Generating`.
    private fun handleGenerateButtonClick(postClickUIState: ViewState.GenerationState) {
        if (!_viewState.value.canGenerateWithAI) {
            _viewState.update {
                _viewState.value.copy(shouldShowErrorOutlineIfEmpty = true)
            }
            return
        }

        val isRetry = when (postClickUIState) {
            Regenerating -> true
            Generating -> false
            else -> false // default to false if other states are added in the future
        }

        tracker.track(
            stat = PRODUCT_DESCRIPTION_AI_GENERATE_BUTTON_TAPPED,
            properties = mapOf(
                KEY_IS_RETRY to isRetry
            )
        )

        _viewState.update { _viewState.value.copy(generationState = postClickUIState) }

        launch {
            val languageISOCode = _viewState.value.identifiedLanguageISOCode ?: identifyLanguage().getOrNull()
            languageISOCode?.let { generateProductDescriptionText(it) }
        }
    }

    fun onFeaturesChanged(features: String) {
        _viewState.update {
            _viewState.value.copy(
                features = features,
                shouldShowErrorOutlineIfEmpty = features.isEmpty()
            )
        }
    }

    fun onTitleChanged(title: String) {
        _viewState.update {
            _viewState.value.copy(
                productTitle = title,
                shouldShowErrorOutlineIfEmpty = title.isEmpty()
            )
        }
    }

    fun onApplyButtonClicked() {
        tracker.track(PRODUCT_DESCRIPTION_AI_APPLY_BUTTON_TAPPED)

        if (appPrefsWrapper.wasAIProductDescriptionCelebrationShown) {
            triggerEvent(ExitWithResult(Pair(_viewState.value.description, _viewState.value.productTitle)))
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
        triggerEvent(ExitWithResult(Pair(_viewState.value.description, _viewState.value.productTitle)))
    }

    fun onDescriptionFeedbackReceived(isUseful: Boolean) {
        tracker.track(
            stat = PRODUCT_AI_FEEDBACK,
            properties = mapOf(
                KEY_SOURCE to VALUE_PRODUCT_DESCRIPTION,
                KEY_IS_USEFUL to isUseful
            )
        )

        // If the user says the description is not useful, we should try identifying language again.
        if (!isUseful) {
            _viewState.update { _viewState.value.copy(identifiedLanguageISOCode = null) }
        }
    }

    data class ViewState(
        val productTitle: String = "",
        val features: String = "",
        val description: String = "",
        val identifiedLanguageISOCode: String? = null,
        val generationState: GenerationState = Start(),
        val isProductTitleInitiallyPresent: Boolean,
        val shouldShowErrorOutlineIfEmpty: Boolean = false
    ) {
        val canGenerateWithAI: Boolean
            get() = if (isProductTitleInitiallyPresent) {
                features.isNotEmpty()
            } else {
                productTitle.isNotEmpty() && features.isNotEmpty()
            }

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
