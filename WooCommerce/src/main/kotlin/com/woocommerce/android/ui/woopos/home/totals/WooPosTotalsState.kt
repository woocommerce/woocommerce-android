package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosTotalsState(
    val orderId: Long?,
    val isCollectPaymentButtonEnabled: Boolean,
    val orderTotal: java.math.BigDecimal,
    val orderSubtotal: java.math.BigDecimal,
    val orderTax: java.math.BigDecimal,
    val isLoading: Boolean
) : Parcelable
