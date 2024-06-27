package com.woocommerce.android.ui.woopos.root.navigation

sealed class WooPosNavigationEvent {
    data object ExitPosClicked : WooPosNavigationEvent()
    data object BackFromHomeClicked : WooPosNavigationEvent()
}
