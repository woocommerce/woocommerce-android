package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersInMemoryCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.OrderBy
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import javax.inject.Inject

sealed class LoadOrdersResult {
    data class Success(val orders: List<Order>) : LoadOrdersResult()
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
        emit(LoadOrdersResult.Success(cached))

        val result = restClient.fetchOrders(
            site = selectedSite.get(),
            count = POS_ORDERS_PAGE_SIZE,
            page = 1,
            orderBy = OrderBy.DATE,
            sortOrder = OrderRestClient.SortOrder.DESCENDING,
            statusFilter = null,
            createdVia = "pos-rest-api"
        )

        if (result.isError) {
            emit(LoadOrdersResult.Error(result.error.message))
        } else {
            val mapped = result.orders.toAppModels()
            ordersCache.addAll(mapped)
            emit(LoadOrdersResult.Success(result.orders.toAppModels()))
        }
    }

    private suspend fun List<OrderEntity>?.toAppModels(): List<Order> = this?.map {
        orderMapper.toAppModel(it)
    } ?: emptyList()
}
