package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class WooPosTotalsRepository @Inject constructor(
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val dateUtils: DateUtils,
) {
    private var orderCreationJob: Deferred<Result<Order>>? = null

    suspend fun createOrderWithProducts(productIds: List<Long>): Result<Order> {
        check(productIds.isNotEmpty()) { "List of IDs is empty" }

        orderCreationJob?.cancel()

        return withContext(IO) {
            productIds.forEach { productId ->
                require(productId >= 0) { "Invalid product ID: $productId" }
            }

            orderCreationJob = async {
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
                                itemId = 0L,
                                productId = productId,
                                variationId = 0L,
                                quantity = quantity.toFloat(),
                                total = EMPTY_TOTALS_SUBTOTAL_VALUE,
                                subtotal = EMPTY_TOTALS_SUBTOTAL_VALUE,
                                price = EMPTY_TOTALS_SUBTOTAL_VALUE,
                                attributesList = emptyList(),
                            )
                        }
                )

                orderCreateEditRepository.createOrUpdateOrder(order)
            }
            orderCreationJob!!.await()
        }
    }

    private companion object {
        /**
         * This magic value used to indicate that we don't want to send subtotals and totals
         * And let the backend to calculate them.
         */
        val EMPTY_TOTALS_SUBTOTAL_VALUE = -Double.MAX_VALUE.toBigDecimal()
    }
}
