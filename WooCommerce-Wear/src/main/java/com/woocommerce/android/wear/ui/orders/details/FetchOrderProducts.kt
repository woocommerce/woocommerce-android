package com.woocommerce.android.wear.ui.orders.details

import com.woocommerce.android.wear.extensions.combineWithTimeout
import com.woocommerce.android.wear.model.OrderItem
import com.woocommerce.android.wear.model.Refund
import com.woocommerce.android.wear.model.getNonRefundedProducts
import com.woocommerce.android.wear.model.toAppModel
import com.woocommerce.android.wear.phone.PhoneConnectionRepository
import com.woocommerce.android.wear.system.NetworkStatus
import com.woocommerce.android.wear.ui.orders.OrdersRepository
import com.woocommerce.android.wear.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Error
import com.woocommerce.android.wear.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Finished
import com.woocommerce.android.wear.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Waiting
import com.woocommerce.commons.MessagePath.REQUEST_ORDER_PRODUCTS
import com.woocommerce.commons.WearOrderedProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class FetchOrderProducts @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val ordersRepository: OrdersRepository,
    private val networkStatus: NetworkStatus
) {
    suspend operator fun invoke(
        selectedSite: SiteModel,
        orderId: Long
    ): Flow<OrderProductsRequest> {
        return selectDataSource(selectedSite, orderId)
            .combineWithTimeout(TIMEOUT_FOR_ORDER_PRODUCTS) { orderProducts, isTimeout ->
                when {
                    orderProducts.isNotEmpty() -> Finished(orderProducts)
                    isTimeout.not() -> Waiting
                    else -> Error
                }
            }.filterNotNull()
    }

    private suspend fun selectDataSource(
        selectedSite: SiteModel,
        orderId: Long
    ): Flow<List<WearOrderedProduct>> {
        return when {
            networkStatus.isConnected() -> flow {
                ordersRepository.fetchOrderRefunds(selectedSite, orderId)
                    .asWearOrderedProducts(retrieveOrderLineItems(selectedSite, orderId))
                    .let { emit(it) }
            }

            phoneRepository.isPhoneConnectionAvailable() -> {
                phoneRepository.sendMessage(
                    REQUEST_ORDER_PRODUCTS,
                    orderId.toString().toByteArray()
                )
                ordersRepository.observeOrderProductsDataChanges(orderId, selectedSite.siteId)
            }

            else -> flow {
                ordersRepository.getOrderRefunds(selectedSite, orderId)
                    .asWearOrderedProducts(retrieveOrderLineItems(selectedSite, orderId))
                    .let { emit(it) }
            }
        }
    }

    private suspend fun retrieveOrderLineItems(
        selectedSite: SiteModel,
        orderId: Long
    ) = ordersRepository.getOrderFromId(selectedSite, orderId)
        ?.getLineItemList()
        ?.map { it.toAppModel() }
        ?: emptyList()

    private fun List<Refund>.asWearOrderedProducts(
        orderItems: List<OrderItem>
    ) = getNonRefundedProducts(orderItems).map {
        WearOrderedProduct(
            amount = it.quantity.toString(),
            total = it.total.toString(),
            name = it.name
        )
    }

    sealed class OrderProductsRequest {
        data object Error : OrderProductsRequest()
        data object Waiting : OrderProductsRequest()
        data class Finished(val products: List<WearOrderedProduct>) : OrderProductsRequest()
    }

    companion object {
        const val TIMEOUT_FOR_ORDER_PRODUCTS = 10000L
    }
}
