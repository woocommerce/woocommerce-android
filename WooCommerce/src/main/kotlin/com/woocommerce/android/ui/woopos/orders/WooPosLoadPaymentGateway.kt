package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCGatewayStore
import javax.inject.Inject

class WooPosLoadPaymentGateway @Inject constructor(
    private val gatewayStore: WCGatewayStore,
    private val selectedSite: SelectedSite,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(order: Order): Result<PaymentGateway> = withContext(coroutineDispatchers.io) {
        val site = selectedSite.get()

        var gateway = gatewayStore.getGateway(site, order.paymentMethod)?.toAppModel()

        if (gateway == null) {
            val fetchResult = gatewayStore.fetchAllGateways(site)
            if (fetchResult.isError) {
                return@withContext Result.failure(
                    Exception("Failed to fetch payment gateways: ${fetchResult.error.message}")
                )
            }

            gateway = gatewayStore.getGateway(site, order.paymentMethod)?.toAppModel()
        }

        return@withContext if (gateway != null) {
            Result.success(gateway)
        } else {
            Result.failure(Exception("Payment gateway '${order.paymentMethod}' not found"))
        }
    }
}
