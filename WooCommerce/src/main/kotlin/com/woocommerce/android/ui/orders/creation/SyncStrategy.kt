package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
interface SyncStrategy {
    fun syncOrderChanges(changes: Flow<Order>, retryTrigger: Flow<Unit>): Flow<CreateUpdateOrder.OrderUpdateStatus>
}
