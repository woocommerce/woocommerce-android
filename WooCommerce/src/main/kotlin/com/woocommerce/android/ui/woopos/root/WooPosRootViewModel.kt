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
import com.woocommerce.android.ui.woopos.util.toStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class WooPosRootViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
) : ViewModel() {
    val bottomToolbarState: StateFlow<BottomToolbarState> = cardReaderFacade.readerStatus.map {
        BottomToolbarState(cardReaderStatus = mapCardReaderStatusToUiState(it))
    }.toStateFlow(viewModelScope, BottomToolbarState(BottomToolbarState.CardReaderStatus.Unknown))

    fun onUiEvent(event: WooPosRootUIEvent) {
        when (event) {
            ConnectToAReaderClicked -> cardReaderFacade.connectToReader()
            ExitPOSClicked -> TODO()
        }
    }

    private fun mapCardReaderStatusToUiState(status: CardReaderStatus): BottomToolbarState.CardReaderStatus {
        return when (status) {
            is Connected -> BottomToolbarState.CardReaderStatus.Connected
            is Connecting -> BottomToolbarState.CardReaderStatus.Connecting
            is NotConnected -> BottomToolbarState.CardReaderStatus.NotConnected
        }
    }
}
