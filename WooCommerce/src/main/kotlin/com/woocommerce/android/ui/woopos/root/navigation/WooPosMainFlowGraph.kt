package com.woocommerce.android.ui.woopos.root.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.woocommerce.android.ui.woopos.cardpayment.cardPaymentScreen
import com.woocommerce.android.ui.woopos.cashpayment.cashPaymentScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.authenticatedwebview.webViewScreen
import com.woocommerce.android.ui.woopos.emailreceipt.emailReceiptScreen
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.home.eligibilityScreen
import com.woocommerce.android.ui.woopos.home.homeScreen
import com.woocommerce.android.ui.woopos.markorderascomplete.markOrderAsCompleteScreen
import com.woocommerce.android.ui.woopos.orders.details.refund.refundReasonScreen
import com.woocommerce.android.ui.woopos.orders.ordersScreen
import com.woocommerce.android.ui.woopos.paymentsuccess.paymentSuccessScreen
import com.woocommerce.android.ui.woopos.scantopay.scanToPayScreen
import com.woocommerce.android.ui.woopos.settings.settingsScreen
import com.woocommerce.android.ui.woopos.splash.SPLASH_ROUTE
import com.woocommerce.android.ui.woopos.splash.splashScreen

const val MAIN_GRAPH_ROUTE = "main-graph"

fun NavGraphBuilder.mainGraph(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    homeViewModel: WooPosHomeViewModel,
) {
    navigation(
        startDestination = SPLASH_ROUTE,
        route = MAIN_GRAPH_ROUTE,
    ) {
        splashScreen(onNavigationEvent = onNavigationEvent)
        homeScreen(homeViewModel = homeViewModel)
        cardPaymentScreen(onNavigationEvent = onNavigationEvent)
        cashPaymentScreen(onNavigationEvent = onNavigationEvent)
        markOrderAsCompleteScreen(onNavigationEvent = onNavigationEvent)
        scanToPayScreen(onNavigationEvent = onNavigationEvent)
        emailReceiptScreen(onNavigationEvent = onNavigationEvent)
        eligibilityScreen(onNavigationEvent = onNavigationEvent)
        settingsScreen(onNavigationEvent = onNavigationEvent)
        ordersScreen(onNavigationEvent = onNavigationEvent)
        refundReasonScreen(onNavigationEvent = onNavigationEvent)
        paymentSuccessScreen(onNavigationEvent = onNavigationEvent)
        webViewScreen(onNavigationEvent = onNavigationEvent)
    }
}
