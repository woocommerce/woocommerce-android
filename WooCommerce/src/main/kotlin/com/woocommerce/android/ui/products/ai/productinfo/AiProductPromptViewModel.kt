package com.woocommerce.android.ui.products.ai.productinfo

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.ai.TextRecognitionEngine
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.ImageAction.Remove
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.ImageAction.Replace
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.ImageAction.View
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import javax.inject.Inject

@HiltViewModel
class AiProductPromptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tracker: AnalyticsTrackerWrapper,
    private val textRecognitionEngine: TextRecognitionEngine,
) : ScopedViewModel(savedState = savedStateHandle) {
    private val _state = savedStateHandle.getStateFlow(
        viewModelScope,
        AiProductPromptState(
            productPrompt = "",
            selectedTone = Tone.Casual,
            isMediaPickerDialogVisible = false,
            mediaUri = null,
            isScanningImage = false,
            showImageFullScreen = false,
            noTextDetectedMessage = false
        )
    )

    val state = _state.asLiveData()

    fun onBackButtonClick() {
        triggerEvent(Exit)
    }

    fun onPromptUpdated(prompt: String) {
        _state.value = _state.value.copy(productPrompt = prompt)
    }

    fun onAddImageForScanning() {
        tracker.track(
            AnalyticsEvent.PRODUCT_NAME_AI_PACKAGE_IMAGE_BUTTON_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_PRODUCT_CREATION_AI
            )
        )
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
        // TODO()
    }

    fun onToneSelected(tone: Tone) {
        _state.value = _state.value.copy(selectedTone = tone)
    }

    fun onMediaSelected(mediaUri: String) {
        _state.value = _state.value.copy(isScanningImage = true)
        launch {
            textRecognitionEngine.processImage(mediaUri)
                .onSuccess { keywords ->
                    tracker.track(
                        AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_SCAN_COMPLETED,
                        mapOf(
                            AnalyticsTracker.KEY_SCANNED_TEXT_COUNT to keywords.size
                        )
                    )
                    if (keywords.isNotEmpty()) {
                        _state.value = _state.value.copy(
                            productPrompt = keywords.joinToString(separator = " "),
                            noTextDetectedMessage = false
                        )
                    } else {
                        _state.value = _state.value.copy(noTextDetectedMessage = true)
                    }
                }
                .onFailure { error ->
                    tracker.track(
                        AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_SCAN_FAILED,
                        mapOf(
                            AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                            AnalyticsTracker.KEY_ERROR_DESC to error.message,
                        )
                    )
                    triggerEvent(ShowSnackbar(R.string.ai_product_creation_scanning_photo_error))
                }
            _state.value = _state.value.copy(
                mediaUri = mediaUri,
                isScanningImage = false,
            )
        }
    }

    fun onImageActionSelected(imageAction: ImageAction) {
        when (imageAction) {
            View -> _state.value = _state.value.copy(showImageFullScreen = true)
            Replace -> _state.value = _state.value.copy(isMediaPickerDialogVisible = true)
            Remove -> _state.value = _state.value.copy(mediaUri = null)
        }
    }

    fun onImageFullScreenDismissed() {
        _state.value = _state.value.copy(showImageFullScreen = false)
    }

    @Parcelize
    data class AiProductPromptState(
        val productPrompt: String,
        val selectedTone: Tone,
        val isMediaPickerDialogVisible: Boolean,
        val mediaUri: String?,
        val isScanningImage: Boolean,
        val showImageFullScreen: Boolean,
        val noTextDetectedMessage: Boolean,
    ) : Parcelable

    enum class Tone(@StringRes val displayName: Int, val slug: String) {
        Casual(R.string.product_creation_ai_tone_casual, "Casual"),
        Formal(R.string.product_creation_ai_tone_formal, "Formal"),
        Flowery(R.string.product_creation_ai_tone_flowery, "Flowery"),
        Convincing(R.string.product_creation_ai_tone_convincing, "Convincing");

        companion object {
            fun fromString(source: String): Tone =
                Tone.values().firstOrNull { it.slug == source } ?: Casual
        }
    }

    enum class ImageAction(@StringRes val displayName: Int) {
        View(R.string.ai_product_creation_view_image),
        Replace(R.string.ai_product_creation_replace_image),
        Remove(R.string.ai_product_creation_remove_image)
    }

    data class ShowMediaDialog(val source: DataSource) : Event()
    data class ViewImage(val mediaUri: String) : Event()
}
