package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.ui.products.ProductHelper
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundShippingLine
import java.math.RoundingMode.HALF_UP
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class Refund(
    val id: Long,
    val dateCreated: Date,
    val amount: BigDecimal,
    val reason: String?,
    val automaticGatewayRefund: Boolean,
    val items: List<Item>,
    val shippingLines: List<ShippingLine>
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
        val price: BigDecimal = BigDecimal.ZERO
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
            amount.roundError(),
            reason,
            automaticGatewayRefund,
            items.map { it.toAppModel() },
            shippingLineItems.map { it.toAppModel() }
    )
}

fun WCRefundItem.toAppModel(): Refund.Item {
    return Refund.Item(
            productId ?: -1,
            -quantity, // WCRefundItem.quantity is NEGATIVE
            itemId,
            name ?: "",
            variationId ?: -1,
            -subtotal.roundError(), // WCRefundItem.subtotal is NEGATIVE
            -(total ?: BigDecimal.ZERO).roundError(), // WCRefundItem.total is NEGATIVE
            -totalTax.roundError(), // WCRefundItem.totalTax is NEGATIVE
            sku ?: "",
            price?.roundError() ?: BigDecimal.ZERO
    )
}

fun WCRefundShippingLine.toAppModel(): Refund.ShippingLine {
    return Refund.ShippingLine(
        itemId = getRefundedShippingLineId(),
        methodId = methodId ?: "",
        methodTitle = methodTitle ?: "",
        totalTax = -totalTax.roundError(),      // WCRefundShippineLine.totalTax is NEGATIVE
        total = (total).roundError()            // WCREfundShippingLine.total is NEGATIVE
    )
}

/**
 * In a "WCRefundShippingLine" object, the id of the refunded shipping line is buried in "metaData" property like so:
 *
 * -------------------------------------------
 *
 * meta_data: [
 *     0: {
 *         display_key: "_refunded_item_id"
 *         display_value: "72"
 *         id: 591                         <-- Not what we want. This is the metadata id.
 *         key: "_refunded_item_id"
 *         value: "72"                     <-- This is the specific shipping line id that we want.
 *         }
 *    ]
 *
 * -------------------------------------------
 *
 * This method extracts that `value` property and returns it, or returns -1 if it can't find it.
 */
fun WCRefundShippingLine.getRefundedShippingLineId(): Long {
    if (this.metaData != null) {
        val resultJson = this.metaData!!.get(0).asJsonObject
        if (resultJson.has("value") && resultJson.get("value").isJsonPrimitive) {
            return resultJson.get("value").asLong
        }
    }
    return -1
}

fun List<Refund>.hasNonRefundedProducts(products: List<Order.Item>) =
    getMaxRefundQuantities(products).values.any { it > 0 }

fun List<Refund>.getNonRefundedProducts(
    products: List<Order.Item>
): List<Order.Item> {
    val leftoverProducts = getMaxRefundQuantities(products).filter { it.value > 0 }
    return products
        .filter { leftoverProducts.contains(it.uniqueId) }
        .map {
            val newQuantity = leftoverProducts[it.uniqueId]
            val quantity = it.quantity.toBigDecimal()
            val totalTax = if (quantity > BigDecimal.ZERO) {
                it.totalTax.divide(quantity, 2, HALF_UP)
            } else BigDecimal.ZERO

            it.copy(
                quantity = newQuantity ?: error("Missing product"),
                total = it.price.times(newQuantity.toBigDecimal()),
                totalTax = totalTax
            )
        }
}

/*
 * Calculates the max quantity for each item by subtracting the number of already-refunded items
 */
fun List<Refund>.getMaxRefundQuantities(
    products: List<Order.Item>
): Map<Long, Int> {
    val map = mutableMapOf<Long, Int>()
    val groupedRefunds = this.flatMap { it.items }.groupBy { it.uniqueId }
    products.map { item ->
        map[item.uniqueId] = item.quantity - (groupedRefunds[item.uniqueId]?.sumBy { it.quantity } ?: 0)
    }
    return map
}
