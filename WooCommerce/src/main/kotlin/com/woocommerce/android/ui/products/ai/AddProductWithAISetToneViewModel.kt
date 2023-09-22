package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class AddProductWithAISetToneViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    val navArgs = AddProductWithAISetToneBottomSheetArgs.fromSavedStateHandle(savedStateHandle)

    private val _viewState = MutableStateFlow(
        ViewState(selectedAiTone = navArgs.aiTone)
    )
    val viewState = _viewState.asLiveData()

    fun onToneSelected(tone: AiTone) {
        _viewState.value = _viewState.value.copy(selectedAiTone = tone)
        triggerEvent(ExitWithResult(tone))
    }

    data class ViewState(val selectedAiTone: AiTone)

    enum class AiTone { Casual, Formal, Flowery, Convincing }
}
