package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosTotalsViewState : Parcelable {
    data object Loading : WooPosTotalsViewState()

    data class Totals(
        var orderSubtotalText: String,
        var orderTaxText: String,
        var orderTotalText: String,
    ) : WooPosTotalsViewState()

    data class PaymentSuccess(var orderTotalText: String) : WooPosTotalsViewState()

    data class Error(val message: String) : WooPosTotalsViewState()
}
