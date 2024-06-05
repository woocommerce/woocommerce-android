package com.woocommerce.android.ui.woopos.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ConnectToAReaderClicked
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ExitPOSClicked
import com.woocommerce.android.ui.woopos.util.toStateFlow
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class WooPosRootViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {
    val bottomToolbarState: StateFlow<BottomToolbarState> = cardReaderFacade.readerStatus.map {
        BottomToolbarState(cardReaderStatus = getCardReaderStatusText(status = it))
    }.toStateFlow(viewModelScope, BottomToolbarState(BottomToolbarState.CardReaderStatus("")))

    private fun getCardReaderStatusText(status: CardReaderStatus) = when (status) {
        is NotConnected -> BottomToolbarState.CardReaderStatus(
            title = resourceProvider.getString(R.string.woopos_reader_disconnected)
        )
        is Connecting -> BottomToolbarState.CardReaderStatus(
            title = resourceProvider.getString(R.string.woopos_reader_connecting)
        )
        is Connected -> BottomToolbarState.CardReaderStatus(
            title = resourceProvider.getString(R.string.woopos_reader_connected)
        )
    }

    fun onUiEvent(event: WooPosRootUIEvent) {
        when (event) {
            ConnectToAReaderClicked -> cardReaderFacade.connectToReader()
            ExitPOSClicked -> TODO()
        }
    }
}
