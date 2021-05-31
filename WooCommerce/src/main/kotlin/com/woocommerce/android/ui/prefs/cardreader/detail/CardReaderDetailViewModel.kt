package com.woocommerce.android.ui.prefs.cardreader.detail

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderConnectScreen
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderDetailViewModel @Inject constructor(savedState: SavedStateHandle) :
    ScopedViewModel(savedState) {
    fun onConnectBtnClicked() {
        triggerEvent(CardReaderConnectScreen)
    }

    sealed class NavigationTarget : Event() {
        object CardReaderConnectScreen : NavigationTarget()
    }
}
