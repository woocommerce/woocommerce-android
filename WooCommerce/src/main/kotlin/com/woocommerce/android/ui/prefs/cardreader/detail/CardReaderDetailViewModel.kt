package com.woocommerce.android.ui.prefs.cardreader.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.ViewState.NotConnectedState
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderDetailViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState = MutableLiveData<ViewState>(
        NotConnectedState(onPrimaryActionClicked = ::onConnectBtnClicked)
    )
    val viewStateData: LiveData<ViewState> = viewState

    fun onConnectBtnClicked() {
        triggerEvent(CardReaderConnectScreen)
    }

    sealed class NavigationTarget : Event() {
        object CardReaderConnectScreen : NavigationTarget()
    }

    sealed class ViewState() {
        data class NotConnectedState(val onPrimaryActionClicked: (() -> Unit)) : ViewState(
            headerLabel = R.string.card_reader_detail_not_connected_header,
            illustration = R.drawable.img_card_reader_not_connected,
            firstHintLabel = R.string.card_reader_detail_not_connected_first_hint_label,
            secondHintLabel = R.string.card_reader_detail_not_connected_second_hint_label,
            connectBtnLabel = R.string.card_reader_details_not_connected_connect_button_label
        )

        data class ConnectedState(): ViewState(

        )
    }
}
