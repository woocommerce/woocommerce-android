package com.woocommerce.android.ui.woopos.root.navigation

sealed class WooPosNavigationEvent {
    data object ExitPosClicked : WooPosNavigationEvent()
    data object BackFromSplashClicked : WooPosNavigationEvent()
    data object OpenHomeFromSplash : WooPosNavigationEvent()
}
