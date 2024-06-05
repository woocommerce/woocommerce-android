package com.woocommerce.android.ui.woopos.root

import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosRootViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade
) : ViewModel() {
    fun onUiEvent(event: WooPosRootUIEvents) {
        when (event) {
            WooPosRootUIEvents.ConnectToAReaderClicked -> cardReaderFacade.connectToReader()
            WooPosRootUIEvents.ExitPOSClicked -> TODO()
        }
    }
}
