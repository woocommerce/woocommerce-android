package com.woocommerce.android.ui.orders.wooshippinglabels.models

import java.math.BigDecimal
import java.util.Date

data class PurchasedLabelModel(
    val labelId: Long,
    val tracking: String,
    val refundableAmount: BigDecimal,
    val status: ShippingLabelStatus,
    val created: Date?,
    val carrierId: String,
    val serviceName: String,
    val commercialInvoiceUrl: String,
    val isCommercialInvoiceSubmittedElectronically: Boolean,
    val packageName: String,
    val isLetter: Boolean,
    val productNames: List<String>,
    val productIds: List<Long>,
)

enum class ShippingLabelStatus {
    UNKNOWN,
    PURCHASE_IN_PROGRESS,
    PURCHASED,
    PURCHASE_ERROR,
    ANONYMIZED
}
