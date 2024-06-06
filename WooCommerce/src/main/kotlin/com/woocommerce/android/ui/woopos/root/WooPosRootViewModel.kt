package com.woocommerce.android.ui.woopos.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ConnectToAReaderClicked
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ExitPOSClicked
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WooPosRootViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
) : ViewModel() {
    val bottomToolbarState: StateFlow<WooPosBottomToolbarState> = cardReaderFacade.readerStatus.map {
        WooPosBottomToolbarState(cardReaderStatus = mapCardReaderStatusToUiState(it))
    }.stateIn(viewModelScope, WhileSubscribed(), WooPosBottomToolbarState(WooPosBottomToolbarState.CardReaderStatus.Unknown))

    fun onUiEvent(event: WooPosRootUIEvent) {
        when (event) {
            ConnectToAReaderClicked -> handleConnectToReaderButtonClicked()
            ExitPOSClicked -> TODO()
        }
    }

    private fun handleConnectToReaderButtonClicked() {
        if (bottomToolbarState.value.cardReaderStatus != WooPosBottomToolbarState.CardReaderStatus.Connected) {
            cardReaderFacade.connectToReader()
        }
    }

    private fun mapCardReaderStatusToUiState(status: CardReaderStatus): WooPosBottomToolbarState.CardReaderStatus {
        return when (status) {
            is Connected -> WooPosBottomToolbarState.CardReaderStatus.Connected
            is Connecting -> WooPosBottomToolbarState.CardReaderStatus.Connecting
            is NotConnected -> WooPosBottomToolbarState.CardReaderStatus.NotConnected
        }
    }
}
