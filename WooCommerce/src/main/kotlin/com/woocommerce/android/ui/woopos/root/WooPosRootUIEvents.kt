package com.woocommerce.android.ui.woopos.root

sealed class WooPosRootUIEvents {
    data object ExitPOS : WooPosRootUIEvents()
    data object ConnectToAReader: WooPosRootUIEvents()
}
