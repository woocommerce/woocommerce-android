package com.woocommerce.android.ui.woopos.home.totals

sealed class WooPosTotalsUIEvent {
    data object OnNewTransactionClicked : WooPosTotalsUIEvent()
    data object RetryFailedTransactionClicked : WooPosTotalsUIEvent()
    data object GoBackToCheckoutAfterFailedPayment : WooPosTotalsUIEvent()
    data object RetryOrderCreationClicked : WooPosTotalsUIEvent()
    data object OnStartReceiptFlowClicked : WooPosTotalsUIEvent()
    data object OnCashPaymentClicked : WooPosTotalsUIEvent()
    data object ConnectReaderClicked : WooPosTotalsUIEvent()
    data object OnBackClicked : WooPosTotalsUIEvent()
}
