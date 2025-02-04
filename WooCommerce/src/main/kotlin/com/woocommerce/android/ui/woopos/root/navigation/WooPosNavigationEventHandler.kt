package com.woocommerce.android.ui.woopos.root.navigation

import androidx.activity.ComponentActivity
import androidx.navigation.NavHostController
import com.woocommerce.android.ui.woopos.cashpayment.navigateToCashPaymentScreen
import com.woocommerce.android.ui.woopos.emailreceipt.navigateToEmailReceipt
import com.woocommerce.android.ui.woopos.home.navigateToHomeScreen
import com.woocommerce.android.ui.woopos.home.navigateToHomeScreenAfterSuccessfulCashPayment
import com.woocommerce.android.ui.woopos.home.navigateToHomeScreenIfHomeScreenNotOpen

fun NavHostController.handleNavigationEvent(
    event: WooPosNavigationEvent,
    activity: ComponentActivity,
) {
    when (event) {
        is WooPosNavigationEvent.ExitPosClicked,
        is WooPosNavigationEvent.BackFromSplashClicked -> activity.finish()

        is WooPosNavigationEvent.OpenHomeFromSplash -> navigateToHomeScreen()
        is WooPosNavigationEvent.OpenCashPayment -> navigateToCashPaymentScreen(event.orderId)
        is WooPosNavigationEvent.GoBack -> popBackStack()
        is WooPosNavigationEvent.OpenHomeFromCashPaymentAfterSuccessfulPayment ->
            navigateToHomeScreenAfterSuccessfulCashPayment()

        is WooPosNavigationEvent.OpenEmailReceipt -> navigateToEmailReceipt(event.orderId)
        WooPosNavigationEvent.ReturnHomeFromCashPayment -> navigateToHomeScreenIfHomeScreenNotOpen()
    }
}
