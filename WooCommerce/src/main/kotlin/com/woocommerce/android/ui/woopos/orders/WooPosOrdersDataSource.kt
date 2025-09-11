package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.OrderBy
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
    private val canLoadMore = AtomicBoolean(false)
    private val offset = AtomicInteger(0)

    val hasMorePages: Boolean
        get() = canLoadMore.get()

    companion object {
        const val POS_ORDERS_PAGE_SIZE = 25
    }

    fun loadOrders(): Flow<LoadOrdersResult> = flow {
        val cached = ordersCache.getAll()
        if (cached.isNotEmpty()) {
            emit(LoadOrdersResult.SuccessCache(cached))
        }

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
            ordersCache.setAll(mapped)
            emit(LoadOrdersResult.SuccessRemote(result.orders.toAppModels()))
        }
    }

    suspend fun loadMore(): Result<List<Order>> = withContext(Dispatchers.IO) {
        val result = restClient.fetchOrders(
            site = selectedSite.get(),
            count = POS_ORDERS_PAGE_SIZE,
            page = offset.get(),
            orderBy = OrderBy.DATE,
            sortOrder = OrderRestClient.SortOrder.DESCENDING,
            statusFilter = null,
            createdVia = "pos-rest-api"
        )

        if (result.isError) {
            return@withContext Result.failure(result.error.toThrowable())
        } else {
            canLoadMore.set(result.canLoadMore)
            offset.addAndGet(POS_ORDERS_PAGE_SIZE)

            val mapped = result.orders.toAppModels()
            return@withContext Result.success(mapped)
        }
    }

    fun clearCache() = ordersCache.clear()

    private suspend fun List<OrderEntity>.toAppModels(): List<Order> = map {
        orderMapper.toAppModel(it)
    }

    fun WCOrderStore.OrderError.toThrowable(): Throwable =
        Throwable("[$type] $message")
}
