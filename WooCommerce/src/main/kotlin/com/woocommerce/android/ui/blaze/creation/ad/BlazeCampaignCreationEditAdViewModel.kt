package com.woocommerce.android.ui.blaze.creation.ad

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationEditAdViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val aiRepository: AIRepository
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val TAGLINE_MAX_LENGTH = 50
        private const val DESCRIPTION_MAX_LENGTH = 140
    }

    private val _viewState = MutableStateFlow(ViewState("", "", null))
    val viewState = _viewState.asLiveData()

    data class ViewState(
        val tagLine: String,
        val description: String,
        val campaignImageUrl: String?,
    ) {
        val taglineCharactersRemaining: Int
            get() = TAGLINE_MAX_LENGTH - tagLine.length
        val descriptionCharactersRemaining: Int
            get() = DESCRIPTION_MAX_LENGTH - description.length
    }

    fun onNextAISuggestionTapped() {

    }

    fun onPreviousAISuggestionTapped() {

    }

    fun onTagLineChanged(tagLine: String) {
        _viewState.value = _viewState.value.copy(tagLine = tagLine.substring(0, TAGLINE_MAX_LENGTH))
    }

    fun onDescriptionChanged(description: String) {
        _viewState.value = _viewState.value.copy(description = description.substring(0, DESCRIPTION_MAX_LENGTH))
    }
}
