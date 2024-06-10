package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.util.DateUtils
import java.util.Date
import javax.inject.Inject

class WooPosCartRepository @Inject constructor(
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val dateUtils: DateUtils,
) {
    suspend fun createOrderWithProducts(productIds: List<Long>): Result<Order> {
        check(productIds.isNotEmpty()) { "Cart is empty" }
        val order = Order.getEmptyOrder(
            dateCreated = dateUtils.getCurrentDateInSiteTimeZone() ?: Date(),
            dateModified = dateUtils.getCurrentDateInSiteTimeZone() ?: Date()
        ).copy(
            status = Order.Status.Custom(Order.Status.AUTO_DRAFT),
            items = productIds
                .groupingBy { it }
                .eachCount()
                .map { (productId, quantity) ->
                    Order.Item.EMPTY.copy(
                        productId = productId,
                        quantity = quantity.toFloat(),
                        total = EMPTY_TOTALS_SUBTOTAL_VALUE,
                        subtotal = EMPTY_TOTALS_SUBTOTAL_VALUE,
                    )
                }
        )

        return orderCreateEditRepository.createOrUpdateOrder(order)
    }

    private companion object {
        /**
         * This magic value used to indicate that we don't want to send subtotals and totals
         * And let the backend to calculate them.
         */
        val EMPTY_TOTALS_SUBTOTAL_VALUE = -Double.MAX_VALUE.toBigDecimal()
    }
}
