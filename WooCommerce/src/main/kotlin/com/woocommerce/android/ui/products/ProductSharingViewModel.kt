package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.LaunchUrlInChromeTab
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class ProductSharingViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val tracker: AnalyticsTrackerWrapper,
    resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: ProductSharingDialogArgs by savedStateHandle.navArgs()

    private val labelForWriteWithAI = resourceProvider.getString(R.string.product_sharing_write_with_ai)
    private val labelForGenerating = resourceProvider.getString(R.string.product_sharing_generating)
    private val labelForRegenerate = resourceProvider.getString(R.string.product_sharing_regenerate)
    private val labelForGeneratingFailure = resourceProvider.getString(R.string.product_sharing_ai_generating_failure)

    private val _viewState = MutableStateFlow(
        ProductSharingViewState(
            productTitle = navArgs.productName,
            buttonState = AIButtonState.WriteWithAI(labelForWriteWithAI)
        )
    )
    val viewState = _viewState.asLiveData()

    init {
        tracker.track(AnalyticsEvent.PRODUCT_SHARING_AI_DISPLAYED)
    }

    fun onGenerateButtonClicked() {
        val isRetry = _viewState.value.buttonState is AIButtonState.Regenerate
        tracker.track(
            AnalyticsEvent.PRODUCT_SHARING_AI_GENERATE_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_IS_RETRY to isRetry
            )
        )

        _viewState.update {
            it.copy(
                buttonState = AIButtonState.Generating(labelForGenerating),
                isGenerating = true,
                errorMessage = ""
            )
        }

        launch {
            val languageISOCode = _viewState.value.identifiedLanguageISOCode
                ?: identifyLanguage().getOrNull()
            if (languageISOCode != null) {
                generateProductSharingText(languageISOCode = languageISOCode)
            }
        }
    }

    private suspend fun identifyLanguage(): Result<String> {
        return aiRepository.identifyISOLanguageCode(
            site = selectedSite.get(),
            text = "${navArgs.productName} ${navArgs.productDescription.orEmpty()}"
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

    private suspend fun generateProductSharingText(languageISOCode: String) {
        val result = aiRepository.generateProductSharingText(
            site = selectedSite.get(),
            navArgs.productName,
            navArgs.permalink,
            navArgs.productDescription.orEmpty(),
            languageISOCode
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
        tracker.track(AnalyticsEvent.PRODUCT_SHARING_AI_MESSAGE_GENERATED)

        _viewState.update {
            it.copy(
                buttonState = AIButtonState.Regenerate(labelForRegenerate),
                shareMessage = completions,
                isGenerating = false,
                shouldShowFeedbackForm = true
            )
        }
    }

    private fun handleIdentificationSuccess(languageISOCode: String) {
        _viewState.update {
            it.copy(
                identifiedLanguageISOCode = languageISOCode
            )
        }

        tracker.track(
            AnalyticsEvent.AI_IDENTIFY_LANGUAGE_SUCCESS,
            mapOf(
                AnalyticsTracker.KEY_DETECTED_LANGUAGE to languageISOCode,
                AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_SHARING
            )
        )
    }

    private fun handleIdentificationFailure(error: JetpackAICompletionsException) {
        tracker.track(
            AnalyticsEvent.AI_IDENTIFY_LANGUAGE_FAILED,
            mapOf(
                AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                AnalyticsTracker.KEY_ERROR_TYPE to error.errorType,
                AnalyticsTracker.KEY_ERROR_DESC to error.errorMessage,
                AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_SHARING
            )
        )

        resetButtonStateAfterFailure()
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

        resetButtonStateAfterFailure()
    }

    private fun resetButtonStateAfterFailure() {
        _viewState.update {
            // This is to return the previous button's state before generating.
            val previousButtonState = if (it.buttonState is AIButtonState.Regenerate) {
                AIButtonState.Regenerate(labelForRegenerate)
            } else {
                AIButtonState.WriteWithAI(labelForWriteWithAI)
            }

            it.copy(
                buttonState = previousButtonState,
                errorMessage = labelForGeneratingFailure,
                isGenerating = false
            )
        }
    }

    fun onShareMessageEdited(message: String) {
        _viewState.update { state ->
            state.copy(
                shareMessage = message,
                errorMessage = ""
            )
        }
    }

    fun onShareButtonClicked() {
        val writtenMessage = _viewState.value.shareMessage
        val messageToShare = writtenMessage.ifEmpty { navArgs.productName } + "\n" + navArgs.permalink

        tracker.track(
            AnalyticsEvent.PRODUCT_SHARING_AI_SHARE_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_WITH_MESSAGE to writtenMessage.isEmpty()
            )
        )

        triggerEvent(
            ProductNavigationTarget.ShareProductWithMessage(
                title = navArgs.productName,
                subject = messageToShare
            )
        )
    }

    fun onInfoButtonClicked() {
        triggerEvent(
            LaunchUrlInChromeTab(AppUrls.AUTOMATTIC_AI_GUIDELINES)
        )
    }

    fun onDialogDismissed() {
        tracker.track(AnalyticsEvent.PRODUCT_SHARING_AI_DISMISSED)
    }

    fun onDescriptionFeedbackReceived(isUseful: Boolean) {
        tracker.track(
            stat = AnalyticsEvent.PRODUCT_AI_FEEDBACK,
            properties = mapOf(
                AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_SHARING,
                AnalyticsTracker.KEY_IS_USEFUL to isUseful
            )
        )

        // If the user says the description is not useful, we should try identifying language again.
        _viewState.update {
            _viewState.value.copy(
                identifiedLanguageISOCode = if (!isUseful) null else _viewState.value.identifiedLanguageISOCode,
                shouldShowFeedbackForm = false
            )
        }
    }

    data class ProductSharingViewState(
        val productTitle: String,
        val shareMessage: String = "",
        val buttonState: AIButtonState,
        val isGenerating: Boolean = false,
        val errorMessage: String = "",
        val identifiedLanguageISOCode: String? = null,
        val shouldShowFeedbackForm: Boolean = false
    )

    sealed class AIButtonState(val label: String) {
        data class WriteWithAI(val text: String) : AIButtonState(text)
        data class Regenerate(val text: String) : AIButtonState(text)
        data class Generating(val text: String) : AIButtonState(text)
    }
}
