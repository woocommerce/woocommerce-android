package com.woocommerce.android.model

import java.math.BigDecimal

data class GiftCardSummary(
    val id: Long,
    val code: String,
    val used: BigDecimal
)
