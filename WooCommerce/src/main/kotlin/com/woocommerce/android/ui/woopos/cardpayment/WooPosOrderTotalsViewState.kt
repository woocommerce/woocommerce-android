package com.woocommerce.android.ui.woopos.cardpayment

data class WooPosOrderTotalsViewState(
    val subtotal: String,
    val discount: String?,
    val taxes: String,
    val total: String,
)
