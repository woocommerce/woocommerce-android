package com.woocommerce.android.ui.woopos.home.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class WooPosOrdersRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val orderMapper: OrderMapper,
    private val selectedSite: SelectedSite,
) {
    fun fetchPosOrders(): Flow<Result<List<Order>>> = flow {
        val cachedOrders = orderStore.getOrdersForSite(selectedSite.get())
            .filter { it.status != "auto-draft" }
            .sortedByDescending { it.dateCreated }
            .take(20)
            .map { orderMapper.toAppModel(it) }
        
        if (cachedOrders.isNotEmpty()) {
            emit(Result.success(cachedOrders))
        }
        
        val result = orderStore.fetchOrders(
            site = selectedSite.get(),
            count = 20,
            deleteOldData = false
        )
        
        if (result.isError) {
            emit(Result.failure(Exception(result.error.message)))
        } else {
            val allOrders = orderStore.getOrdersForSite(selectedSite.get())
                .filter { it.status != "auto-draft" }
                .sortedByDescending { it.dateCreated }
                .take(20)
                .map { orderMapper.toAppModel(it) }
            
            emit(Result.success(allOrders))
        }
    }
}