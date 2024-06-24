package com.woocommerce.android.wear.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date

@Parcelize
data class Refund(
    val id: Long,
    val dateCreated: Date,
    val amount: BigDecimal,
    val reason: String?,
    val automaticGatewayRefund: Boolean,
    val items: List<Item>
) : Parcelable {
    @Parcelize
    data class Item(
        val productId: Long,
        val quantity: Int,
        val id: Long = 0,
        val name: String = "",
        val variationId: Long = 0,
        val subtotal: BigDecimal = BigDecimal.ZERO,
        val total: BigDecimal = BigDecimal.ZERO,
        val totalTax: BigDecimal = BigDecimal.ZERO,
        val sku: String = "",
        val price: BigDecimal = BigDecimal.ZERO,
        val orderItemId: Long = 0
    ) : Parcelable
}

fun WCRefundModel.toAppModel(): Refund {
    return Refund(
        id,
        dateCreated,
        amount,
        reason,
        automaticGatewayRefund,
        items.map { it.toAppModel() }
    )
}

fun WCRefundModel.WCRefundItem.toAppModel(): Refund.Item {
    return Refund.Item(
        productId ?: -1,
        -quantity, // WCRefundItem.quantity is NEGATIVE
        itemId,
        name ?: "",
        variationId ?: -1,
        -subtotal, // WCRefundItem.subtotal is NEGATIVE
        -(total ?: BigDecimal.ZERO), // WCRefundItem.total is NEGATIVE
        -totalTax, // WCRefundItem.totalTax is NEGATIVE
        sku ?: "",
        price ?: BigDecimal.ZERO,
        metaData?.get(0)?.value?.toString()?.toLongOrNull() ?: -1
    )
}

fun List<Refund>.getNonRefundedProducts(
    products: List<OrderItem>
): List<OrderItem> {
    val leftoverProducts = getMaxRefundQuantities(products).filter { it.value > 0 }
    return products
        .filter { leftoverProducts.contains(it.itemId) }
        .map {
            val newQuantity = leftoverProducts[it.itemId]
            if (newQuantity == it.quantity) return@map it
            val quantity = it.quantity.toBigDecimal()
            val individualProductTax = if (quantity > BigDecimal.ZERO) {
                it.totalTax.divide(quantity, 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            it.copy(
                quantity = newQuantity ?: error("Missing product"),
                total = it.price.times(newQuantity.toBigDecimal()),
                totalTax = individualProductTax.times(newQuantity.toBigDecimal())
            )
        }
}

/*
 * Calculates the max quantity for each item by subtracting the number of already-refunded items
 */
fun List<Refund>.getMaxRefundQuantities(
    products: List<OrderItem>
): Map<Long, Float> {
    val map = mutableMapOf<Long, Float>()
    val groupedRefunds = this.flatMap { it.items }.groupBy { it.orderItemId }
    products.map { item ->
        map[item.itemId] = item.quantity - (groupedRefunds[item.itemId]?.sumOf { it.quantity } ?: 0)
    }
    return map
}
