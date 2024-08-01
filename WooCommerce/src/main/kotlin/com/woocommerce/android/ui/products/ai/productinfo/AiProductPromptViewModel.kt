package com.woocommerce.android.ui.products.ai.productinfo

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Image
import com.woocommerce.android.ui.products.ai.AiTone
import com.woocommerce.android.ui.products.ai.TextRecognitionEngine
import com.woocommerce.android.ui.products.ai.components.ImageAction
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import javax.inject.Inject

@HiltViewModel
class AiProductPromptViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val tracker: AnalyticsTrackerWrapper,
    private val textRecognitionEngine: TextRecognitionEngine,
    private val prefs: AppPrefsWrapper,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState = savedStateHandle) {
    companion object {
        private const val SUGGESTIONS_BAR_INITIAL_PROGRESS = 0.05F
        private const val DEFAULT_PROMPT_DELIMITER = " "
    }

    private var isFirstAttempt: Boolean
        get() = savedStateHandle["isFirstAttempt"] ?: true
        set(value) {
            savedStateHandle["isFirstAttempt"] = value
        }

    private val _state = savedStateHandle.getStateFlow(
        viewModelScope,
        AiProductPromptState(
            productPrompt = "",
            selectedTone = prefs.aiContentGenerationTone,
            isMediaPickerDialogVisible = false,
            selectedImage = null,
            isScanningImage = false,
            showImageFullScreen = false,
            noTextDetectedMessage = false,
            promptSuggestionBarState = PromptSuggestionBar(
                progressBarColorRes = R.color.linear_progress_background_gray,
                messageRes = R.string.ai_product_creation_prompt_suggestion_initial,
                progress = SUGGESTIONS_BAR_INITIAL_PROGRESS
            )
        )
    )

    val state = _state.asLiveData()

    fun onBackButtonClick() {
        triggerEvent(Exit)
    }

    fun onPromptUpdated(prompt: String) {
        _state.value = _state.value.copy(productPrompt = prompt)
        updatePromptSuggestionBarState(prompt)
    }

    @Suppress("MagicNumber")
    private fun updatePromptSuggestionBarState(prompt: String) {
        val wordCount = prompt.split(DEFAULT_PROMPT_DELIMITER).size
        val promptSuggestionBarState = when {
            prompt.isEmpty() -> {
                PromptSuggestionBar(
                    progressBarColorRes = R.color.linear_progress_background_gray,
                    messageRes = R.string.ai_product_creation_prompt_suggestion_initial,
                    progress = SUGGESTIONS_BAR_INITIAL_PROGRESS
                )
            }

            wordCount < 8 -> {
                PromptSuggestionBar(
                    progressBarColorRes = R.color.ai_linear_progress_background_more_red,
                    messageRes = R.string.ai_product_creation_prompt_suggestion_add_more_details,
                    progress = 0.2f
                )
            }

            wordCount < 17 -> {
                PromptSuggestionBar(
                    progressBarColorRes = R.color.ai_linear_progress_background_orange,
                    messageRes = R.string.ai_product_creation_prompt_suggestion_getting_better,
                    progress = 0.4f
                )
            }

            wordCount < 27 -> {
                PromptSuggestionBar(
                    progressBarColorRes = R.color.ai_linear_progress_background_yellow,
                    messageRes = R.string.ai_product_creation_prompt_suggestion_almost_there,
                    progress = 0.7f
                )
            }

            else -> {
                PromptSuggestionBar(
                    progressBarColorRes = R.color.ai_linear_progress_background_green,
                    messageRes = R.string.ai_product_creation_prompt_suggestion_great_prompt,
                    progress = 0.9f
                )
            }
        }
        _state.value = _state.value.copy(promptSuggestionBarState = promptSuggestionBarState)
    }

    fun onAddImageForScanning() {
        tracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_STARTED_PACKAGE_PHOTO_SELECTION_FLOW)
        _state.value = _state.value.copy(isMediaPickerDialogVisible = true)
    }

    fun onMediaPickerDialogDismissed() {
        _state.value = _state.value.copy(isMediaPickerDialogVisible = false)
    }

    fun onMediaLibraryRequested(source: DataSource) {
        viewModelScope.launch {
            triggerEvent(ShowMediaDialog(source))
            _state.value = _state.value.copy(isMediaPickerDialogVisible = false)
        }
    }

    fun onGenerateProductClicked() {
        tracker.track(
            AnalyticsEvent.PRODUCT_CREATION_AI_GENERATE_DETAILS_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_IS_FIRST_ATTEMPT to isFirstAttempt,
                AnalyticsTracker.KEY_FEATURE_WORD_COUNT to _state.value.productPrompt.split(" ").size,
            )
        )
        isFirstAttempt = false
        triggerEvent(
            ShowProductPreviewScreen(
                productFeatures = _state.value.productPrompt,
                image = _state.value.selectedImage
            )
        )
    }

    fun onToneSelected(tone: AiTone) {
        tracker.track(
            AnalyticsEvent.PRODUCT_CREATION_AI_TONE_SELECTED,
            mapOf(
                AnalyticsTracker.KEY_TONE to tone.slug
            )
        )
        prefs.aiContentGenerationTone = tone
        _state.value = _state.value.copy(selectedTone = tone)
    }

    fun onMediaSelected(image: Image) {
        _state.value = _state.value.copy(isScanningImage = true)
        launch {
            textRecognitionEngine.processImage(image.uri)
                .onSuccess { keywords ->
                    tracker.track(
                        AnalyticsEvent.PRODUCT_CREATION_AI_TEXT_DETECTED,
                        mapOf(
                            "number_of_texts" to keywords.size
                        )
                    )
                    if (keywords.isNotEmpty()) {
                        onPromptUpdated(keywords.joinToString(separator = DEFAULT_PROMPT_DELIMITER))
                        _state.value = _state.value.copy(noTextDetectedMessage = false)
                    } else {
                        _state.value = _state.value.copy(noTextDetectedMessage = true)
                    }
                }
                .onFailure { error ->
                    tracker.track(
                        AnalyticsEvent.PRODUCT_CREATION_AI_TEXT_DETECTION_FAILED,
                        mapOf(
                            AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                            AnalyticsTracker.KEY_ERROR_DESC to error.message,
                        )
                    )
                    triggerEvent(ShowSnackbar(R.string.ai_product_creation_scanning_photo_error))
                }
            _state.value = _state.value.copy(
                selectedImage = image,
                isScanningImage = false,
            )
        }
    }

    fun onImageActionSelected(imageAction: ImageAction) {
        when (imageAction) {
            ImageAction.View -> _state.value = _state.value.copy(showImageFullScreen = true)
            ImageAction.Replace -> {
                tracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_STARTED_PACKAGE_PHOTO_SELECTION_FLOW)
                _state.value = _state.value.copy(isMediaPickerDialogVisible = true)
            }

            ImageAction.Remove -> {
                val previousState = _state.value
                _state.value = _state.value.copy(
                    selectedImage = null,
                    noTextDetectedMessage = false,
                )
                triggerEvent(
                    Event.ShowUndoSnackbar(
                        message = resourceProvider.getString(R.string.ai_product_creation_photo_removed),
                        undoAction = { _state.value = previousState }
                    )
                )
            }
        }
    }

    fun onImageFullScreenDismissed() {
        _state.value = _state.value.copy(showImageFullScreen = false)
    }

    @Parcelize
    data class AiProductPromptState(
        val productPrompt: String,
        val selectedTone: AiTone,
        val isMediaPickerDialogVisible: Boolean,
        val selectedImage: Image?,
        val isScanningImage: Boolean,
        val showImageFullScreen: Boolean,
        val noTextDetectedMessage: Boolean,
        val promptSuggestionBarState: PromptSuggestionBar
    ) : Parcelable

    @Parcelize
    data class PromptSuggestionBar(
        @ColorRes val progressBarColorRes: Int,
        @StringRes val messageRes: Int,
        val progress: Float,
    ) : Parcelable

    data class ShowMediaDialog(val source: DataSource) : Event()
    data class ShowProductPreviewScreen(
        val productFeatures: String,
        val image: Image?
    ) : Event()
}
