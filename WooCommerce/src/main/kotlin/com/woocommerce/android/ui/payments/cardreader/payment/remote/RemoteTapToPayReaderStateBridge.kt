package com.woocommerce.android.ui.payments.cardreader.payment.remote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent

class RemoteTapToPayReaderStateBridge {

    private val _stateOverride = MutableLiveData<ViewState?>(null)
    val stateOverride: LiveData<ViewState?> = _stateOverride

    private val _events = MutableLiveData<MultiLiveEvent.Event>()
    val events: LiveData<MultiLiveEvent.Event> = _events

    fun push(state: ViewState) {
        require(state is RemoteTapToPayReadyToPair || state is RemoteTapToPayWaitingForPayment) {
            "push only accepts RemoteTapToPayReadyToPair or RemoteTapToPayWaitingForPayment"
        }
        _stateOverride.postValue(state)
    }

    fun clear() {
        _stateOverride.postValue(null)
    }

    fun emitEvent(event: MultiLiveEvent.Event) {
        event.isHandled = false
        _events.postValue(event)
    }
}
