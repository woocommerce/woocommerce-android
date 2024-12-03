package com.woocommerce.android.ui.woopos.home.totals

sealed class WooPosTotalsUIEvent {
    data object CollectPaymentClicked : WooPosTotalsUIEvent()
    data object OnNewTransactionClicked : WooPosTotalsUIEvent()
    data object RetryOrderCreationClicked : WooPosTotalsUIEvent()
    data object OnStartReceiptFlowClicked : WooPosTotalsUIEvent()
    data object OnSendReceiptClicked : WooPosTotalsUIEvent()
    data class OnEmailChanged(val email: String) : WooPosTotalsUIEvent()
}
