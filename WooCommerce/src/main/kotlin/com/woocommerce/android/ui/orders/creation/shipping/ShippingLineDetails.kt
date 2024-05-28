package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.model.ShippingMethod
import java.math.BigDecimal

data class ShippingLineDetails(
    val id: Long,
    val shippingMethod: ShippingMethod?,
    val amount: BigDecimal,
    val name: String
)
