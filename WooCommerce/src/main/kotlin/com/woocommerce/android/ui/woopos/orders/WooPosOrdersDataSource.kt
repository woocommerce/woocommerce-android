package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class WooPosOrdersDataSource @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val orderMapper: OrderMapper,
)
{
    suspend fun loadOrders(): WooResult<List<Order>> {
        val result = fetchOrdersFromStore(1, 1)

        return if (result.isError) {
            WooResult(result.error)
        } else {
            WooResult(result.model.toAppModels())
        }
    }

    private suspend fun fetchOrdersFromStore(
        offset: Int,
        pageSize: Int
    ): WooResult<List<OrderEntity>> {
        return orderStore.fetchOrders(
            site = selectedSite.get(),
            page = 1,
            createdVia = "pos-rest-api"
        )
    }

    suspend fun List<OrderEntity>?.toAppModels(): List<Order> = this?.map { orderMapper.toAppModel(it) } ?: emptyList()
}
