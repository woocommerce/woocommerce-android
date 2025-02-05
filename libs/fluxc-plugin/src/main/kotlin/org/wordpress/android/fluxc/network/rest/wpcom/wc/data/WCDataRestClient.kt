package org.wordpress.android.fluxc.network.rest.wpcom.wc.data

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class WCDataRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchCountries(
        site: SiteModel
    ): WooPayload<Array<CountryApiResponse>> {
        val url = WOOCOMMERCE.data.countries.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<CountryApiResponse>::class.java
        )
        return response.toWooPayload()
    }

    data class CountryApiResponse(
        val code: String? = null,
        val name: String? = null,
        val states: List<State>
    ) {
        data class State(
            val code: String? = null,
            val name: String? = null
        )
    }
}
