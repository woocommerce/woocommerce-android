package com.woocommerce.android.ui.woopos.home.totals

sealed class WooPosTotalsUIEvent {
    data object CollectPaymentClicked : WooPosTotalsUIEvent()
    data object OnNewTransactionClicked : WooPosTotalsUIEvent()
    data object RetryClicked : WooPosTotalsUIEvent()
}
