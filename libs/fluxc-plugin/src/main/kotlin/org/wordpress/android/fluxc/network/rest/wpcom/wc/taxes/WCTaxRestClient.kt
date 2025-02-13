package org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCTaxRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchTaxClassList(
        site: SiteModel
    ): WooPayload<Array<TaxClassApiResponse>> {
        val url = WOOCOMMERCE.taxes.classes.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site,
            url,
            Array<TaxClassApiResponse>::class.java
        )
        return response.toWooPayload()
    }

    suspend fun fetchTaxRateList(
        site: SiteModel,
        page: Int,
        pageSize: Int,
    ): WooPayload<Array<TaxRateDto>> {
        val url = WOOCOMMERCE.taxes.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site,
            url,
            Array<TaxRateDto>::class.java,
            mutableMapOf<String, String>().apply {
                put("page", page.toString())
                put("per_page", pageSize.toString())
            }
        )
        return when (response) {
            is WPAPIResponse.Success -> {
                WooPayload(response.data)
            }
            is WPAPIResponse.Error -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    data class TaxClassApiResponse(
        val name: String? = null,
        val slug: String? = null
    )
}
