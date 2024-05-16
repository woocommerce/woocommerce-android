package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.extensions.combineWithTimeout
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.ui.orders.OrdersRepository
import com.woocommerce.android.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Error
import com.woocommerce.android.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Finished
import com.woocommerce.android.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Waiting
import com.woocommerce.commons.wear.MessagePath.REQUEST_ORDER_PRODUCTS
import com.woocommerce.commons.wear.orders.WearOrderProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

class FetchOrderProducts @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val ordersRepository: OrdersRepository
) {
    suspend operator fun invoke(orderId: Long): Flow<OrderProductsRequest> {
        phoneRepository.sendMessage(
            REQUEST_ORDER_PRODUCTS,
            orderId.toString().toByteArray()
        )

        return ordersRepository.observeOrderProductsDataChanges(orderId)
            .combineWithTimeout { orderProducts, isTimeout ->
                when {
                    orderProducts.isNotEmpty() -> Finished(orderProducts)
                    isTimeout.not() -> Waiting
                    else -> Error
                }
            }.filterNotNull()
    }

    sealed class OrderProductsRequest {
        data object Error : OrderProductsRequest()
        data object Waiting : OrderProductsRequest()
        data class Finished(val products: List<WearOrderProduct>) : OrderProductsRequest()
    }
}
