package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCartRepository @Inject constructor(
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val dateUtils: DateUtils,
    private val store: WCProductStore,
    private val site: SelectedSite,
) {
    suspend fun createOrderWithProducts(productIds: List<Long>): Result<Order> = withContext(IO) {
        check(productIds.isNotEmpty()) { "List of IDs is empty" }
        val order = Order.getEmptyOrder(
            dateCreated = dateUtils.getCurrentDateInSiteTimeZone() ?: Date(),
            dateModified = dateUtils.getCurrentDateInSiteTimeZone() ?: Date()
        ).copy(
            status = Order.Status.Custom(Order.Status.AUTO_DRAFT),
            items = productIds
                .groupingBy { it }
                .eachCount()
                .map { (productId, quantity) ->
                    val product = getProductById(productId)
                    Order.Item.EMPTY.copy(
                        itemId = 0L,
                        productId = productId,
                        variationId = 0L,
                        quantity = quantity.toFloat(),
                        total = EMPTY_TOTALS_SUBTOTAL_VALUE,
                        subtotal = EMPTY_TOTALS_SUBTOTAL_VALUE,
                        price = product?.price ?: BigDecimal.ZERO,
                        sku = product?.sku.orEmpty(),
                        attributesList = emptyList(),
                    )
                }
        )

        orderCreateEditRepository.createOrUpdateOrder(order)
    }

    suspend fun getProductById(productId: Long): Product? = withContext(IO) {
        store.getProductByRemoteId(site.getOrNull()!!, productId)?.toAppModel()
    }

    private companion object {
        /**
         * This magic value used to indicate that we don't want to send subtotals and totals
         * And let the backend to calculate them.
         */
        val EMPTY_TOTALS_SUBTOTAL_VALUE = -Double.MAX_VALUE.toBigDecimal()
    }
}
