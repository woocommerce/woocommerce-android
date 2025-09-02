package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.OrderBy
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import javax.inject.Inject

private sealed class FetchOrdersResult {
    data class Success(val orders: List<OrderEntity>) : FetchOrdersResult()
    data class Error(val error: WooError) : FetchOrdersResult()
}

sealed class LoadOrdersResult {
    data class Cached(val orders: List<Order>) : LoadOrdersResult()
    data class Remote(val ordersResult: Result<List<Order>>) : LoadOrdersResult()
}

class WooPosOrdersDataSource @Inject constructor(
    private val restClient: OrderRestClient,
    private val selectedSite: SelectedSite,
    private val orderMapper: OrderMapper,
    private val ordersCache: WooPosOrdersCache
) {
    suspend fun loadOrders(): Flow<LoadOrdersResult> = flow {
        val cached = ordersCache.getAll()
        emit(LoadOrdersResult.Cached(cached))
        val result = fetchOrdersFromRemote(1)
        if (result is FetchOrdersResult.Error) {
            emit(LoadOrdersResult.Remote(Result.failure(Exception(result.error.message))))
        } else if (result is FetchOrdersResult.Success) {
            val mapped = result.orders.toAppModels()
            ordersCache.addAll(mapped)
            emit(LoadOrdersResult.Remote(Result.success(mapped)))
        }
    }

    private suspend fun fetchOrdersFromRemote(
        page: Int
    ): FetchOrdersResult {
        val result = restClient.fetchOrders(
            site = selectedSite.get(),
            count = 25,
            page = page,
            orderBy = OrderBy.DATE,
            sortOrder = OrderRestClient.SortOrder.DESCENDING,
            statusFilter = null,
            createdVia = "pos-rest-api"
        )

        return if (result.isError) {
            FetchOrdersResult.Error(
                WooError(API_ERROR, SERVER_ERROR, result.error.message)
            )
        } else {
            FetchOrdersResult.Success(result.orders)
        }
    }

    private suspend fun List<OrderEntity>?.toAppModels(): List<Order> = this?.map {
        orderMapper.toAppModel(it)
    } ?: emptyList()
}
