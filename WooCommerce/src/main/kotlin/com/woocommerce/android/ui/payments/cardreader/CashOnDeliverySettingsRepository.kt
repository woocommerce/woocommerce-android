package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.tools.SelectedSite
import dagger.Reusable
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.store.Settings
import org.wordpress.android.fluxc.store.WCGatewayStore
import javax.inject.Inject

@Reusable
class CashOnDeliverySettingsRepository @Inject constructor(
    private val gatewayStore: WCGatewayStore,
    private val selectedSite: SelectedSite,
) {
    suspend fun toggleCashOnDeliveryOption(shouldEnable: Boolean): WooResult<WCGatewayModel> {
        return gatewayStore.updatePaymentGateway(
            site = selectedSite.get(),
            gatewayId = GatewayRestClient.GatewayId.CASH_ON_DELIVERY,
            enabled = shouldEnable,
            title = "Pay in Person",
            description = "Pay by card or another accepted payment method",
            settings = Settings(
                instructions = "Pay by card or another accepted payment method"
            )
        )
    }

    suspend fun isCashOnDeliveryEnabled(): Boolean {
        val gateways = gatewayStore.fetchAllGateways(selectedSite.get()).model
        return gateways?.firstOrNull { wcGatewayModel ->
            wcGatewayModel.id.equals("cod", ignoreCase = true)
        }?.isEnabled ?: false
    }
}
