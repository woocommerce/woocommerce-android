package com.woocommerce.android.ui.woopos.root

import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ConnectToAReaderClicked
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.ExitPOSClicked
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosRootViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade
) : ViewModel() {
    fun onUiEvent(event: WooPosRootUIEvent) {
        when (event) {
            ConnectToAReaderClicked -> cardReaderFacade.connectToReader()
            ExitPOSClicked -> TODO()
        }
    }
}
