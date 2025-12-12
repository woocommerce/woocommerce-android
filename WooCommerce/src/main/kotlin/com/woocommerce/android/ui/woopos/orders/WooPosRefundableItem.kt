package com.woocommerce.android.ui.woopos.orders

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
    val lineTotal: BigDecimal
        get() = unitPrice

    val lineTax: BigDecimal
        get() = unitTax
}
