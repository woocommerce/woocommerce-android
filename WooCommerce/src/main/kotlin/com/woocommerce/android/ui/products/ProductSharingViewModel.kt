package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.ai.AIPrompts
import com.woocommerce.android.ai.AIRepository
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

@HiltViewModel
class ProductSharingViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val tracker: AnalyticsTrackerWrapper,
    private val resourceProvider: ResourceProvider,
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
            val result = aiRepository.fetchJetpackAICompletionsForSite(
                site = selectedSite.get(),
                prompt = AIPrompts.generateProductSharingPrompt(
                    navArgs.productName,
                    navArgs.permalink,
                    navArgs.productDescription.orEmpty()
                )
            )
            result.fold(
                onSuccess = { completions ->
                    _viewState.update {
                        it.copy(
                            buttonState = AIButtonState.Regenerate(labelForRegenerate),
                            shareMessage = completions,
                            isGenerating = false
                        )
                    }
                },
                onFailure = {
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
        val subject = _viewState.value.shareMessage.ifEmpty { navArgs.productName } + "\n" + navArgs.permalink
        triggerEvent(
            ProductNavigationTarget.ShareProductWithMessage(
                title = navArgs.productName,
                subject = subject
            )
        )
    }

    fun onInfoButtonClicked() {
        triggerEvent(
            LaunchUrlInChromeTab(AppUrls.AUTOMATTIC_AI_GUIDELINES)
        )
    }

    data class ProductSharingViewState(
        val productTitle: String,
        val shareMessage: String = "",
        val buttonState: AIButtonState,
        val isGenerating: Boolean = false,
        val errorMessage: String = ""
    )

    sealed class AIButtonState(val label: String) {
        data class WriteWithAI(val text: String) : AIButtonState(text)
        data class Regenerate(val text: String) : AIButtonState(text)
        data class Generating(val text: String) : AIButtonState(text)
    }
}
