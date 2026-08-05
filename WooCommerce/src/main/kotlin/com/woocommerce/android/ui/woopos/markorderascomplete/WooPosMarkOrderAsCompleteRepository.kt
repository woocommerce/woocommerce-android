package com.woocommerce.android.ui.woopos.markorderascomplete

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.notifications.push.NewOrderNotificationSuppressionCache
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class WooPosMarkOrderAsCompleteRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val orderMapper: OrderMapper,
    private val newOrderNotificationSuppressionCache: NewOrderNotificationSuppressionCache,
) {
    suspend fun getOrderById(orderId: Long): Order? = withContext(Dispatchers.IO) {
        orderStore.getOrderByIdAndSite(orderId, selectedSite.get())?.let { orderMapper.toAppModel(it) }
    }

    suspend fun markOrderAsComplete(
        orderId: Long,
        customerNote: String?,
    ): MarkOrderAsCompleteOutcome = withContext(Dispatchers.IO) {
        val statusModel = orderStore.getOrderStatusForSiteAndKey(
            selectedSite.get(),
            Order.Status.Completed.value,
        ) ?: WCOrderStatusModel(
            statusKey = Order.Status.Completed.value,
            label = Order.Status.Completed.value,
        )

        val previousStatusKey = orderStore.getOrderByIdAndSite(orderId, selectedSite.get())?.status

        val updateResult = orderStore.updateOrderStatusAndPaymentDetails(
            orderId = orderId,
            site = selectedSite.get(),
            newStatus = statusModel,
            newPaymentMethodId = MANUAL_PAYMENT_METHOD_ID,
            newPaymentMethodTitle = MANUAL_PAYMENT_METHOD_TITLE,
        )
            .filterIsInstance<WCOrderStore.UpdateOrderResult.RemoteUpdateResult>()
            .first()

        if (updateResult.event.isError) {
            WooLog.e(T.POS, "Mark order as complete failed - ${updateResult.event.error.message}")
            return@withContext MarkOrderAsCompleteOutcome.Failure
        }

        newOrderNotificationSuppressionCache.recordOrderStatusChanged(
            siteId = selectedSite.get().siteId,
            orderId = orderId,
            previousStatusKey = previousStatusKey,
            newStatusKey = Order.Status.Completed.value,
        )

        val trimmedNote = customerNote?.takeIf { it.isNotBlank() }
            ?: return@withContext MarkOrderAsCompleteOutcome.Success

        val noteResult = orderStore.postOrderNote(
            site = selectedSite.get(),
            orderId = orderId,
            note = trimmedNote,
            isCustomerNote = false,
        )
        if (noteResult.isError) {
            WooLog.e(T.POS, "Mark order as complete note post failed - ${noteResult.error?.message}")
            MarkOrderAsCompleteOutcome.SuccessWithFailedNote
        } else {
            MarkOrderAsCompleteOutcome.Success
        }
    }

    private companion object {
        const val MANUAL_PAYMENT_METHOD_ID = "other"
        const val MANUAL_PAYMENT_METHOD_TITLE = "Other"
    }
}

sealed class MarkOrderAsCompleteOutcome {
    data object Success : MarkOrderAsCompleteOutcome()
    data object SuccessWithFailedNote : MarkOrderAsCompleteOutcome()
    data object Failure : MarkOrderAsCompleteOutcome()
}
