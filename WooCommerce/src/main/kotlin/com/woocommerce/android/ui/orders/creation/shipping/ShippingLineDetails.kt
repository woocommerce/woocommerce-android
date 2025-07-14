package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingMethod
import java.math.BigDecimal

data class ShippingLineDetails(
    val id: Long,
    val shippingMethod: ShippingMethod?,
    val amount: BigDecimal,
    val name: String
)

fun List<Order.ShippingLine>.toShippingLineDetails(
    shippingMethods: List<ShippingMethod>,
): List<ShippingLineDetails>? {
    if (this.isEmpty()) {
        return null
    }
    val filteredShippingLines = this.filter { line -> line.methodId != null }
    val shippingMethodsMap = shippingMethods.associateBy { it.id }
    return filteredShippingLines.map { shippingLine ->
        val method = shippingLine.methodId?.let {
            if (it == " ") {
                shippingMethodsMap[ShippingMethodsRepository.NA_ID]
            } else {
                shippingMethodsMap[it]
            }
        }

        ShippingLineDetails(
            id = shippingLine.itemId,
            name = shippingLine.methodTitle,
            shippingMethod = method,
            amount = shippingLine.total
        )
    }
}
