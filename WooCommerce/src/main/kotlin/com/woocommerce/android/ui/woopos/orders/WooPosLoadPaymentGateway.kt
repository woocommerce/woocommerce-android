package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCGatewayStore
import javax.inject.Inject

class WooPosLoadPaymentGateway @Inject constructor(
    private val gatewayStore: WCGatewayStore,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(order: Order): PaymentGateway {
        val paymentGateway = gatewayStore.getGateway(selectedSite.get(), order.paymentMethod)?.toAppModel()
        return if (paymentGateway != null && paymentGateway.isEnabled) {
            paymentGateway
        } else {
            PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)
        }
    }

    companion object {
        private const val REFUND_METHOD_MANUAL = "manual"
    }
}
