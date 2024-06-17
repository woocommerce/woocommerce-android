package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosTotalsState(
    val orderId: Long?,
    val isCollectPaymentButtonEnabled: Boolean,
    var orderSubtotalText: String,
    var orderTaxText: String,
    var orderTotalText: String,
    ) : Parcelable
