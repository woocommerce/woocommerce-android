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
    private val productFeatures = savedStateHandle.getStateFlow(
        viewModelScope,
        AiProductPromptState(
            productPrompt = "",
            selectedAiTone = AiTone.Casual
        )
    )

    fun onBackButtonClick() {
        triggerEvent(Exit)
    }

    val state = productFeatures.asLiveData()

    @Parcelize
    data class AiProductPromptState(
        val productPrompt: String,
        val selectedAiTone: AiTone
    ) : Parcelable
}
