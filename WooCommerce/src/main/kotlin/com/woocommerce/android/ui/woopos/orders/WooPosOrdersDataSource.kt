package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.Refund
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    data class SuccessCache(val ordersWithRefunds: Map<Order, RefundFetchResult>) : LoadOrdersResult()
    data class SuccessRemote(val ordersWithRefunds: Map<Order, RefundFetchResult>) : LoadOrdersResult()
    data class Error(val message: String) : LoadOrdersResult()
}

sealed class SearchOrdersResult {
    data class Success(val ordersWithRefunds: Map<Order, RefundFetchResult>) : SearchOrdersResult()
    data class Error(val message: String) : SearchOrdersResult()
}

sealed class RefundFetchResult {
    data class Success(val refunds: List<Refund>) : RefundFetchResult()
    object Error : RefundFetchResult()
}

class WooPosOrdersDataSource @Inject constructor(
    private val restClient: OrderRestClient,
    private val selectedSite: SelectedSite,
    private val orderMapper: OrderMapper,
    private val ordersCache: WooPosOrdersInMemoryCache,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds
) {
    private val canLoadMore = AtomicBoolean(false)
    private val page = AtomicInteger(1)

    val hasMorePages: Boolean
        get() = canLoadMore.get()

    companion object {
        const val POS_ORDERS_PAGE_SIZE = 25
        private const val UNKNOWN_ERROR = "Unknown error"
    }

    fun loadOrders(): Flow<LoadOrdersResult> = flow {
        val cached = ordersCache.getAll()
        if (cached.isNotEmpty()) {
            val cachedWithRefunds = fetchRefundsForOrders(cached)
            emit(LoadOrdersResult.SuccessCache(cachedWithRefunds))
        }

        val result = loadFirstPage()
        result.onSuccess { orders ->
            ordersCache.setAll(orders)
            val ordersWithRefunds = fetchRefundsForOrders(orders)
            emit(LoadOrdersResult.SuccessRemote(ordersWithRefunds))
        }.onFailure {
            emit(LoadOrdersResult.Error(it.message ?: UNKNOWN_ERROR))
        }
    }

    suspend fun searchOrders(searchQuery: String): SearchOrdersResult {
        val result = loadFirstPage(searchQuery)
        return result.fold(
            onSuccess = { orders ->
                val ordersWithRefunds = fetchRefundsForOrders(orders)
                SearchOrdersResult.Success(ordersWithRefunds)
            },
            onFailure = { SearchOrdersResult.Error(it.message ?: UNKNOWN_ERROR) }
        )
    }

    suspend fun loadMore(searchQuery: String? = null): Result<Map<Order, RefundFetchResult>> =
        withContext(Dispatchers.IO) {
            loadNextPage(searchQuery).map { orders ->
                fetchRefundsForOrders(orders)
            }
        }

    suspend fun refreshOrderById(orderId: Long): Result<Order> {
        val site = selectedSite.get()
        val payload = restClient.fetchSingleOrder(site, orderId)
        return if (payload.error == null) {
            val entity = payload.orderWithMeta.first
            val order = orderMapper.toAppModel(entity)

            updateCachedOrderIfPresent(order)

            Result.success(order)
        } else {
            Result.failure(Throwable("[${payload.error.type}] ${payload.error.message}"))
        }
    }

    private fun updateCachedOrderIfPresent(order: Order) {
        val current = ordersCache.getAll()
        val idx = current.indexOfFirst { it.id == order.id }
        if (idx < 0) return

        val updated = current.toMutableList()
        updated[idx] = order
        ordersCache.setAll(updated)
    }

    private suspend fun loadFirstPage(searchQuery: String? = null): Result<List<Order>> {
        page.set(1)
        canLoadMore.set(false)
        return fetchAndMap(searchQuery)
    }

    private suspend fun loadNextPage(searchQuery: String? = null): Result<List<Order>> {
        return fetchAndMap(searchQuery)
    }

    private suspend fun fetchAndMap(searchQuery: String? = null): Result<List<Order>> {
        val result = fetchOrdersFromRemote(page.get(), searchQuery)
        return if (result.isError) {
            Result.failure(result.error.toThrowable())
        } else {
            canLoadMore.set(result.canLoadMore)
            page.incrementAndGet()
            Result.success(result.orders.toAppModels())
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
        searchQuery = searchQuery
    )

    fun clearCache() = ordersCache.clear()

    private suspend fun List<OrderEntity>.toAppModels(): List<Order> = map {
        orderMapper.toAppModel(it)
    }

    private fun WCOrderStore.OrderError.toThrowable(): Throwable =
        Throwable("[$type] $message")

    private suspend fun fetchRefundsForOrders(orders: List<Order>): Map<Order, RefundFetchResult> =
        coroutineScope {
            orders.map { order ->
                async {
                    retrieveOrderRefunds(order).fold(
                        onSuccess = { refunds -> order to RefundFetchResult.Success(refunds) },
                        onFailure = { order to RefundFetchResult.Error }
                    )
                }
            }.awaitAll().toMap()
        }
}
