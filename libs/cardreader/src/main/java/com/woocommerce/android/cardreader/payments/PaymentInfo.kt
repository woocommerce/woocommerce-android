package com.woocommerce.android.cardreader.payments

import java.math.BigDecimal

data class PaymentInfo(
    val paymentDescription: String,
    val statementDescriptor: String?,
    val orderId: Long,
    val amount: BigDecimal,
    val currency: String,
    val customerEmail: String?,
    val isPluginCanSendReceipt: Boolean,
    val customerName: String?,
    val storeName: String?,
    val siteUrl: String?,
    val orderKey: String?,
    val feeAmount: Long?,
    internal val countryCode: String? = null,
)
