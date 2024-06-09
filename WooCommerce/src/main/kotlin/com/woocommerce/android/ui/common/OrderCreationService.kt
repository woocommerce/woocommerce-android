package com.woocommerce.android.ui.common

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class OrderCreationService @Inject constructor(
    private val orderCreateEditRepository: OrderCreateEditRepository
) {

    fun createOrder(order: Order, scope: CoroutineScope, onSuccess: (Order) -> Unit, onFailure: (Throwable) -> Unit) {
        scope.launch {
            orderCreateEditRepository.createOrUpdateOrder(order).fold(
                onSuccess = {
                    onSuccess(it)
                },
                onFailure = {
                    onFailure(it)
                }
            )
        }
    }
}
