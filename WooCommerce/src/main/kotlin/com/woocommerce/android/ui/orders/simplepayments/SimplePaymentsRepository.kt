package com.woocommerce.android.ui.orders.simplepayments

import com.woocommerce.android.tools.SelectedSite
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

@ViewModelScoped
class SimplePaymentsRepository @Inject constructor(
    private val orderUpdateStore: OrderUpdateStore,
    private val selectedSite: SelectedSite
) {
    suspend fun updateSimplePayment(
        orderId: Long,
        amount: String,
        customerNote: String,
        billingEmail: String,
        isTaxable: Boolean
    ): Flow<WCOrderStore.UpdateOrderResult> {
        return orderUpdateStore.updateSimplePayment(
            selectedSite.get(),
            orderId,
            amount,
            customerNote,
            billingEmail,
            isTaxable
        )
    }
}
