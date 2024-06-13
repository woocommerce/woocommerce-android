package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosTotalsState(
    val orderId: Long?,
    val isCollectPaymentButtonEnabled: Boolean,
    var orderTotals: java.math.BigDecimal,
    var orderTax: java.math.BigDecimal,
    // orderCalculationState = TotalsCalculationState.CALCULATED
) : Parcelable
