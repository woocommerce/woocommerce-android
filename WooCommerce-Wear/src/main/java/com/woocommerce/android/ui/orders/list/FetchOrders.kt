package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.extensions.combineWithTimeout
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.system.NetworkStatus
import com.woocommerce.android.ui.orders.OrdersRepository
import com.woocommerce.commons.wear.MessagePath.REQUEST_ORDERS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore.OrdersForWearablesResult.Success
import javax.inject.Inject

class FetchOrders @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val ordersRepository: OrdersRepository,
    private val networkStatus: NetworkStatus,
) {
    suspend operator fun invoke(
        selectedSite: SiteModel
    ): Flow<List<OrderEntity>> = selectOrdersDataSource(selectedSite)
        .combineWithTimeout { orders, isTimeout ->
            when {
                orders.isNotEmpty() -> orders
                isTimeout -> emptyList()
                else -> null
            }
        }.filterNotNull()

    private suspend fun selectOrdersDataSource(
        selectedSite: SiteModel
    ): Flow<List<OrderEntity>> {
        return if (networkStatus.isConnected()) {
            flow {
                when (val result = ordersRepository.fetchOrders(selectedSite)) {
                    is Success -> result.orders
                    else -> emptyList()
                }.let { emit(it) }
            }
        } else {
            phoneRepository.sendMessage(REQUEST_ORDERS)
            return ordersRepository.observeOrdersDataChanges()
        }
    }
}
