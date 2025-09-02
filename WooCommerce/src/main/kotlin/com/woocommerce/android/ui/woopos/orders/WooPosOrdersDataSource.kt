package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.OrderBy
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import javax.inject.Inject

private sealed class FetchOrdersResult {
    data class Success(val orders: List<OrderEntity>) : FetchOrdersResult()
    data class Error(val error: WooError) : FetchOrdersResult()
}

class WooPosOrdersDataSource @Inject constructor(
    private val orderStore: OrderRestClient,
    private val selectedSite: SelectedSite,
    private val orderMapper: OrderMapper,
) {
    suspend fun loadOrders(): WooResult<List<Order>> {
        return when (val result = fetchOrdersFromStore(1)) {
            is FetchOrdersResult.Error -> WooResult(result.error)
            is FetchOrdersResult.Success -> WooResult(result.orders.toAppModels())
        }
    }

    private suspend fun fetchOrdersFromStore(
        page: Int
    ): FetchOrdersResult {
        val result = orderStore.fetchOrders(
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
