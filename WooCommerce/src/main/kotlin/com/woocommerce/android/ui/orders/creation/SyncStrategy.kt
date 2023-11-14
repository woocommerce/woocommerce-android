package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import kotlinx.coroutines.flow.Flow

interface SyncStrategy {
    fun syncOrderChanges(changes: Flow<Order>, retryTrigger: Flow<Unit>): Flow<CreateUpdateOrder.OrderUpdateStatus>
}
