package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class WooPosOrdersDataSource @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val orderMapper: OrderMapper,
    private val ordersCache: WooPosOrdersCache,
) {
    fun loadOrders(): Flow<OrdersResult> = flow {
        val cached = ordersCache.getAll()
        emit(OrdersResult.Cached(cached))

        val result = fetchOrdersFromStore(page = 1)
        if (result.isError) {
            emit(OrdersResult.Remote(Result.failure(Exception(result.error.message))))
        } else {
            val mapped = result.model.toAppModels()
            ordersCache.addAll(mapped)
            emit(OrdersResult.Remote(Result.success(mapped)))
        }
    }

    private suspend fun fetchOrdersFromStore(
        page: Int
    ): WooResult<List<OrderEntity>> {
        return orderStore.fetchOrders(
            site = selectedSite.get(),
            count = 25,
            page = page,
            createdVia = "pos-rest-api"
        )
    }

    private suspend fun List<OrderEntity>?.toAppModels(): List<Order> = this?.map {
        orderMapper.toAppModel(it)
    } ?: emptyList()

    sealed class OrdersResult {
        data class Cached(val orders: List<Order>) : OrdersResult()
        data class Remote(val ordersResult: Result<List<Order>>) : OrdersResult()
    }
}
