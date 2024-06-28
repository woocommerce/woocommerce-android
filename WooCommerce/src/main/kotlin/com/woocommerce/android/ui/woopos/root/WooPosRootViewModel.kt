package com.woocommerce.android.ui.woopos.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ConnectToAReaderClicked
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ExitConfirmationDialogDismissed
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ExitPOSClicked
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.OnBackFromHomeClicked
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosRootViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
) : ViewModel() {
    private val _rootScreenState = MutableStateFlow(
        WooPosRootScreenState(
            cardReaderStatus = WooPosRootScreenState.WooPosCardReaderStatus.Unknown,
            exitConfirmationDialog = null,
        )
    )
    val rootScreenState: StateFlow<WooPosRootScreenState> = _rootScreenState

    init {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.collect {
                _rootScreenState.value = _rootScreenState.value.copy(
                    cardReaderStatus = mapCardReaderStatusToUiState(it)
                )
            }
        }
    }

    fun onUiEvent(event: WooPosRootUIEvent) {
        when (event) {
            ConnectToAReaderClicked -> handleConnectToReaderButtonClicked()
            ExitConfirmationDialogDismissed -> {
                _rootScreenState.value = _rootScreenState.value.copy(exitConfirmationDialog = null)
            }

            ExitPOSClicked, OnBackFromHomeClicked -> {
                _rootScreenState.value = _rootScreenState.value.copy(
                    exitConfirmationDialog = WooPosRootScreenState.WooPosExitConfirmationDialog
                )
            }

            is WooPosRootUIEvent.OnSuccessfulPayment -> TODO()
        }
    }

    private fun handleConnectToReaderButtonClicked() {
        if (_rootScreenState.value.cardReaderStatus != WooPosRootScreenState.WooPosCardReaderStatus.Connected) {
            cardReaderFacade.connectToReader()
        }
    }

    private fun mapCardReaderStatusToUiState(status: CardReaderStatus): WooPosRootScreenState.WooPosCardReaderStatus {
        return when (status) {
            is Connected -> WooPosRootScreenState.WooPosCardReaderStatus.Connected
            is Connecting -> WooPosRootScreenState.WooPosCardReaderStatus.Connecting
            is NotConnected -> WooPosRootScreenState.WooPosCardReaderStatus.NotConnected
        }
    }
}
