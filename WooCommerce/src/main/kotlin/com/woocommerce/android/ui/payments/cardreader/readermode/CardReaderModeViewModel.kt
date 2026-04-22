package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardReaderModeViewModel @Inject constructor(
    private val session: CardReaderRemoteSession,
    private val bridge: CardReaderModeStateBridge,
) : ViewModel() {

    val stateOverride: LiveData<ViewState?> = bridge.stateOverride
    val events: LiveData<MultiLiveEvent.Event> = bridge.events

    init {
        viewModelScope.launch {
            session.state.collect { sessionState ->
                when (sessionState) {
                    CardReaderRemoteSessionState.Idle -> bridge.clear()
                    CardReaderRemoteSessionState.Starting -> bridge.push(
                        RemoteTapToPayStarting(onPrimaryActionClicked = ::exit)
                    )
                    is CardReaderRemoteSessionState.ReadyToPair -> bridge.push(
                        RemoteTapToPayReadyToPair(
                            deviceName = sessionState.deviceName,
                            fingerprintSuffix = sessionState.fingerprintSuffix,
                            onPrimaryActionClicked = ::exit,
                        )
                    )
                    is CardReaderRemoteSessionState.WaitingForPayment -> bridge.push(
                        RemoteTapToPayWaitingForPayment(
                            tabletName = sessionState.tabletName,
                            onPrimaryActionClicked = ::exit,
                        )
                    )
                }
            }
        }
        session.start(viewModelScope)
    }

    override fun onCleared() {
        session.stop()
        super.onCleared()
    }

    private fun exit() = bridge.emitEvent(CardReaderModeExit)
}
