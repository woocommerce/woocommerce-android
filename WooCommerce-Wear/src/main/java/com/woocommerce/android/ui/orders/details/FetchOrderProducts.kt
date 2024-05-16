package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.extensions.combineWithTimeout
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.ui.orders.OrdersRepository
import com.woocommerce.commons.wear.MessagePath.REQUEST_ORDER_PRODUCTS
import com.woocommerce.commons.wear.orders.WearOrderProduct
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

class FetchOrderProducts @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val ordersRepository: OrdersRepository
) {
    suspend operator fun invoke(orderId: Long): Flow<List<WearOrderProduct>> {
        phoneRepository.sendMessage(
            REQUEST_ORDER_PRODUCTS,
            orderId.toString().toByteArray()
        )

        return ordersRepository.observeOrderProductsDataChanges(orderId)
            .combineWithTimeout { orderProducts, isTimeout ->
                when {
                    orderProducts.isNotEmpty() -> orderProducts
                    isTimeout -> emptyList()
                    else -> null
                }
            }.filterNotNull()
    }
}
