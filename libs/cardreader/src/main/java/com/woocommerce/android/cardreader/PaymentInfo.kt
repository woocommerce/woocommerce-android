package com.woocommerce.android.cardreader

import java.math.BigDecimal

data class PaymentInfo(
    val paymentDescription: String,
    val orderId: Long,
    val amount: BigDecimal,
    val currency: String,
    val customerEmail: String?,
    val customerName: String?,
    val storeName: String?,
    val siteUrl: String?,
    internal val customerId: String? = null,
)
