package com.woocommerce.android.ui.payments.refunds

import android.os.Parcelable
import com.woocommerce.android.model.Order
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.RefundRequestTax
import java.math.BigDecimal

sealed class RefundItem : Parcelable {
    abstract val itemId: Long
    abstract val quantity: Int?
    abstract val subtotal: BigDecimal
    abstract val taxes: List<TaxRefund>

    fun toDataModel(): RefundRequestItem {
        return RefundRequestItem(
            itemId = itemId,
            quantity = quantity,
            refundTotal = subtotal,
            refundTax = taxes.map {
                RefundRequestTax(
                    taxRateId = it.rateId,
                    refundTotal = it.tax
                )
            }
        )
    }
}

@Parcelize
data class TaxRefund(
    val rateId: Long,
    val tax: BigDecimal,
) : Parcelable

@Parcelize
data class ProductRefundListItem(
    val orderItem: Order.Item,
    val maxQuantity: Float = 0f,
    override val quantity: Int = 0,
    override val subtotal: BigDecimal = BigDecimal.ZERO,
    override val taxes: List<TaxRefund> = emptyList()
) : RefundItem() {
    override val itemId: Long
        get() = orderItem.itemId

    val availableRefundQuantity
        get() = maxQuantity.toInt()

    val taxesTotal: BigDecimal
        get() = taxes.sumOf { it.tax }
}

@Parcelize
data class ShippingRefundListItem(
    val shippingLine: Order.ShippingLine,
    val isSelected: Boolean = false
) : RefundItem() {
    override val itemId: Long
        get() = shippingLine.itemId
    override val quantity: Int?
        get() = null
    override val subtotal: BigDecimal
        get() = shippingLine.total
    override val taxes: List<TaxRefund>
        get() = shippingLine.taxes.map {
            TaxRefund(
                rateId = it.rateId,
                tax = it.taxAmount
            )
        }
}

@Parcelize
data class FeeRefundListItem(
    val feeLine: Order.FeeLine,
    val isSelected: Boolean = false
) : RefundItem() {
    override val itemId: Long
        get() = feeLine.id
    override val quantity: Int?
        get() = null
    override val subtotal: BigDecimal
        get() = feeLine.total
    override val taxes: List<TaxRefund>
        get() = feeLine.taxes.map {
            TaxRefund(
                rateId = it.rateId,
                tax = it.taxAmount
            )
        }
}
