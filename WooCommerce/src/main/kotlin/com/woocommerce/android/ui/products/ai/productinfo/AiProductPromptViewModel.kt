package com.woocommerce.android.ui.products.ai.productinfo

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.AiTone
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
            selectedAiTone = AiTone.Casual
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

    fun onToneSelected(aiTone: AiTone) {
        _state.value = _state.value.copy(selectedAiTone = aiTone)
    }


    @Parcelize
    data class AiProductPromptState(
        val productPrompt: String,
        val selectedAiTone: AiTone
    ) : Parcelable
}
