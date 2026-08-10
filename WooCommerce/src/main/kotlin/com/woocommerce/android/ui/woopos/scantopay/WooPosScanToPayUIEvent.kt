package com.woocommerce.android.ui.woopos.scantopay

sealed class WooPosScanToPayUIEvent {
    data object RetryClicked : WooPosScanToPayUIEvent()
    data object CancelClicked : WooPosScanToPayUIEvent()
    data object CollectOnRegisterClicked : WooPosScanToPayUIEvent()
}
