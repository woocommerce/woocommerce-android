package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayError
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocationPermissionDenied
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocationPermissionExplainer
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
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
    private val cardReaderManager: CardReaderManager,
    private val developerOptionsRepository: DeveloperOptionsRepository,
    private val selectedSite: SelectedSite,
) : ViewModel() {

    private val _viewState = MutableStateFlow<ViewState?>(null)
    val viewState: StateFlow<ViewState?> = _viewState.asStateFlow()

    private val _events = Channel<CardReaderModeEvent>(capacity = Channel.BUFFERED)
    val events: Flow<CardReaderModeEvent> = _events.receiveAsFlow()

    private var sessionStarted = false

    fun onLocationPermissionMissing() {
        if (sessionStarted) return
        _viewState.value = RemoteTapToPayLocationPermissionExplainer(
            onPrimaryActionClicked = { _events.trySend(CardReaderModeEvent.RequestLocationPermission) },
        )
    }

    fun onLocationPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        when {
            granted -> startSessionIfNeeded()
            !shouldShowRationale -> _viewState.value = RemoteTapToPayLocationPermissionDenied(
                onPrimaryActionClicked = { _events.trySend(CardReaderModeEvent.OpenAppSettings) },
            )
            else -> onLocationPermissionMissing()
        }
    }

    private fun startSessionIfNeeded() {
        if (sessionStarted) return
        val siteId = selectedSite.getOrNull()?.remoteId()?.value ?: return
        sessionStarted = true

        if (!cardReaderManager.initialized) {
            cardReaderManager.initialize(
                updateFrequency = developerOptionsRepository.getUpdateSimulatedReaderOption(),
                useInterac = developerOptionsRepository.isInteracPaymentEnabled(),
                isDebug = BuildConfig.DEBUG,
            )
        }
        viewModelScope.launch {
            session.state.collect { sessionState ->
                _viewState.value = mapToViewState(sessionState)
            }
        }
        session.start(
            parentScope = viewModelScope,
            siteId = siteId,
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
        is CardReaderRemoteSessionState.Error -> RemoteTapToPayError(
            message = state.message,
            onPrimaryActionClicked = ::exit,
        )
    }

    private fun exit() {
        _events.trySend(CardReaderModeEvent.Exit)
    }
}
