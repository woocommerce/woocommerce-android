package com.woocommerce.android.ui.products.ai.productinfo

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class AiProductPromptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedState = savedStateHandle) {
    private val _state = savedStateHandle.getStateFlow(
        viewModelScope,
        AiProductPromptState(
            productPrompt = "",
            selectedTone = Tone.Casual
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
        TODO("Not yet implemented")
    }

    fun onGenerateProductClicked() {
        TODO("Not yet implemented")
    }

    fun onToneSelected(tone: Tone) {
        _state.value = _state.value.copy(selectedTone = tone)
    }


    @Parcelize
    data class AiProductPromptState(
        val productPrompt: String,
        val selectedTone: Tone
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
}
