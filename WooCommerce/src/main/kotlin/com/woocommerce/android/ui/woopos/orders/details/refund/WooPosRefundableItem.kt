package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

@Immutable
data class WooPosRefundableItem(
    val orderItemId: Long,
    val productId: Long,
    val variationId: Long,
    val name: String,
    val unitPrice: BigDecimal,
    val unitTax: BigDecimal,
    val formattedUnitPrice: String,
    val formattedUnitTax: String,
    val rowIndex: Int,
) {
    val uniqueId: String
        get() = "${orderItemId}_$rowIndex"
}
