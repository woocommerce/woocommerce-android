package com.woocommerce.android.ui.woopos.cardpayment

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class WooPosCardPaymentRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val orderMapper: OrderMapper,
) {
    suspend fun fetchOrGetOrder(orderId: Long): Order? = withContext(Dispatchers.IO) {
        getOrderById(orderId) ?: run {
            val result = orderStore.fetchSingleOrder(selectedSite.get(), orderId)
            if (result.isError) return@withContext null
            getOrderById(orderId)
        }
    }

    private suspend fun getOrderById(orderId: Long): Order? {
        return orderStore.getOrderByIdAndSite(orderId, selectedSite.get())?.let {
            orderMapper.toAppModel(it)
        }
    }
}
