package com.woocommerce.android.ui.blaze.creation.ad

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_EDIT_AD_AI_SUGGESTION_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_EDIT_AD_SAVE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.AiSuggestionForAd
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationEditAdViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val blazeRepository: BlazeRepository
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val TAGLINE_MAX_LENGTH = 32
        private const val DESCRIPTION_MAX_LENGTH = 140
    }

    private val navArgs: BlazeCampaignCreationEditAdFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState(navArgs.adImage)
    )
    val viewState = _viewState.asLiveData()

    init {
        loadSuggestions()
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            val passedDetails = AiSuggestionForAd(navArgs.tagline, navArgs.description)
            val suggestions = navArgs.aiSuggestionsForAd.toList()
            _viewState.update {
                it.copy(
                    suggestions = listOf(passedDetails) + (suggestions - passedDetails),
                    suggestionIndex = 0
                )
            }
        }
    }

    fun onNextSuggestionTapped() {
        analyticsTrackerWrapper.track(stat = BLAZE_CREATION_EDIT_AD_AI_SUGGESTION_TAPPED)
        _viewState.update {
            val index = it.suggestionIndex
            it.copy(suggestionIndex = index + 1)
        }
    }

    fun onPreviousSuggestionTapped() {
        analyticsTrackerWrapper.track(stat = BLAZE_CREATION_EDIT_AD_AI_SUGGESTION_TAPPED)
        _viewState.update {
            val index = it.suggestionIndex
            it.copy(suggestionIndex = index - 1)
        }
    }

    fun onSaveTapped() {
        analyticsTrackerWrapper.track(stat = BLAZE_CREATION_EDIT_AD_SAVE_TAPPED)
        triggerEvent(
            ExitWithResult(
                EditAdResult(
                    tagline = _viewState.value.tagLine,
                    description = _viewState.value.description,
                    campaignImage = _viewState.value.adImage
                )
            )
        )
    }

    fun onChangeImageTapped() {
        setMediaPickerDialogVisibility(true)
    }

    fun onMediaPickerDialogDismissed() {
        setMediaPickerDialogVisibility(false)
    }

    fun onMediaLibraryRequested(source: MediaPickerSetup.DataSource) {
        triggerEvent(ShowMediaLibrary(source))
        setMediaPickerDialogVisibility(false)
    }

    fun onBackButtonTapped() {
        triggerEvent(Exit)
    }

    fun onTagLineChanged(tagLine: String) {
        updateSuggestion(AiSuggestionForAd(tagLine.take(TAGLINE_MAX_LENGTH), _viewState.value.description))
    }

    fun onDescriptionChanged(description: String) {
        updateSuggestion(AiSuggestionForAd(_viewState.value.tagLine, description.take(DESCRIPTION_MAX_LENGTH)))
    }

    fun onLocalImageSelected(uri: String) {
        launch {
            if (blazeRepository.isValidAdImage(uri)) {
                _viewState.update {
                    it.copy(adImage = BlazeRepository.BlazeCampaignImage.LocalImage(uri))
                }
            } else {
                showInvalidImageSizeDialog()
            }
        }
    }

    fun onWPMediaSelected(image: Product.Image) {
        launch {
            if (blazeRepository.isValidAdImage(image.source)) {
                _viewState.update {
                    it.copy(
                        adImage = BlazeRepository.BlazeCampaignImage.RemoteImage(
                            mediaId = image.id,
                            uri = image.source
                        )
                    )
                }
            } else {
                showInvalidImageSizeDialog()
            }
        }
    }

    private fun showInvalidImageSizeDialog() {
        triggerEvent(
            Event.ShowDialog(
                titleId = R.string.blaze_campaign_edit_ad_invalid_image_title,
                messageId = R.string.blaze_campaign_edit_ad_invalid_image_description,
                positiveButtonId = R.string.dialog_ok,
                positiveBtnAction = { dialog, _ ->
                    dialog.dismiss()
                },
            )
        )
    }

    private fun updateSuggestion(suggestion: AiSuggestionForAd) {
        _viewState.update {
            val suggestions = it.suggestions.toMutableList()
            suggestions[it.suggestionIndex] = suggestion
            it.copy(suggestions = suggestions)
        }
    }

    private fun setMediaPickerDialogVisibility(isVisible: Boolean) {
        _viewState.update {
            it.copy(isMediaPickerDialogVisible = isVisible)
        }
    }

    data class ShowMediaLibrary(val source: MediaPickerSetup.DataSource) : Event()

    @Parcelize
    data class ViewState(
        val adImage: BlazeRepository.BlazeCampaignImage,
        val suggestions: List<AiSuggestionForAd> = emptyList(),
        val suggestionIndex: Int = 0,
        val isMediaPickerDialogVisible: Boolean = false
    ) : Parcelable {
        val tagLine: String
            get() = suggestions.getOrNull(suggestionIndex)?.tagLine ?: ""
        val description: String
            get() = suggestions.getOrNull(suggestionIndex)?.description ?: ""
        val taglineCharactersRemaining: Int
            get() = TAGLINE_MAX_LENGTH - (suggestions.getOrNull(suggestionIndex)?.tagLine?.length ?: 0)
        val descriptionCharactersRemaining: Int
            get() = DESCRIPTION_MAX_LENGTH - (suggestions.getOrNull(suggestionIndex)?.description?.length ?: 0)
        val isPreviousSuggestionButtonEnabled: Boolean
            get() = suggestionIndex > 0
        val isNextSuggestionButtonEnabled: Boolean
            get() = suggestionIndex < suggestions.size - 1
    }

    @Parcelize
    data class EditAdResult(
        val tagline: String,
        val description: String,
        val campaignImage: BlazeRepository.BlazeCampaignImage
    ) : Parcelable
}
