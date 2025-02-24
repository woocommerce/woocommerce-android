package com.woocommerce.android.ui.woopos.root.navigation

import androidx.activity.ComponentActivity
import androidx.navigation.NavHostController
import com.woocommerce.android.ui.woopos.cashpayment.CASH_ROUTE
import com.woocommerce.android.ui.woopos.cashpayment.navigateToCashPaymentScreen
import com.woocommerce.android.ui.woopos.emailreceipt.navigateToEmailReceipt
import com.woocommerce.android.ui.woopos.home.navigateToHomeScreen
import com.woocommerce.android.ui.woopos.home.navigateToHomeScreenAfterSuccessfulCashPayment
import com.woocommerce.android.ui.woopos.home.navigateToHomeScreenIfHomeScreenNotOpen
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BackToCheckoutFromCash
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun NavHostController.handleNavigationEvent(
    event: WooPosNavigationEvent,
    activity: ComponentActivity,
    tracker: WooPosAnalyticsTracker,
) {
    when (event) {
        is WooPosNavigationEvent.ExitPosClicked,
        is WooPosNavigationEvent.BackFromSplashClicked -> activity.finish()

        is WooPosNavigationEvent.OpenHomeFromSplash -> navigateToHomeScreen()
        is WooPosNavigationEvent.OpenCashPayment -> navigateToCashPaymentScreen(event.orderId)
        is WooPosNavigationEvent.GoBack -> {
            if (currentDestination?.route == CASH_ROUTE) {
                CoroutineScope(Dispatchers.Main).launch {
                    tracker.track(BackToCheckoutFromCash)
                }
            }
            popBackStack()
        }
        is WooPosNavigationEvent.OpenHomeFromCashPaymentAfterSuccessfulPayment ->
            navigateToHomeScreenAfterSuccessfulCashPayment()

        is WooPosNavigationEvent.OpenEmailReceipt -> navigateToEmailReceipt(event.orderId)
        WooPosNavigationEvent.ReturnHomeFromCashPayment -> navigateToHomeScreenIfHomeScreenNotOpen()
    }
}
