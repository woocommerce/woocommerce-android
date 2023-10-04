package com.woocommerce.android.ui.payments.taptopay.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TapToPayAboutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableLiveData<UiState>()
    val viewState: LiveData<UiState> = _viewState

    init {
        _viewState.value = UiState(
            importantInfo = UiState.ImportantInfo(
                pinDescription = "Your PIN is used to verify your identity when you make a purchase. You can change your PIN in the settings.",
                onLearnMoreAboutCardReaders = { },
            )
        )
    }

    fun onBackClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    data class UiState(val importantInfo: ImportantInfo?) {
        data class ImportantInfo(
            val pinDescription: String,
            val onLearnMoreAboutCardReaders: () -> Unit,
        )
    }
}
