package com.woocommerce.android.ui.woopos.root.navigation

import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability

sealed class WooPosNavigationEvent {
    data object ExitPosClicked : WooPosNavigationEvent()
    data object BackFromSplashClicked : WooPosNavigationEvent()
    data object OpenHomeFromSplash : WooPosNavigationEvent()
    data class OpenCashPayment(val orderId: Long) : WooPosNavigationEvent()
    data class OpenEmailReceipt(val orderId: Long) : WooPosNavigationEvent()
    data object GoBack : WooPosNavigationEvent()
    data object OpenHomeFromCashPaymentAfterSuccessfulPayment : WooPosNavigationEvent()
    data object ReturnHomeFromCashPayment : WooPosNavigationEvent()
    data class OpenEligibilityScreenFromSplash(
        val reason: WooPosLaunchability.NonLaunchabilityReason
    ) : WooPosNavigationEvent()
    data object OpenSettings : WooPosNavigationEvent()
}
