package com.woocommerce.android.ui.woopos.scantopay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

const val SCAN_TO_PAY_ROUTE_ORDER_ID_KEY = "orderId"

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
    data object PayInPersonSelected : WooPosScanToPayState()

    @Parcelize
    data class Failed(
        val message: String,
    ) : WooPosScanToPayState()
}
