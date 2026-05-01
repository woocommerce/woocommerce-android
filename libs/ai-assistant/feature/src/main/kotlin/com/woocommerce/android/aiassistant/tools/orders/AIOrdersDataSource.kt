package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.tools.CachedLookupResult
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.last
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.UpdateOrderRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.OrderBy
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.SortOrder
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

internal class AIOrdersDataSource @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
) {
    data class OrdersPage(
        val orders: List<OrderEntity>,
        val canLoadMore: Boolean,
    )

    data class OrderPatch(
        val status: String? = null,
        val customerNote: String? = null,
        val billingEmail: String? = null,
    )

    data class BulkUpdateResult(
        val updatedIds: List<Long>,
        val failedOrders: List<WCOrderStore.UpdateOrdersStatusResult.FailedOrder>,
    )

    suspend fun fetchOrders(
        search: String? = null,
        status: String? = null,
        page: Int = 1,
        perPage: Int = PAGE_SIZE,
        customer: Long? = null,
        include: List<Long>? = null,
        after: String? = null,
        before: String? = null,
        orderby: String? = null,
        order: String? = null,
    ): Result<OrdersPage> {
        val site = selectedSite.get()
        val normalisedSearch = search?.trim()?.takeIf { it.isNotEmpty() }
        val normalisedStatus = status?.takeIf { it != "any" }
        val normalisedInclude = include?.takeIf { it.isNotEmpty() }
        val clampedPerPage = perPage.coerceIn(1, MAX_PAGE_SIZE)
        val resolvedOrderBy = OrderBy.entries.find { it.value == orderby } ?: OrderBy.DATE
        val resolvedSortOrder = SortOrder.entries.find { it.value == order } ?: SortOrder.DESCENDING

        val result = orderStore.fetchOrders(
            site = site,
            count = clampedPerPage,
            page = page,
            orderBy = resolvedOrderBy,
            sortOrder = resolvedSortOrder,
            statusFilter = normalisedStatus,
            deleteOldData = false,
            searchQuery = normalisedSearch,
            customer = customer,
            include = normalisedInclude,
            after = after,
            before = before,
        )
        return if (result.isError) {
            Result.failure(OnChangedException(result.error))
        } else {
            val orders = requireNotNull(result.model)
            Result.success(OrdersPage(orders = orders, canLoadMore = orders.size >= clampedPerPage))
        }
    }

    suspend fun getOrder(orderId: Long): Result<OrderEntity> {
        val site = selectedSite.get()
        orderStore.getOrderByIdAndSite(orderId, site)?.let { return Result.success(it) }
        val result = orderStore.fetchSingleOrderSync(site, orderId)
        return if (result.isError) {
            Result.failure(OnChangedException(result.error))
        } else {
            Result.success(requireNotNull(result.model))
        }
    }

    suspend fun getOrders(orderIds: List<Long>): Result<CachedLookupResult<OrderEntity>> {
        val ids = orderIds.distinct()
        if (ids.isEmpty()) {
            return Result.success(
                CachedLookupResult(
                    items = emptyList(),
                    cacheHitCount = 0,
                    cacheMissCount = 0,
                    fetchAttempted = false,
                    fetchFailed = false,
                )
            )
        }

        val site = selectedSite.get()
        val cachedOrders = orderStore.getOrdersByIdsAndSite(ids, site)
        val cachedIds = cachedOrders.map { it.orderId }.toSet()
        val idsToFetch = ids.filterNot { it in cachedIds }
        if (idsToFetch.isEmpty()) {
            return Result.success(
                CachedLookupResult(
                    items = cachedOrders,
                    cacheHitCount = cachedIds.size,
                    cacheMissCount = 0,
                    fetchAttempted = false,
                    fetchFailed = false,
                )
            )
        }

        val fetched = fetchOrders(include = idsToFetch, perPage = idsToFetch.size)
        return fetched.fold(
            onSuccess = {
                Result.success(
                    CachedLookupResult(
                        items = orderStore.getOrdersByIdsAndSite(ids, site),
                        cacheHitCount = cachedIds.size,
                        cacheMissCount = idsToFetch.size,
                        fetchAttempted = true,
                        fetchFailed = false,
                    )
                )
            },
            onFailure = {
                Result.success(
                    CachedLookupResult(
                        items = cachedOrders,
                        cacheHitCount = cachedIds.size,
                        cacheMissCount = idsToFetch.size,
                        fetchAttempted = true,
                        fetchFailed = true,
                    )
                )
            },
        )
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: String): Result<Unit> = runCatching {
        val site = selectedSite.get()
        val statusModel = WCOrderStatusModel(statusKey = newStatus)
        // last() handles two cases: only OptimisticUpdateResult (order not in Room, carries error)
        // or both Optimistic + RemoteUpdateResult (normal path, remote result governs).
        val lastResult = orderStore.updateOrderStatus(orderId, site, statusModel).last()
        if (lastResult.event.isError) {
            throw OnChangedException(requireNotNull(lastResult.event.error))
        }
    }

    suspend fun bulkUpdateOrders(orderIds: List<Long>, patch: OrderPatch): Result<BulkUpdateResult> {
        val site = selectedSite.get()
        val requests = orderIds.associateWith {
            UpdateOrderRequest(
                status = patch.status?.let { status -> WCOrderStatusModel(statusKey = status) },
                customerNote = patch.customerNote,
                billingEmail = patch.billingEmail,
                decimalPlaces = null,
            )
        }
        val result = orderStore.batchUpdateOrders(site, requests)
        return if (result.isError) {
            Result.failure(OnChangedException(requireNotNull(result.error)))
        } else {
            val model = requireNotNull(result.model)
            Result.success(
                BulkUpdateResult(
                    updatedIds = model.updatedOrders,
                    failedOrders = model.failedOrders,
                )
            )
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 50
    }
}
