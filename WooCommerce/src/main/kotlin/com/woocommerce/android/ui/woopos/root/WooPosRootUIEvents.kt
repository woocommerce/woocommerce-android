package com.woocommerce.android.ui.woopos.root

sealed class WooPosRootUIEvents {
    data object ExitPOSClicked : WooPosRootUIEvents()
    data object ConnectToAReaderClicked : WooPosRootUIEvents()
}
