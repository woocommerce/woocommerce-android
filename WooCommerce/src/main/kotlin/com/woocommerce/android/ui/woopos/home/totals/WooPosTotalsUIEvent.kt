package com.woocommerce.android.ui.woopos.home.totals

sealed class WooPosTotalsUIEvent {
    data object OnNewTransactionClicked : WooPosTotalsUIEvent()
    data object RetryFailedTransactionClicked : WooPosTotalsUIEvent()
    data object GoBackToCheckoutAfterFailedPayment : WooPosTotalsUIEvent()
    data object GoBackToCheckoutAfterFailedCouponValidation : WooPosTotalsUIEvent()
    data object OnRemoveCouponsClicked : WooPosTotalsUIEvent()
    data object GoBackToOrderEditAfterProductNotFound : WooPosTotalsUIEvent()
    data object OnRemoveProductsClicked : WooPosTotalsUIEvent()
    data object RetryOrderCreationClicked : WooPosTotalsUIEvent()
    data object OnStartReceiptFlowClicked : WooPosTotalsUIEvent()
    data object OnCashPaymentClicked : WooPosTotalsUIEvent()
    data object OnTapToPayClicked : WooPosTotalsUIEvent()
    data class OnAllPaymentMethodsVisibilityChanged(val isVisible: Boolean) : WooPosTotalsUIEvent()
    data object ConnectReaderClicked : WooPosTotalsUIEvent()
    data object OnBackClicked : WooPosTotalsUIEvent()
}
