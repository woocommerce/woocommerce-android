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

class WooPosOrdersDataSource @Inject constructor(
    private val orderStore: OrderRestClient,
    private val selectedSite: SelectedSite,
    private val orderMapper: OrderMapper,
) {
    suspend fun loadOrders(): WooResult<List<Order>> {
        val result = fetchOrdersFromStore(1)

        return if (result.isError) {
            WooResult(result.error)
        } else {
            WooResult(result.model.toAppModels())
        }
    }

    private suspend fun fetchOrdersFromStore(
        page: Int
    ): WooResult<List<OrderEntity>> {
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
            WooResult(WooError(API_ERROR, SERVER_ERROR, result.error.message))
        } else {
            WooResult(result.orders)
        }
    }

    private suspend fun List<OrderEntity>?.toAppModels(): List<Order> = this?.map {
        orderMapper.toAppModel(it)
    } ?: emptyList()
}
