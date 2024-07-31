package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.sumByBigDecimal
import com.woocommerce.android.ui.products.ProductHelper
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundFeeLine
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundShippingLine
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.util.Date

@Parcelize
data class Refund(
    val id: Long,
    val dateCreated: Date,
    val amount: BigDecimal,
    val reason: String?,
    val automaticGatewayRefund: Boolean,
    val items: List<Item>,
    val shippingLines: List<ShippingLine>,
    val feeLines: List<FeeLine>,
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
    ) : Parcelable {
        @IgnoredOnParcel
        val uniqueId: Long = ProductHelper.productOrVariationId(productId, variationId)
    }

    @Parcelize
    data class ShippingLine(
        val itemId: Long,
        val methodId: String,
        val methodTitle: String,
        val totalTax: BigDecimal,
        val total: BigDecimal
    ) : Parcelable

    @Parcelize
    data class FeeLine(
        val id: Long,
        val name: String,
        val totalTax: BigDecimal,
        val total: BigDecimal,
    ) : Parcelable

    fun getRefundMethod(
        paymentMethodTitle: String,
        isCashPayment: Boolean,
        defaultValue: String
    ): String {
        return when {
            paymentMethodTitle.isBlank() -> defaultValue
            automaticGatewayRefund || isCashPayment -> paymentMethodTitle
            else -> "$defaultValue - $paymentMethodTitle"
        }
    }
}

fun WCRefundModel.toAppModel(): Refund {
    return Refund(
        id,
        dateCreated,
        amount,
        reason,
        automaticGatewayRefund,
        items.map { it.toAppModel() },
        shippingLineItems.map { it.toAppModel() },
        feeLineItems.map { it.toAppModel() },
    )
}

fun WCRefundItem.toAppModel(): Refund.Item {
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

fun WCRefundShippingLine.toAppModel(): Refund.ShippingLine {
    return Refund.ShippingLine(
        itemId = metaData?.get(0)?.value?.toString()?.toLongOrNull() ?: -1,
        methodId = methodId ?: "",
        methodTitle = methodTitle ?: "",
        totalTax = -totalTax, // WCRefundShippineLine.totalTax is NEGATIVE
        total = (total) // WCREfundShippingLine.total is NEGATIVE
    )
}

fun WCRefundFeeLine.toAppModel(): Refund.FeeLine {
    return Refund.FeeLine(
        id = metaData?.get(0)?.value?.toString()?.toLongOrNull() ?: -1,
        name = name,
        totalTax = -totalTax, // WCRefundFeeLine.totalTax is NEGATIVE
        total = (total), // WCRefundFeeLine.total is NEGATIVE
    )
}

fun List<Refund>.getRefundedProductsAmount() = filter { it.items.isNotEmpty() }.sumByBigDecimal {
    it.items.sumByBigDecimal { item -> item.total }
}

fun List<Refund>.getNonRefundedProducts(
    products: List<Order.Item>
): List<Order.Item> {
    val leftoverProducts = getMaxRefundQuantities(products).filter { it.value > 0 }
    return products
        .filter { leftoverProducts.contains(it.itemId) }
        .map {
            val newQuantity = leftoverProducts[it.itemId]
            if (newQuantity == it.quantity) return@map it
            val quantity = it.quantity.toBigDecimal()
            val individualProductTax = if (quantity > BigDecimal.ZERO) {
                it.totalTax.divide(quantity, 2, HALF_UP)
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
    products: List<Order.Item>
): Map<Long, Float> {
    val map = mutableMapOf<Long, Float>()
    val groupedRefunds = this.flatMap { it.items }.groupBy { it.orderItemId }
    products.map { item ->
        map[item.itemId] = item.quantity - (groupedRefunds[item.itemId]?.sumOf { it.quantity } ?: 0)
    }
    return map
}
