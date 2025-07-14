package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import javax.inject.Inject

class MarkOrderAsComplete @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository
) {
    suspend operator fun invoke(orderId: Long): Result<Unit> {
        val result = orderDetailRepository.updateOrderStatus(
            orderId = orderId,
            newStatus = CoreOrderStatus.COMPLETED.value
        )
            .filter { it is UpdateOrderResult.RemoteUpdateResult }
            .first()

        return when {
            result.event.isError -> Result.failure(OnChangedException(result.event.error))
            else -> Result.success(Unit)
        }
    }
}
