package com.woocommerce.android.ui.woopos.home.orders

import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import javax.inject.Inject

class WooPosOrdersRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val refundStore: WCRefundStore,
    private val orderMapper: OrderMapper,
    private val selectedSite: SelectedSite,
) {
    fun fetchPosOrders(): Flow<Result<List<Order>>> = flow {
        val cachedOrders = orderStore.getOrdersForSite(selectedSite.get())
            .filter { it.status != "auto-draft" }
            .sortedByDescending { it.dateCreated }
            .take(ORDERS_LIMIT)
            .map { orderMapper.toAppModel(it) }

        if (cachedOrders.isNotEmpty()) {
            emit(Result.success(cachedOrders))
        }

        val result = orderStore.fetchOrders(
            site = selectedSite.get(),
            count = ORDERS_LIMIT,
            deleteOldData = false
        )

        if (result.isError) {
            emit(Result.failure(Exception(result.error.message)))
        } else {
            val allOrders = orderStore.getOrdersForSite(selectedSite.get())
                .filter { it.status != "auto-draft" }
                .sortedByDescending { it.dateCreated }
                .take(ORDERS_LIMIT)
                .map { orderMapper.toAppModel(it) }

            emit(Result.success(allOrders))
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun processRefund(
        orderId: Long,
        amount: BigDecimal,
        reason: String,
        method: RefundMethod
    ): Result<Refund> {
        return try {
            val site = selectedSite.get()
            val autoRefund = method == RefundMethod.CARD

            val result = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
                refundStore.createAmountRefund(
                    site = site,
                    orderId = orderId,
                    amount = amount,
                    reason = reason,
                    autoRefund = autoRefund
                )
            }

            when {
                result == null -> Result.failure(
                    Exception("Request timeout while processing refund")
                )
                result.isError -> Result.failure(
                    Exception(result.error.message)
                )
                else -> Result.success(result.model!!.toAppModel())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun fetchOrderRefunds(orderId: Long): Result<List<Refund>> {
        return try {
            val site = selectedSite.get()
            val refunds = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
                refundStore.getAllRefunds(site, orderId)
            }

            if (refunds == null) {
                Result.failure(Exception("Request timeout while fetching refunds"))
            } else {
                val appRefunds = refunds.map { it.toAppModel() }
                Result.success(appRefunds)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val ORDERS_LIMIT = 20
    }
}
