package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class TotalsUIState : Parcelable {
    data object Loading : TotalsUIState()

    data class Totals(
        var orderSubtotalText: String,
        var orderTaxText: String,
        var orderTotalText: String,
    ) : TotalsUIState()

    data class PaymentSuccess(
        var orderSubtotalText: String,
        var orderTaxText: String,
        var orderTotalText: String
    ) : TotalsUIState()

    data class Error(val message: String) : TotalsUIState()
}
