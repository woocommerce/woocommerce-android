package com.woocommerce.android.ui.blaze.creation.ad

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
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

    private val navArgs: BlazeCampaignCreationEditAdFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = MutableStateFlow(ViewState(navArgs.tagline, navArgs.description, navArgs.adImageUrl))
    val viewState = _viewState.asLiveData()

    data class ViewState(
        val tagLine: String,
        val description: String,
        val campaignImageUrl: String?,
        val isPreviousSuggestionButtonEnabled: Boolean = false,
        val isNextSuggestionButtonEnabled: Boolean = true,
    ) {
        val taglineCharactersRemaining: Int
            get() = TAGLINE_MAX_LENGTH - tagLine.length
        val descriptionCharactersRemaining: Int
            get() = DESCRIPTION_MAX_LENGTH - description.length
    }

    fun onNextSuggestionTapped() {

    }

    fun onPreviousSuggestionTapped() {

    }

    fun onChangeImageTapped() {

    }

    fun onBackButtonTapped() {

    }

    fun onTagLineChanged(tagLine: String) {
        _viewState.value = _viewState.value.copy(tagLine = tagLine.take(TAGLINE_MAX_LENGTH))
    }

    fun onDescriptionChanged(description: String) {
        _viewState.value = _viewState.value.copy(description = description.take(DESCRIPTION_MAX_LENGTH))
    }
}
