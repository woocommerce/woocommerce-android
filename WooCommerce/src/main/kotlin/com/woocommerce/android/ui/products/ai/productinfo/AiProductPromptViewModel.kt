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
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
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
    private val tracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState = savedStateHandle) {
    private val _state = savedStateHandle.getStateFlow(
        viewModelScope,
        AiProductPromptState(
            productPrompt = "",
            selectedTone = Tone.Casual,
            isMediaPickerDialogVisible = false,
            mediaUri = null,
            isScanningImage = false
        )
    )

    val state = _state.asLiveData()

    fun onBackButtonClick() {
        triggerEvent(Exit)
    }

    fun onPromptUpdated(prompt: String) {
        _state.value = _state.value.copy(productPrompt = prompt)
    }

    fun onReadTextFromProductPhoto() {
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
        TODO("Not yet implemented")
    }

    fun onToneSelected(tone: Tone) {
        _state.value = _state.value.copy(selectedTone = tone)
    }

    fun onMediaSelected(mediaUri: String) {
        _state.value = _state.value.copy(mediaUri = mediaUri)
    }

    @Parcelize
    data class AiProductPromptState(
        val productPrompt: String,
        val selectedTone: Tone,
        val isMediaPickerDialogVisible: Boolean,
        val mediaUri: String?,
        val isScanningImage: Boolean
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

    data class ShowMediaDialog(val source: DataSource) : Event()
}
