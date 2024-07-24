package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.background.LastUpdateDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ShouldUpdateOrdersList @Inject constructor(private val lastUpdateDataStore: LastUpdateDataStore) {
    suspend operator fun invoke(listId: Int): Boolean {
        return lastUpdateDataStore.getLastUpdateKeyByOrdersListId(listId).let { key ->
            lastUpdateDataStore.shouldUpdateData(key).first()
        }
    }
}
