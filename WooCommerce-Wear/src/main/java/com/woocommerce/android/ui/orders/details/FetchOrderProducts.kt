package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.extensions.combineWithTimeout
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.system.NetworkStatus
import com.woocommerce.android.ui.orders.OrdersRepository
import com.woocommerce.android.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Error
import com.woocommerce.android.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Finished
import com.woocommerce.android.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Waiting
import com.woocommerce.commons.MessagePath.REQUEST_ORDER_PRODUCTS
import com.woocommerce.commons.WearOrderedProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject
import kotlinx.coroutines.flow.flow

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
            networkStatus.isConnected() -> fetchOrderedProducts(selectedSite, orderId)
            phoneRepository.isPhoneConnectionAvailable() -> {
                phoneRepository.sendMessage(
                    REQUEST_ORDER_PRODUCTS,
                    orderId.toString().toByteArray()
                )
                ordersRepository.observeOrderProductsDataChanges(orderId, selectedSite.siteId)
            }
            else -> flowOf(emptyList()) // Retrieve stored data instead
        }
    }

    private suspend fun fetchOrderedProducts(
        selectedSite: SiteModel,
        orderId: Long
    ) = flow<List<WearOrderedProduct>> {
        val orderItems = ordersRepository.fetchSingleOrder(selectedSite, orderId)
            ?.getLineItemList()
            ?.map { it.toAppModel() }
            ?: emptyList()

        ordersRepository.fetchOrderRefunds(selectedSite, orderId)
            .getNonRefundedProducts(orderItems)
            .map {
                WearOrderedProduct(
                    amount = it.quantity.toString(),
                    total = it.total.toString(),
                    name = it.name
                )
            }
    }

    sealed class OrderProductsRequest {
        data object Error : OrderProductsRequest()
        data object Waiting : OrderProductsRequest()
        data class Finished(val products: List<WearOrderedProduct>) : OrderProductsRequest()
    }

    companion object {
        const val TIMEOUT_FOR_ORDER_PRODUCTS = 5000L
    }
}
