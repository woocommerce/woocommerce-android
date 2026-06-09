package com.woocommerce.android.ui.woopos.scantopay

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class WooPosScanToPayRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val orderMapper: OrderMapper,
) {
    suspend fun promoteOrderToPending(orderId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val statusModel = orderStore.getOrderStatusForSiteAndKey(
            selectedSite.get(),
            Order.Status.Pending.value,
        ) ?: WCOrderStatusModel(
            statusKey = Order.Status.Pending.value,
            label = Order.Status.Pending.value,
        )

        orderStore.updateOrderStatus(
            orderId = orderId,
            site = selectedSite.get(),
            newStatus = statusModel,
        )
            .filterIsInstance<WCOrderStore.UpdateOrderResult.RemoteUpdateResult>()
            .map { result ->
                if (result.event.isError) {
                    WooLog.e(T.POS, "Scan to Pay: promote to pending failed - ${result.event.error.message}")
                    Result.failure(Exception(result.event.error.message))
                } else {
                    Result.success(Unit)
                }
            }
            .first()
    }

    suspend fun fetchOrderSnapshot(orderId: Long): Order? = withContext(Dispatchers.IO) {
        val result = orderStore.fetchSingleOrderSync(selectedSite.get(), orderId)
        if (result.isError) {
            WooLog.e(T.POS, "Scan to Pay: fetch failed - ${result.error?.message}")
            null
        } else {
            result.model?.let { orderMapper.toAppModel(it) }
        }
    }

    suspend fun getCachedOrder(orderId: Long): Order? = withContext(Dispatchers.IO) {
        orderStore.getOrderByIdAndSite(orderId, selectedSite.get())?.let { orderMapper.toAppModel(it) }
    }

    suspend fun addOrderNote(orderId: Long, note: String): Result<Unit> = withContext(Dispatchers.IO) {
        val noteResult = orderStore.postOrderNote(
            site = selectedSite.get(),
            orderId = orderId,
            note = note,
            isCustomerNote = false,
        )
        if (noteResult.isError) {
            WooLog.e(T.POS, "Scan to Pay: post note failed - ${noteResult.error?.message}")
            Result.failure(Exception(noteResult.error?.message ?: "post order note failed"))
        } else {
            Result.success(Unit)
        }
    }
}
