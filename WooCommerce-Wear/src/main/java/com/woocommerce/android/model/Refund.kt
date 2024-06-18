package com.woocommerce.android.model

import android.os.Parcelable
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel

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
    ) : Parcelable

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

fun WCRefundModel.WCRefundShippingLine.toAppModel(): Refund.ShippingLine {
    return Refund.ShippingLine(
        itemId = metaData?.get(0)?.value?.toString()?.toLongOrNull() ?: -1,
        methodId = methodId ?: "",
        methodTitle = methodTitle ?: "",
        totalTax = -totalTax, // WCRefundShippineLine.totalTax is NEGATIVE
        total = (total) // WCREfundShippingLine.total is NEGATIVE
    )
}

fun WCRefundModel.WCRefundFeeLine.toAppModel(): Refund.FeeLine {
    return Refund.FeeLine(
        id = metaData?.get(0)?.value?.toString()?.toLongOrNull() ?: -1,
        name = name,
        totalTax = -totalTax, // WCRefundFeeLine.totalTax is NEGATIVE
        total = (total), // WCRefundFeeLine.total is NEGATIVE
    )
}
