package com.woocommerce.android.ui.orders.details.editing

import com.woocommerce.android.tools.SelectedSite
import dagger.hilt.android.scopes.ViewModelScoped
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

@ViewModelScoped
class OrderEditingRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) {
    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun updateCustomerOrderNote(
        orderId: Long,
        customerOrderNote: String
    ) {
        // TODO
    }
}
