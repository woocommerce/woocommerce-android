package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.background.LastUpdateDataStore
import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.store.ListStore
import javax.inject.Inject

class ShouldUpdateOrdersList @Inject constructor(
    private val lastUpdateDataStore: LastUpdateDataStore,
    private val listStore: ListStore
) {
    suspend operator fun invoke(listDescriptor: ListDescriptor): Boolean {
        val listId = listDescriptor.uniqueIdentifier.value
        val shouldUpdateByState = listStore.getListState(listDescriptor) == ListState.NEEDS_REFRESH
        val shouldUpdateByCache = lastUpdateDataStore.getLastUpdateKeyByOrdersListId(listId).let { key ->
            lastUpdateDataStore.shouldUpdateData(key).first()
        }
        return shouldUpdateByState || shouldUpdateByCache
    }
}
