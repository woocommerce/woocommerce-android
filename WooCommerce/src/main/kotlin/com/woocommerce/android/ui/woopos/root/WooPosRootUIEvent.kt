package com.woocommerce.android.ui.woopos.root

sealed class WooPosRootUIEvent {
    data object ExitPOSClicked : WooPosRootUIEvent()
    data object ConnectToAReaderClicked : WooPosRootUIEvent()
}
