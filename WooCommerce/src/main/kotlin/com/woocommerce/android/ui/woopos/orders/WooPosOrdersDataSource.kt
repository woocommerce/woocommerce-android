package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.OrderBy
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import javax.inject.Inject

sealed class LoadOrdersResult {
    data class SuccessCache(val orders: List<Order>) : LoadOrdersResult()
    data class SuccessRemote(val orders: List<Order>) : LoadOrdersResult()
    data class Error(val message: String) : LoadOrdersResult()
}

class WooPosOrdersDataSource @Inject constructor(
    private val restClient: OrderRestClient,
    private val selectedSite: SelectedSite,
    private val orderMapper: OrderMapper,
    private val ordersCache: WooPosOrdersInMemoryCache
) {
    companion object {
        const val POS_ORDERS_PAGE_SIZE = 25
    }

    fun loadOrders(): Flow<LoadOrdersResult> = flow {
        val cached = ordersCache.getAll()
        if (cached.isNotEmpty()) {
            emit(LoadOrdersResult.SuccessCache(cached))
        }

        val result = fetchOrdersFromRemote(searchQuery = null, page = 1)

        if (result.isError) {
            emit(LoadOrdersResult.Error(result.error?.message ?: "Unknown error"))
        } else {
            val mapped = result.orders.toAppModels()
            ordersCache.setAll(mapped)
            emit(LoadOrdersResult.SuccessRemote(mapped))
        }
    }

    suspend fun searchOrders(searchQuery: String): LoadOrdersResult {
        val result = fetchOrdersFromRemote(searchQuery = searchQuery, page = 1)

        return if (result.isError) {
            LoadOrdersResult.Error(result.error?.message ?: "Unknown error")
        } else {
            val mapped = result.orders.toAppModels()
            LoadOrdersResult.SuccessRemote(mapped)
        }
    }

    private suspend fun fetchOrdersFromRemote(
        page: Int,
        searchQuery: String?
    ) = restClient.fetchOrders(
        site = selectedSite.get(),
        count = POS_ORDERS_PAGE_SIZE,
        page = page,
        orderBy = OrderBy.DATE,
        sortOrder = OrderRestClient.SortOrder.DESCENDING,
        statusFilter = null,
        createdVia = "pos-rest-api",
        searchQuery = searchQuery,
    )

    fun clearCache() = ordersCache.clear()

    private suspend fun List<OrderEntity>.toAppModels(): List<Order> = map {
        orderMapper.toAppModel(it)
    }
}
