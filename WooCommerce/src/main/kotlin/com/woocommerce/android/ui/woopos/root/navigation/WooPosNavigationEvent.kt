package com.woocommerce.android.ui.woopos.root.navigation

sealed class WooPosNavigationEvent {
    data object ExitPosClicked : WooPosNavigationEvent()
    data object BackFromHomeClicked : WooPosNavigationEvent()
    data class NavigateToPaymentSuccess(val orderId: Long) : WooPosNavigationEvent()
    data object NewTransactionClicked : WooPosNavigationEvent()
}
