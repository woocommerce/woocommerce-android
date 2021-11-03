package com.woocommerce.android.ui.orders.details.editing

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

@ViewModelScoped
@Suppress("UnusedPrivateMember")
class OrderEditingRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val orderUpdateStore: OrderUpdateStore,
    private val selectedSite: SelectedSite
) {
    fun getOrder(orderIdentifier: OrderIdentifier) = orderStore.getOrderByIdentifier(orderIdentifier)!!.toAppModel()

    suspend fun updateCustomerOrderNote(
        remoteOrderId: LocalOrRemoteId.RemoteId,
        customerOrderNote: String
    ): Flow<WCOrderStore.UpdateOrderResult> {
        return orderUpdateStore.updateCustomerOrderNote(remoteOrderId, selectedSite.get(), customerOrderNote)
    }

    suspend fun updateOrderAddress(
        remoteOrderId: LocalOrRemoteId.RemoteId,
        orderAddress: OrderAddress
    ): Flow<WCOrderStore.UpdateOrderResult> {
        return orderUpdateStore.updateOrderAddress(remoteOrderId, selectedSite.get().localId(), orderAddress)
    }

    suspend fun updateBothOrderAddresses(
        remoteOrderId: LocalOrRemoteId.RemoteId,
        shippingAddress: OrderAddress.Shipping,
        billingAddress: OrderAddress.Billing
    ): Flow<WCOrderStore.UpdateOrderResult> {
        return orderUpdateStore.updateBothOrderAddresses(
            remoteOrderId,
            selectedSite.get().localId(),
            shippingAddress,
            billingAddress
        )
    }
}
