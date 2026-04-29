package com.woocommerce.android.aiassistant.tools.orders

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
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
        val cached = orderStore.getOrderByIdAndSite(orderId, site)
        if (cached != null) {
            return Result.success(cached)
        }
        val result = orderStore.fetchSingleOrderSync(site, orderId)
        return if (result.isError) {
            Result.failure(OnChangedException(result.error))
        } else {
            Result.success(requireNotNull(result.model))
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 50
    }
}
