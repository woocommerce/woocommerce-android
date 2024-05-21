package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.extensions.combineWithTimeout
import com.woocommerce.android.extensions.toWearOrder
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.system.NetworkStatus
import com.woocommerce.android.ui.orders.OrdersRepository
import com.woocommerce.android.ui.orders.list.FetchOrders.OrdersRequest.Error
import com.woocommerce.android.ui.orders.list.FetchOrders.OrdersRequest.Finished
import com.woocommerce.android.ui.orders.list.FetchOrders.OrdersRequest.Waiting
import com.woocommerce.commons.MessagePath.REQUEST_ORDERS
import com.woocommerce.commons.WearOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCWearableStore.OrdersForWearablesResult.Success
import javax.inject.Inject

class FetchOrders @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val ordersRepository: OrdersRepository,
    private val networkStatus: NetworkStatus,
) {
    suspend operator fun invoke(
        selectedSite: SiteModel
    ): Flow<OrdersRequest> = selectOrdersDataSource(selectedSite)
        .combineWithTimeout { orders, isTimeout ->
            when {
                orders.isNotEmpty() -> Finished(orders)
                isTimeout.not() -> Waiting
                else -> Error
            }
        }.distinctUntilChanged()
        .filterNotNull()

    private suspend fun selectOrdersDataSource(
        selectedSite: SiteModel
    ): Flow<List<WearOrder>> {
        return when {
            networkStatus.isConnected() -> flow {
                when (val result = ordersRepository.fetchOrders(selectedSite)) {
                    is Success -> result.orders
                    else -> emptyList()
                }.map { it.toWearOrder() }.let { emit(it) }
            }
            phoneRepository.isPhoneConnectionAvailable() -> {
                phoneRepository.sendMessage(REQUEST_ORDERS)
                ordersRepository.observeOrdersDataChanges(selectedSite.siteId)
            }
            else -> flow {
                val orders = ordersRepository.getStoredOrders(selectedSite)
                    .map { it.toWearOrder() }
                emit(orders)
            }
        }
    }

    sealed class OrdersRequest {
        data object Error : OrdersRequest()
        data object Waiting : OrdersRequest()
        data class Finished(val orders: List<WearOrder>) : OrdersRequest()
    }
}
