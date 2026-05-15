package com.woocommerce.android.ui.woopos.scantopay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosScanToPayState : Parcelable {
    @Parcelize
    data object Loading : WooPosScanToPayState()

    @Parcelize
    data class ShowingQR(
        val paymentUrl: String,
        val totalText: String,
    ) : WooPosScanToPayState()

    @Parcelize
    data object PaymentDetected : WooPosScanToPayState()

    @Parcelize
    data class Failed(
        val message: String,
        val retryable: Boolean,
    ) : WooPosScanToPayState()
}
