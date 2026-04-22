package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderModeViewModel @Inject constructor(
    bridge: CardReaderModeStateBridge,
) : ViewModel() {
    val stateOverride: LiveData<ViewState?> = bridge.stateOverride
    val events: LiveData<MultiLiveEvent.Event> = bridge.events
}
