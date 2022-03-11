package com.woocommerce.android.cardreader.payments

import java.math.BigDecimal

data class RefundParams(
    val chargeId: String,
    val amount: BigDecimal,
    val currency: String
)
