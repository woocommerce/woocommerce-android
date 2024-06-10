package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.util.DateUtils
import java.util.Date
import javax.inject.Inject

class WooPosCartCartRepository @Inject constructor(
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val dateUtils: DateUtils,
) {
    suspend fun createOrderWithProducts(
        productIds: List<Long>,
        onSuccess: (Order) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        check(productIds.isNotEmpty()) { "Cart is empty" }
        val order = Order.getEmptyOrder(
            dateCreated = dateUtils.getCurrentDateInSiteTimeZone() ?: Date(),
            dateModified = dateUtils.getCurrentDateInSiteTimeZone() ?: Date()
        ).apply {
            copy(
                status = Order.Status.Custom(Order.Status.AUTO_DRAFT),
                items = productIds.map { productId ->
                    productId to productIds.count { it == productId }
                }.map { productIdWithQuantity ->
                    Order.Item.EMPTY.copy(
                        productId = productIdWithQuantity.first,
                        quantity = productIdWithQuantity.second.toFloat(),
                    )
                }
            )
        }

        orderCreateEditRepository.createOrUpdateOrder(order).fold(
            onSuccess = { onSuccess(it) },
            onFailure = { onFailure(it) }
        )
    }
}
