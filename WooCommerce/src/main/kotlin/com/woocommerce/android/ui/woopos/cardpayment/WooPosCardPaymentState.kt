package com.woocommerce.android.ui.woopos.cardpayment

sealed class WooPosCardPaymentState {
    data object Initiating : WooPosCardPaymentState()

    sealed class Collecting : WooPosCardPaymentState() {
        data class Preparing(
            val title: String,
            val subtitle: String,
        ) : Collecting()

        data class ReadyForPayment(
            val title: String,
            val subtitle: String,
        ) : Collecting()

        data class ReaderDisconnected(
            val title: String,
            val subtitle: String,
            val actionButtonLabel: String,
        ) : Collecting()
    }

    data class PaymentInProgress(
        val title: String,
        val subtitle: String,
    ) : WooPosCardPaymentState()

    data class PaymentFailed(
        val title: String,
        val subtitle: String,
        val actionButtonLabel: String,
        val isDismissButtonVisible: Boolean,
    ) : WooPosCardPaymentState()

    data class PaymentSuccess(
        val orderTotalText: String,
    ) : WooPosCardPaymentState()
}
