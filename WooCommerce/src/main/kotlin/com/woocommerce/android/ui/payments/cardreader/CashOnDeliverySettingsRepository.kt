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
        return if (shouldEnable) {
            gatewayStore.updateGateway(
                site = selectedSite.get(),
                gatewayId = GatewayRestClient.GatewayId.CASH_ON_DELIVERY,
                enabled = true,
                title = PAY_IN_PERSON_TITLE,
                description = PAY_IN_PERSON_DESCRIPTION,
                settings = Settings(
                    instructions = PAY_IN_PERSON_DESCRIPTION
                )
            )
        } else {
            gatewayStore.updateGateway(
                site = selectedSite.get(),
                gatewayId = GatewayRestClient.GatewayId.CASH_ON_DELIVERY,
                enabled = false
            )
        }
    }

    suspend fun isCashOnDeliveryEnabled(): Boolean = fetchCashOnDeliveryGateway().model?.isEnabled == true

    /**
     * A successful result with a `null` model means the store has no Cash on Delivery gateway.
     */
    suspend fun fetchCashOnDeliveryGateway(): WooResult<WCGatewayModel> {
        val result = gatewayStore.fetchAllGateways(selectedSite.get())
        if (result.isError) return WooResult(result.error)
        return WooResult(
            result.model?.firstOrNull { wcGatewayModel ->
                wcGatewayModel.id.equals(CASH_ON_DELIVERY_GATEWAY_ID, ignoreCase = true)
            }
        )
    }

    companion object {
        const val PAY_IN_PERSON_TITLE = "Pay in Person"
        private const val PAY_IN_PERSON_DESCRIPTION = "Pay by card or another accepted payment method"
        private const val CASH_ON_DELIVERY_GATEWAY_ID = "cod"
    }
}
