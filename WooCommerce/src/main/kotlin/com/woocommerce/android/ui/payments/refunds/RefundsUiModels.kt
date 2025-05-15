package com.woocommerce.android.ui.payments.refunds

import android.os.Parcelable
import com.woocommerce.android.model.Order
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.RefundRequestTax
import java.math.RoundingMode.HALF_UP

@Parcelize
data class ProductRefundListItem(
    val orderItem: Order.Item,
    val maxQuantity: Float = 0f,
    val quantity: Int = 0,
    val subtotal: String? = null,
    val taxes: String? = null
) : Parcelable {
    val availableRefundQuantity
        get() = maxQuantity.toInt()

    fun toDataModel(): RefundRequestItem {
        return RefundRequestItem(
            itemId = orderItem.itemId,
            quantity = quantity,
            refundTotal = quantity.toBigDecimal().times(orderItem.price),
            refundTax = listOf(
                RefundRequestTax(
                    taxRateId = 0L,
                    refundTotal = orderItem.totalTax.divide(orderItem.quantity.toBigDecimal(), 2, HALF_UP)
                        .times(quantity.toBigDecimal())
                )
            )
        )
    }
}

@Parcelize
data class ShippingRefundListItem(
    val shippingLine: Order.ShippingLine
) : Parcelable {
    fun toDataModel(): RefundRequestItem {
        return RefundRequestItem(
            shippingLine.itemId,
            quantity = 1, // Hardcoded because a shipping line always has a quantity of 1
            refundTotal = shippingLine.total,
            refundTax = listOf(
                RefundRequestTax(
                    taxRateId = 0L,
                    refundTotal = shippingLine.totalTax
                )
            )
        )
    }
}

@Parcelize
data class FeeRefundListItem(
    val feeLine: Order.FeeLine
) : Parcelable {
    fun toDataModel(): RefundRequestItem {
        return RefundRequestItem(
            feeLine.id,
            quantity = 1, // Hardcoded because a fee line always has a quantity of 1
            refundTotal = feeLine.total,
            refundTax = listOf(
                RefundRequestTax(
                    taxRateId = 0L,
                    refundTotal = feeLine.totalTax,
                )
            )
        )
    }
}
