package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardReaderModeViewModel @Inject constructor(
    private val session: CardReaderRemoteSession,
    private val cardReaderManager: CardReaderManager,
    private val developerOptionsRepository: DeveloperOptionsRepository,
) : ViewModel() {

    private val _stateOverride = MutableLiveData<ViewState?>(null)
    val stateOverride: LiveData<ViewState?> = _stateOverride

    private val _events = MutableLiveData<MultiLiveEvent.Event>()
    val events: LiveData<MultiLiveEvent.Event> = _events

    init {
        if (!cardReaderManager.initialized) {
            cardReaderManager.initialize(
                updateFrequency = developerOptionsRepository.getUpdateSimulatedReaderOption(),
                useInterac = developerOptionsRepository.isInteracPaymentEnabled(),
                isDebug = BuildConfig.DEBUG,
            )
        }
        viewModelScope.launch {
            session.state.collect { sessionState ->
                _stateOverride.postValue(mapToViewState(sessionState))
            }
        }
        session.start(
            parentScope = viewModelScope,
            isSimulated = developerOptionsRepository.isSimulatedCardReaderEnabled(),
        )
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
    }

    private fun exit() {
        CardReaderModeExit.isHandled = false
        _events.postValue(CardReaderModeExit)
    }
}
