package com.woocommerce.android.ui.prefs.cardreader.detail

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.prefs.cardreader.detail.CardReaderDetailViewModel.NavigationTarget.CardReaderScanScreen
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    fun onInitiateScanBtnClicked() {
        triggerEvent(CardReaderScanScreen)
    }

    sealed class NavigationTarget : Event() {
        object CardReaderScanScreen : NavigationTarget()
    }
}
