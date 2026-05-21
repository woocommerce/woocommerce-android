package com.woocommerce.android.ui.woopos.orders.details.refund

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class WooPosCalculateRefundSubtotal @Inject constructor() {
    operator fun invoke(refundableItems: List<WooPosRefundableItem>, numberOfDecimals: Int,): BigDecimal {
        val (lumpSumItems, productRows) = refundableItems.partition { it.isLumpSum }

        val productSubtotal = productRows
            .groupBy { it.orderItemId }
            .entries
            .sumOf { (_, items) ->
                val quantity = items.size.toBigDecimal()
                quantity.multiply(items.first().unitPrice).setScale(numberOfDecimals, RoundingMode.HALF_UP)
            }

        val lumpSumSubtotal = lumpSumItems.sumOf {
            it.unitPrice.setScale(numberOfDecimals, RoundingMode.HALF_UP)
        }

        return productSubtotal + lumpSumSubtotal
    }
}
