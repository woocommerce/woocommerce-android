package com.woocommerce.android.ui.prefs.cardreader.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderWelcomeViewModel @Inject constructor(
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val _viewState = MutableLiveData(ViewState(::onButtonClick))
    val viewState: LiveData<ViewState> = _viewState

    private fun onButtonClick() {
        triggerEvent(CardReaderWelcomeDialogEvent.NavigateToOnboardingFlow)
    }

    sealed class CardReaderWelcomeDialogEvent : Event() {
        object NavigateToOnboardingFlow : Event()
    }

    data class ViewState(val buttonAction: () -> Unit) {
        val header = UiStringRes(R.string.card_reader_welcome_dialog_header)
        val img: Int = R.drawable.img_woman_payment_card
        val text = UiStringRes(R.string.card_reader_welcome_dialog_text)
        val buttonLabel = UiStringRes(R.string.continue_button)
    }
}
