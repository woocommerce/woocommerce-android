package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import java.math.BigDecimal
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
        val itemId: Long = 0,
        val name: String = "",
        val variationId: Long = 0,
        val subtotal: BigDecimal = BigDecimal.ZERO,
        val total: BigDecimal = BigDecimal.ZERO,
        val totalTax: BigDecimal = BigDecimal.ZERO,
        val sku: String = "",
        val price: BigDecimal = BigDecimal.ZERO
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
            price ?: BigDecimal.ZERO
    )
}
