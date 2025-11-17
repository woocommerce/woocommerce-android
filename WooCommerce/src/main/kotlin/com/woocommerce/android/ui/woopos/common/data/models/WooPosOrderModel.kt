package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import java.math.BigDecimal
import java.util.Date

data class WooPosOrderModel(
    val id: Long,
    val number: String,
    val dateCreated: Date,
    val status: Order.Status,

    val total: BigDecimal,
    val productsTotal: BigDecimal,
    val discountTotal: BigDecimal,
    val totalTax: BigDecimal,
    val shippingTotal: BigDecimal,
    val refundTotal: BigDecimal,

    val paymentMethodTitle: String,
    val customerEmail: String?,
    val billingEmail: String?,

    val items: List<LineItem>,
    val discountCode: String?
)  {
    data class LineItem(
        val itemId: Long,
        val productId: Long,
        val name: String,
        val quantity: Float,
        val total: BigDecimal,
    )
}
