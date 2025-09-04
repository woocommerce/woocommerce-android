package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
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
) {
    suspend fun loadOrders(): LoadOrdersResult {
        val result = restClient.fetchOrders(
            site = selectedSite.get(),
            count = 25,
            page = 1,
            orderBy = OrderBy.DATE,
            sortOrder = OrderRestClient.SortOrder.DESCENDING,
            statusFilter = null,
            createdVia = "pos-rest-api"
        )

        return if (result.isError) {
            LoadOrdersResult.Error(result.error.message)
        } else {
            LoadOrdersResult.Success(result.orders.toAppModels())
        }
    }

    private suspend fun List<OrderEntity>.toAppModels(): List<Order> = map {
        orderMapper.toAppModel(it)
    }
}
