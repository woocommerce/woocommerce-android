package com.woocommerce.android.ui.orders.details.editing

import com.woocommerce.android.model.OrderId
import com.woocommerce.android.model.toFluxcRemoteId
import com.woocommerce.android.tools.SelectedSite
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

@ViewModelScoped
@Suppress("UnusedPrivateMember")
class OrderEditingRepository @Inject constructor(
    private val orderUpdateStore: OrderUpdateStore,
    private val selectedSite: SelectedSite
) {
    suspend fun updateCustomerOrderNote(
        remoteOrderId: OrderId,
        customerOrderNote: String
    ): Flow<WCOrderStore.UpdateOrderResult> {
        return orderUpdateStore.updateCustomerOrderNote(
            remoteOrderId.toFluxcRemoteId(),
            selectedSite.get(),
            customerOrderNote
        )
    }

    suspend fun updateOrderAddress(
        remoteOrderId: OrderId,
        orderAddress: OrderAddress
    ): Flow<WCOrderStore.UpdateOrderResult> {
        return orderUpdateStore.updateOrderAddress(
            remoteOrderId.toFluxcRemoteId(),
            selectedSite.get().localId(),
            orderAddress
        )
    }

    suspend fun updateBothOrderAddresses(
        remoteOrderId: OrderId,
        shippingAddress: OrderAddress.Shipping,
        billingAddress: OrderAddress.Billing
    ): Flow<WCOrderStore.UpdateOrderResult> {
        return orderUpdateStore.updateBothOrderAddresses(
            remoteOrderId.toFluxcRemoteId(),
            selectedSite.get().localId(),
            shippingAddress,
            billingAddress
        )
    }
}
