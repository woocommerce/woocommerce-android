package com.woocommerce.android.ui.orders.creation.product.discount

import com.woocommerce.android.model.Order
import java.math.BigDecimal
import javax.inject.Inject

class CalculateItemDiscountAmount @Inject constructor() {
    operator fun invoke(item: Order.Item): BigDecimal {
        return (item.subtotal - item.total) / item.quantity.toBigDecimal()
    }
}