package com.woocommerce.android.ui.woopos.orders

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class WooPosCalculateRefundSubtotal @Inject constructor() {
    operator fun invoke(refundableItems: List<WooPosRefundableItem>, numberOfDecimals: Int,): BigDecimal {
        return refundableItems
            .groupBy { it.orderItemId }
            .entries
            .sumOf { (_, items) ->
                val quantity = items.size.toBigDecimal()
                quantity.multiply(items.first().unitPrice)
            }
            .setScale(numberOfDecimals, RoundingMode.HALF_UP)
    }
}
