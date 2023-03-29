package com.woocommerce.android.model

import java.math.BigDecimal

data class GiftCard(
    val id: Long,
    val code: String,
    val used: BigDecimal
)
