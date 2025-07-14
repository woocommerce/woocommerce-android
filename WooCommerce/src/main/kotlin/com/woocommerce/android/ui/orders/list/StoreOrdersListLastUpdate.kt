package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.background.LastUpdateDataStore
import javax.inject.Inject

class StoreOrdersListLastUpdate @Inject constructor(private val lastUpdateDataStore: LastUpdateDataStore) {
    suspend operator fun invoke(listId: Int) {
        lastUpdateDataStore.getLastUpdateKeyByOrdersListId(listId).let { key ->
            lastUpdateDataStore.storeLastUpdate(key)
        }
    }
}
