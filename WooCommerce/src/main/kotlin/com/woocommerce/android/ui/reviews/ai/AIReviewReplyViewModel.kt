package com.woocommerce.android.ui.reviews.ai

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIReviewReplyViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs = AIReviewReplyFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState

    private suspend fun generateSuggestions() {
        _viewState.update {
            it.copy(
                showOverlay = true,
                generationState = GenerationState.Generating
            )
        }

        val languageISOCode = aiRepository.identifyISOLanguageCode(
            text = "${navArgs.productName} ${navArgs.reviewText}",
            feature = AIRepository.REVIEW_REPLY_FEATURE
        ).getOrDefault("en")

        aiRepository.generateReviewReplySuggestions(
            reviewerName = navArgs.reviewerName,
            rating = navArgs.rating,
            productName = navArgs.productName,
            reviewText = navArgs.reviewText,
            languageISOCode = languageISOCode
        ).fold(
            onSuccess = { suggestions ->
                _viewState.update {
                    it.copy(
                        suggestions = suggestions,
                        generationState = GenerationState.Generated
                    )
                }
            },
            onFailure = { error ->
                if (error.isQuotaExceeded()) {
                    val upgradeUrl = aiRepository.fetchUpgradeUrl()
                    _viewState.update {
                        it.copy(generationState = GenerationState.QuotaExceeded(upgradeUrl))
                    }
                } else {
                    _viewState.update { it.copy(generationState = GenerationState.Failed) }
                }
            }
        )
    }

    private fun Throwable.isQuotaExceeded(): Boolean {
        return this is AIRepository.JetpackAICompletionsException && (
            errorType.equals("API_ERROR", ignoreCase = true) &&
                errorMessage.contains("quota", ignoreCase = true)
            )
    }

    fun onAIButtonClicked() {
        if (_viewState.value.generationState is GenerationState.Generated) {
            _viewState.update { it.copy(showOverlay = true) }
        } else {
            _viewState.update { it.copy(showConfirmationDialog = true) }
        }
    }

    fun onConfirmAIGeneration() {
        _viewState.update { it.copy(showConfirmationDialog = false) }
        launch { generateSuggestions() }
    }

    fun onDismissConfirmationDialog() {
        _viewState.update { it.copy(showConfirmationDialog = false) }
    }

    fun onSuggestionSelected(suggestion: String) {
        _viewState.update {
            it.copy(
                replyText = suggestion,
                showOverlay = false
            )
        }
    }

    fun onDismissOverlay() {
        _viewState.update { it.copy(showOverlay = false) }
    }

    fun onRetryClicked() {
        launch { generateSuggestions() }
    }

    fun onTextChanged(text: String) {
        _viewState.update { it.copy(replyText = text) }
    }

    fun onDonePressed() {
        val text = _viewState.value.replyText
        if (text.isNotBlank()) {
            triggerEvent(ExitWithResult(text))
        }
    }

    fun onBackPressed() {
        if (_viewState.value.showOverlay) {
            _viewState.update { it.copy(showOverlay = false) }
        } else {
            triggerEvent(Exit)
        }
    }

    fun onUpgradeClicked() {
        val state = _viewState.value.generationState
        if (state is GenerationState.QuotaExceeded && state.upgradeUrl != null) {
            triggerEvent(OpenUpgradeUrl(state.upgradeUrl))
        }
        _viewState.update { it.copy(showOverlay = false) }
    }

    data class ViewState(
        val suggestions: List<String> = emptyList(),
        val replyText: String = "",
        val showOverlay: Boolean = false,
        val showConfirmationDialog: Boolean = false,
        val generationState: GenerationState = GenerationState.Idle
    )

    sealed class GenerationState {
        object Idle : GenerationState()
        object Generating : GenerationState()
        object Generated : GenerationState()
        object Failed : GenerationState()
        data class QuotaExceeded(val upgradeUrl: String?) : GenerationState()
    }

    data class OpenUpgradeUrl(val url: String) : MultiLiveEvent.Event()

    companion object {
        const val AI_REVIEW_REPLY_RESULT = "ai-review-reply-result"
    }
}
