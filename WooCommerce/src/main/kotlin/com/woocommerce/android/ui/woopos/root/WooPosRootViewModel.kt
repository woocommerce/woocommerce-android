package com.woocommerce.android.ui.woopos.root

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosRootViewModel @Inject constructor() : ViewModel() {
    fun onUiEvent(event: WooPosRootUIEvent) {
        when (event) {
            WooPosRootUIEvent.ConnectToAReaderClicked -> TODO()
            WooPosRootUIEvent.ExitPOSClicked -> TODO()
        }
    }
}
