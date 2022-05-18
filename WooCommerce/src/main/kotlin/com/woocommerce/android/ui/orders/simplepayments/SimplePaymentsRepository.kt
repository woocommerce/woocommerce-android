package com.woocommerce.android.ui.orders.simplepayments

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreationRepository
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@ViewModelScoped
class SimplePaymentsRepository @Inject constructor(
    private val orderUpdateStore: OrderUpdateStore,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val dispatchers: CoroutineDispatchers
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

    private suspend fun isAutoDraftSupported(): Boolean {
        val version = withContext(dispatchers.io) {
            wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_CORE)?.version
                ?: "0.0"
        }
        return version.semverCompareTo(OrderCreationRepository.AUTO_DRAFT_SUPPORTED_VERSION) >= 0
    }
}
