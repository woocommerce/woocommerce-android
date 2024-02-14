package com.woocommerce.android.ui.blaze.creation.ad

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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
    private val blazeRepository: BlazeRepository,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val TAGLINE_MAX_LENGTH = 32
        private const val DESCRIPTION_MAX_LENGTH = 140
    }

    private val navArgs: BlazeCampaignCreationEditAdFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState(navArgs.adImageUrl)
    )
    val viewState = _viewState.asLiveData()

    init {
        loadSuggestions()
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            blazeRepository.fetchAdSuggestions(navArgs.productId).getOrNull()?.let { list ->
                val index = list.indexOfFirst { it.tagLine == navArgs.tagline && it.description == navArgs.description }
                val suggestions = list.map { AiSuggestionForAd(it.tagLine, it.description) }
                if (index != -1) {
                    _viewState.update {
                        _viewState.value.copy(
                            suggestions = suggestions,
                            suggestionIndex = index
                        )
                    }
                } else {
                    _viewState.update {
                        _viewState.value.copy(
                            suggestions = listOf(AiSuggestionForAd(navArgs.tagline, navArgs.description)) + suggestions,
                            suggestionIndex = 0
                        )
                    }
                }
            }
        }
    }

    fun onNextSuggestionTapped() {
        _viewState.update {
            val index = _viewState.value.suggestionIndex
            _viewState.value.copy(suggestionIndex = index + 1)
        }
    }

    fun onPreviousSuggestionTapped() {
        _viewState.update {
            val index = _viewState.value.suggestionIndex
            _viewState.value.copy(suggestionIndex = index - 1)
        }
    }

    fun onSaveTapped() {
        triggerEvent(
            ExitWithResult(
                EditAdResult(
                    tagline = _viewState.value.tagLine,
                    description = _viewState.value.description,
                    campaignImageUrl = _viewState.value.adImageUrl
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
        updateSuggestion(AiSuggestionForAd(_viewState.value.tagLine, description.take(TAGLINE_MAX_LENGTH)))
    }

    fun onImageChanged(url: String) {
        _viewState.update {
            _viewState.value.copy(adImageUrl = url)
        }
    }

    private fun updateSuggestion(suggestion: AiSuggestionForAd) {
        _viewState.update {
            val suggestions = _viewState.value.suggestions.toMutableList()
            suggestions[_viewState.value.suggestionIndex] = suggestion
            _viewState.value.copy(suggestions = suggestions)
        }
    }

    private fun setMediaPickerDialogVisibility(isVisible: Boolean) {
        _viewState.update {
            _viewState.value.copy(isMediaPickerDialogVisible = isVisible)
        }
    }

    data class ShowMediaLibrary(val source: MediaPickerSetup.DataSource) : Event()

    @Parcelize
    data class ViewState(
        val adImageUrl: String?,
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
        val campaignImageUrl: String?
    ) : Parcelable
}
