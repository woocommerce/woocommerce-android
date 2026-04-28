package com.woocommerce.android.ui.woopos.cardpayment

sealed class WooPosCardPaymentState {
    data object Initiating : WooPosCardPaymentState()

    sealed class Collecting : WooPosCardPaymentState() {
        abstract val orderTotals: WooPosOrderTotalsViewState

        data class Preparing(
            val title: String,
            val subtitle: String,
            override val orderTotals: WooPosOrderTotalsViewState,
        ) : Collecting()

        data class ReadyForPayment(
            val title: String,
            val subtitle: String,
            override val orderTotals: WooPosOrderTotalsViewState,
        ) : Collecting()

        data class ReaderDisconnected(
            val title: String,
            val subtitle: String,
            val actionButtonLabel: String,
            override val orderTotals: WooPosOrderTotalsViewState,
        ) : Collecting()
    }

    data class ConnectingTapToPay(
        val title: String,
        val subtitle: String,
    ) : WooPosCardPaymentState()

    data class PaymentInProgress(
        val title: String,
        val subtitle: String,
    ) : WooPosCardPaymentState()

    data class PaymentFailed(
        val title: String,
        val subtitle: String,
        val actionButtonLabel: String? = null,
        val isDismissButtonVisible: Boolean,
    ) : WooPosCardPaymentState()
}

data class WooPosOrderTotalsViewState(
    val subtotal: String,
    val discount: String?,
    val taxes: String,
    val total: String,
)
