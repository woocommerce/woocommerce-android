package com.woocommerce.android.ui.woopos.root.navigation

import androidx.activity.ComponentActivity
import androidx.navigation.NavHostController
import com.woocommerce.android.ui.woopos.cardpayment.navigateToCardPaymentScreen
import com.woocommerce.android.ui.woopos.cashpayment.navigateToCashPaymentScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.authenticatedwebview.navigateToWebViewScreen
import com.woocommerce.android.ui.woopos.emailreceipt.navigateToEmailReceipt
import com.woocommerce.android.ui.woopos.home.navigateToEligibilityScreen
import com.woocommerce.android.ui.woopos.home.navigateToHomeScreen
import com.woocommerce.android.ui.woopos.home.navigateToHomeScreenAfterSuccessfulCashPayment
import com.woocommerce.android.ui.woopos.home.navigateToHomeScreenIfHomeScreenNotOpen
import com.woocommerce.android.ui.woopos.markorderascomplete.navigateToMarkOrderAsCompleteScreen
import com.woocommerce.android.ui.woopos.orders.details.refund.navigateToIssueRefundScreen
import com.woocommerce.android.ui.woopos.orders.details.refund.navigateToRefundReason
import com.woocommerce.android.ui.woopos.orders.navigateToOrderDetailsScreen
import com.woocommerce.android.ui.woopos.orders.navigateToOrdersScreen
import com.woocommerce.android.ui.woopos.paymentsuccess.navigateToPaymentSuccessScreen
import com.woocommerce.android.ui.woopos.scantopay.navigateToScanToPayScreen
import com.woocommerce.android.ui.woopos.settings.navigateToSettingsScreen
import com.woocommerce.android.ui.woopos.splash.navigateToSplashScreen

@Suppress("CyclomaticComplexMethod")
fun NavHostController.handleNavigationEvent(
    event: WooPosNavigationEvent,
    activity: ComponentActivity,
) {
    when (event) {
        is WooPosNavigationEvent.OpenSplashScreen -> navigateToSplashScreen()
        is WooPosNavigationEvent.ExitPosClicked,
        is WooPosNavigationEvent.BackFromSplashClicked -> activity.finish()

        is WooPosNavigationEvent.OpenHomeFromSplash -> navigateToHomeScreen()
        is WooPosNavigationEvent.OpenCashPayment -> navigateToCashPaymentScreen(event.orderId, event.source)
        is WooPosNavigationEvent.OpenMarkOrderAsPaid -> navigateToMarkOrderAsCompleteScreen(event.orderId)
        is WooPosNavigationEvent.OpenScanToPay -> navigateToScanToPayScreen(event.orderId)

        is WooPosNavigationEvent.OpenCardPayment ->
            navigateToCardPaymentScreen(event.orderId, event.source, event.showCashPaymentButton)

        is WooPosNavigationEvent.GoBackWithResult -> {
            previousBackStackEntry
                ?.savedStateHandle
                ?.set(event.key, event.value)
            popBackStack()
        }

        is WooPosNavigationEvent.GoBack -> popBackStack()

        is WooPosNavigationEvent.OpenHomeFromCashPaymentAfterSuccessfulPayment ->
            navigateToHomeScreenAfterSuccessfulCashPayment()

        is WooPosNavigationEvent.OpenEmailReceipt ->
            navigateToEmailReceipt(event.orderId, event.receiptAlreadySent)

        is WooPosNavigationEvent.OpenIssueRefund ->
            navigateToIssueRefundScreen(event.orderId, event.disablePartialRefund)

        is WooPosNavigationEvent.OpenRefundReason ->
            navigateToRefundReason(event.orderId, event.initialReason)

        WooPosNavigationEvent.ReturnHomeFromCashPayment ->
            navigateToHomeScreenIfHomeScreenNotOpen()

        is WooPosNavigationEvent.OpenEligibilityScreenFromSplash ->
            navigateToEligibilityScreen(event.reason)

        is WooPosNavigationEvent.OpenSettings ->
            navigateToSettingsScreen()

        is WooPosNavigationEvent.OpenOrders ->
            navigateToOrdersScreen()

        is WooPosNavigationEvent.OpenOrderDetails ->
            navigateToOrderDetailsScreen(event.orderId)

        is WooPosNavigationEvent.NavigateToCashPayment -> {
            navigateToCashPaymentScreen(event.orderId, event.source)
        }

        is WooPosNavigationEvent.OpenPaymentSuccess ->
            navigateToPaymentSuccessScreen(
                event.orderId,
                event.source,
            )

        is WooPosNavigationEvent.OpenWebView ->
            navigateToWebViewScreen(event.url, event.title)
    }
}
