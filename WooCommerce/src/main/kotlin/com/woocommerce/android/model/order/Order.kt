package com.woocommerce.android.model.order

import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

data class Order(
    val remoteOrderId: Long,
    val number: String,
    val localSiteId: Int,
    val dateCreated: Date?,
    val dateModified: Date?,
    val status: CoreOrderStatus,
    val total: BigDecimal,
    val totalTax: BigDecimal,
    val shippingTotal: BigDecimal,
    val discountTotal: BigDecimal,
    val refundTotal: BigDecimal,
    val currency: String,
    val customerNote: String,
    val discountCodes: String,
    val paymentMethod: String,
    val paymentMethodTitle: String,
    val pricesIncludeTax: Boolean,
    val billingAddress: OrderAddress,
    val shippingAddress: OrderAddress,
    val items: List<Item>
) {
    data class Item(
        val productId: Long,
        val name: String,
        val price: BigDecimal,
        val quantity: Float,
        val subtotal: BigDecimal,
        val totalTax: BigDecimal,
        val total: BigDecimal,
        val variationId: Long
    )
}

fun WCOrderModel.toAppModel(): Order {
    return Order(
        this.remoteOrderId,
        this.number,
        this.localSiteId,
        DateTimeUtils.dateUTCFromIso8601(this.dateCreated),
        DateTimeUtils.dateUTCFromIso8601(this.dateModified),
        CoreOrderStatus.fromValue(this.status) ?: CoreOrderStatus.PENDING,
        this.total.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        this.totalTax.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        this.shippingTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            this.discountTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        this.refundTotal.toBigDecimal(),
        this.currency,
        this.customerNote,
        this.discountCodes,
        this.paymentMethod,
        this.paymentMethodTitle,
        this.pricesIncludeTax,
        this.getBillingAddress(),
        this.getShippingAddress(),
        getLineItemList()
                .filter { it.productId != null }
                .map {
                    Order.Item(
                        it.productId!!,
                        it.name ?: "",
                        it.price?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        it.quantity ?: 0f,
                        it.subtotal?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        it.variationId ?: 0
                    )
        }
    )
}
