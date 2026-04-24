package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayError
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardReaderModeViewModel @Inject constructor(
    private val session: CardReaderRemoteSession,
) : ViewModel() {

    private val _viewState = MutableStateFlow<ViewState?>(null)
    val viewState: StateFlow<ViewState?> = _viewState.asStateFlow()

    private val _events = Channel<CardReaderModeExit>(capacity = Channel.BUFFERED)
    val events: Flow<CardReaderModeExit> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            session.state.collect { sessionState ->
                _viewState.value = mapToViewState(sessionState)
            }
        }
        session.start(viewModelScope)
    }

    override fun onCleared() {
        session.stop()
        super.onCleared()
    }

    private fun mapToViewState(state: CardReaderRemoteSessionState): ViewState? = when (state) {
        CardReaderRemoteSessionState.Idle -> null
        CardReaderRemoteSessionState.Starting -> RemoteTapToPayStarting(onPrimaryActionClicked = ::exit)
        is CardReaderRemoteSessionState.ReadyToPair -> RemoteTapToPayReadyToPair(
            deviceName = state.deviceName,
            fingerprintSuffix = state.fingerprintSuffix,
            onPrimaryActionClicked = ::exit,
        )
        is CardReaderRemoteSessionState.WaitingForPayment -> RemoteTapToPayWaitingForPayment(
            tabletName = state.tabletName,
            onPrimaryActionClicked = ::exit,
        )
        is CardReaderRemoteSessionState.Error -> RemoteTapToPayError(
            message = state.message,
            onPrimaryActionClicked = ::exit,
        )
    }

    private fun exit() {
        _events.trySend(CardReaderModeExit)
    }
}
