package com.woocommerce.android.ui.woopos.root.navigation

sealed class WooPosNavigationEvent {
    data object ExitPosClicked : WooPosNavigationEvent()
    data object BackFromSplashClicked : WooPosNavigationEvent()
    data object OpenHomeFromSplash : WooPosNavigationEvent()
    data class OpenCashPayment(val orderId: Long) : WooPosNavigationEvent()
    data class OpenEmailReceipt(val orderId: Long) : WooPosNavigationEvent()
    data object GoBack : WooPosNavigationEvent()
    data object OpenHomeFromCashPaymentAfterSuccessfulPayment : WooPosNavigationEvent()
}
