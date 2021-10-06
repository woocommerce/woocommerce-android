package com.woocommerce.android.ui.orders.details.editing

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import dagger.hilt.android.scopes.ViewModelScoped
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

@ViewModelScoped
class OrderEditingRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) {
    fun getOrder(orderIdentifier: OrderIdentifier) = orderStore.getOrderByIdentifier(orderIdentifier)!!.toAppModel()

    suspend fun updateCustomerOrderNote(
        order: Order,
        customerOrderNote: String
    ) {
        // TODO push changes once FluxC work is ready
    }
}
