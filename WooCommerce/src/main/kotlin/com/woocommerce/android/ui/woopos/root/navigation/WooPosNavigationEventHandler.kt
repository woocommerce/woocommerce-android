package com.woocommerce.android.ui.woopos.root.navigation

import androidx.activity.ComponentActivity
import androidx.navigation.NavHostController
import com.woocommerce.android.ui.woopos.home.navigateToHomeScreen
import com.woocommerce.android.ui.woopos.payment.success.navigateToPaymentSuccessScreen
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent
import com.woocommerce.android.ui.woopos.root.WooPosRootUIEvent.OnBackFromHomeClicked

fun NavHostController.handleNavigationEvent(
    event: WooPosNavigationEvent,
    activity: ComponentActivity,
    onUIEvent: (WooPosRootUIEvent) -> Unit,
) {
    when (event) {
        is WooPosNavigationEvent.ExitPosClicked -> activity.finish()
        is WooPosNavigationEvent.BackFromHomeClicked -> onUIEvent(OnBackFromHomeClicked)
        is WooPosNavigationEvent.NavigateToPaymentSuccess -> navigateToPaymentSuccessScreen()
        WooPosNavigationEvent.NewTransactionClicked -> navigateToHomeScreen()
    }
}
