package com.woocommerce.android.ui.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_DETECTED_LANGUAGE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_IS_USEFUL
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_ORDER_THANK_YOU_NOTE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class AIThankYouNoteViewModel @Inject constructor(
    private val site: SelectedSite,
    private val aiRepository: AIRepository,
    private val tracker: AnalyticsTrackerWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    val navArgs = AIThankYouNoteBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asLiveData()

    init {
        tracker.track(AnalyticsEvent.ORDER_THANK_YOU_NOTE_SHOWN)

        launch {
            startThankYouNoteCreation()
        }
    }

    private suspend fun startThankYouNoteCreation() {
        val languageISOCode = _viewState.value.identifiedLanguageISOCode
            ?: identifyLanguage().getOrNull()
        if (languageISOCode != null) {
            createThankYouNote(languageISOCode = languageISOCode)
        }
    }

    private suspend fun createThankYouNote(languageISOCode: String) {
        val result = aiRepository.generateOrderThankYouNote(
            site = site.get(),
            customerName = navArgs.customerName,
            productName = navArgs.productName,
            productDescription = navArgs.productDescription,
            languageISOCode = languageISOCode
        )

        result.fold(
            onSuccess = { completions ->
                handleCompletionsSuccess(completions)
            },
            onFailure = { exception ->
                handleCompletionsFailure(exception as AIRepository.JetpackAICompletionsException)
            }
        )
    }

    private fun handleCompletionsSuccess(completions: String) {
        tracker.track(AnalyticsEvent.ORDER_THANK_YOU_NOTE_GENERATION_SUCCESS)
        _viewState.update {
            _viewState.value.copy(
                generatedThankYouNote = completions,
                generationState = GenerationState.Generated()
            )
        }
    }

    private fun handleCompletionsFailure(error: AIRepository.JetpackAICompletionsException) {
        tracker.track(
            stat = AnalyticsEvent.ORDER_THANK_YOU_NOTE_GENERATION_FAILED,
            properties = mapOf(KEY_ERROR to error.message)
        )

        _viewState.update {
            _viewState.value.copy(
                generationState = GenerationState.Failed
            )
        }
    }

    fun onRegenerateButtonClicked() {
        tracker.track(AnalyticsEvent.ORDER_THANK_YOU_NOTE_REGENERATE_TAPPED)
        _viewState.update {
            _viewState.value.copy(
                generationState = GenerationState.Regenerating
            )
        }
        launch {
            startThankYouNoteCreation()
        }
    }

    fun onDescriptionFeedbackReceived(isUseful: Boolean) {
        tracker.track(
            stat = AnalyticsEvent.PRODUCT_AI_FEEDBACK,
            properties = mapOf(
                KEY_SOURCE to VALUE_ORDER_THANK_YOU_NOTE,
                KEY_IS_USEFUL to isUseful
            )
        )

        // If the user says the description is not useful, we should try identifying language again.
        if (!isUseful) {
            _viewState.update { _viewState.value.copy(identifiedLanguageISOCode = null) }
        }
    }

    fun onCopyButtonClicked() {
        triggerEvent(CopyDescriptionToClipboard(_viewState.value.generatedThankYouNote))
    }

    fun onShareButtonClicked() {
        val messageToShare = _viewState.value.generatedThankYouNote

        tracker.track(AnalyticsEvent.ORDER_THANK_YOU_NOTE_SHARE_TAPPED)

        triggerEvent(ShareNote(messageToShare))
    }

    private suspend fun identifyLanguage(): Result<String> {
        return aiRepository.identifyISOLanguageCode(
            site = site.get(),
            text = "${navArgs.productName} ${navArgs.productDescription.orEmpty()}",
            feature = AIRepository.ORDER_DETAIL_THANK_YOU_NOTE
        ).fold(
            onSuccess = { languageISOCode ->
                handleIdentificationSuccess(languageISOCode)
                Result.success(languageISOCode)
            },
            onFailure = { exception ->
                handleIdentificationFailure(exception as AIRepository.JetpackAICompletionsException)
                Result.failure(exception)
            }
        )
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
                KEY_DETECTED_LANGUAGE to languageISOCode,
                KEY_SOURCE to VALUE_ORDER_THANK_YOU_NOTE
            )
        )
    }

    private fun handleIdentificationFailure(error: AIRepository.JetpackAICompletionsException) {
        tracker.track(
            AnalyticsEvent.AI_IDENTIFY_LANGUAGE_FAILED,
            mapOf(
                KEY_ERROR_CONTEXT to this::class.java.simpleName,
                KEY_ERROR_TYPE to error.errorType,
                KEY_ERROR_DESC to error.errorMessage,
                KEY_SOURCE to VALUE_ORDER_THANK_YOU_NOTE
            )
        )

        _viewState.update {
            _viewState.value.copy(
                generationState = GenerationState.Failed
            )
        }
    }

    data class ViewState(
        val generatedThankYouNote: String = "",
        val generationState: GenerationState = GenerationState.Generating,
        val identifiedLanguageISOCode: String? = null
    )

    sealed class GenerationState {
        object Generating : GenerationState()
        data class Generated(val showError: Boolean = false) : GenerationState()
        object Regenerating : GenerationState()
        object Failed : GenerationState()
    }

    data class CopyDescriptionToClipboard(val description: String) : MultiLiveEvent.Event()
    data class ShareNote(val note: String) : MultiLiveEvent.Event()
}
