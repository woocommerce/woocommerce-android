package com.woocommerce.android.ui.woopos.emailreceipt

import android.util.Patterns
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class WooPosEmailReceiptRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val orderMapper: OrderMapper,
    private val provideEmailPattern: WooPosProvideEmailPattern,
) {
    suspend fun sendReceiptByEmail(orderId: Long, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val order = getOrderById(orderId)
        if (order == null) {
            return@withContext Result.failure(Exception("Failed to get order"))
        }

        if (updateOrderWithEmail(order, email).isFailure) {
            return@withContext Result.failure(Exception("Failed to update order with email"))
        }

        return@withContext triggerOrderReceiptSending(orderId)
    }

    fun isEmailValid(email: String): Boolean = provideEmailPattern().matcher(email).matches()

    private suspend fun triggerOrderReceiptSending(orderId: Long): Result<Unit> {
        val sendOrderResult = orderStore.sendOrderReceipt(selectedSite.get(), orderId)
        return if (sendOrderResult.isError) {
            Result.failure(Exception("Failed to send order receipt"))
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun updateOrderWithEmail(order: Order, email: String): Result<Order> {
        val updatedBillingAddress = order.billingAddress.copy(email = email)
        val updatedCustomer = order.customer?.copy(billingAddress = updatedBillingAddress)
        return orderCreateEditRepository.createOrUpdateOrder(
            order = order.copy(customer = updatedCustomer)
        )
    }

    private suspend fun getOrderById(orderId: Long) =
        orderStore.getOrderByIdAndSite(orderId, selectedSite.get())?.let {
            orderMapper.toAppModel(it)
        }
}

class WooPosProvideEmailPattern @Inject constructor() {
    operator fun invoke() = Patterns.EMAIL_ADDRESS
}
