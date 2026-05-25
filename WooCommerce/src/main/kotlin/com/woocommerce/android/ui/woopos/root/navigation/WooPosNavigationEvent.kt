package com.woocommerce.android.ui.woopos.root.navigation

import com.woocommerce.android.ui.woopos.cardpayment.CardPaymentSource
import com.woocommerce.android.ui.woopos.cashpayment.CashPaymentSource
import com.woocommerce.android.ui.woopos.paymentsuccess.PaymentSuccessSource
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability

sealed class WooPosNavigationEvent {
    data object OpenSplashScreen : WooPosNavigationEvent()
    data object ExitPosClicked : WooPosNavigationEvent()
    data object BackFromSplashClicked : WooPosNavigationEvent()
    data object OpenHomeFromSplash : WooPosNavigationEvent()
    data class OpenCashPayment(
        val orderId: Long,
        val source: CashPaymentSource = CashPaymentSource.CHECKOUT,
    ) : WooPosNavigationEvent()
    data class OpenMarkOrderAsPaid(val orderId: Long) : WooPosNavigationEvent()
    data class OpenCardPayment(
        val orderId: Long,
        val source: CardPaymentSource = CardPaymentSource.CHECKOUT,
        val showCashPaymentButton: Boolean = false,
    ) : WooPosNavigationEvent()
    data class OpenEmailReceipt(
        val orderId: Long,
        val receiptAlreadySent: Boolean = false,
    ) : WooPosNavigationEvent()
    data class OpenIssueRefund(
        val orderId: Long,
        val disablePartialRefund: Boolean = false,
    ) : WooPosNavigationEvent()
    data class OpenRefundReason(val orderId: Long, val initialReason: String = "") : WooPosNavigationEvent()
    data object GoBack : WooPosNavigationEvent()
    data class GoBackWithResult(val key: String, val value: Any) : WooPosNavigationEvent()
    data object OpenHomeFromCashPaymentAfterSuccessfulPayment : WooPosNavigationEvent()
    data object ReturnHomeFromCashPayment : WooPosNavigationEvent()
    data class OpenEligibilityScreenFromSplash(
        val reason: WooPosLaunchability.NonLaunchabilityReason
    ) : WooPosNavigationEvent()
    data object OpenSettings : WooPosNavigationEvent()
    data object OpenOrders : WooPosNavigationEvent()
    data class OpenOrderDetails(val orderId: Long) : WooPosNavigationEvent()
    data object OpenBookings : WooPosNavigationEvent()
    data class NavigateToCashPayment(val orderId: Long, val source: CashPaymentSource) : WooPosNavigationEvent()
    data class NavigateBackToBookingsAfterPayment(val key: String, val value: Any) : WooPosNavigationEvent()
    data class OpenBookingNote(val bookingId: Long) : WooPosNavigationEvent()
    data class OpenPaymentSuccess(
        val orderId: Long,
        val source: PaymentSuccessSource,
    ) : WooPosNavigationEvent()
    data class OpenWebView(
        val url: String,
        val title: String = "",
    ) : WooPosNavigationEvent()
}
