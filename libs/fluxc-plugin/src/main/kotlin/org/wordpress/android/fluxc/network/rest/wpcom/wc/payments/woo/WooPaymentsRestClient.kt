package org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class WooPaymentsRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchDepositsOverview(site: SiteModel): WooPayload<WooPaymentsDepositsOverviewApiResponse> {
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = WOOCOMMERCE.payments.deposits.overview_all.pathV3,
            clazz = WooPaymentsDepositsOverviewApiResponse::class.java
        )

        return response.toWooPayload()
    }
}
