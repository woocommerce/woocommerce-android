package com.woocommerce.android.network.environment

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

/**
 * Client that fetches the store environment from the system status API.
 */
class EnvironmentRestClient @Inject constructor(private val wooNetwork: WooNetwork) {

    /**
     * Fetches the store environment. Currently, Only used to get the `store_id`
     */
    suspend fun fetchStoreEnvironment(site: SiteModel): WooPayload<EnvironmentDto> {
        val url = WOOCOMMERCE.system_status.pathV3
        val params = mapOf("_fields" to "environment")

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = EnvironmentResponse::class.java,
            params = params
        ).toWooPayload { environmentResponse ->
            environmentResponse.environment
        }
    }

    data class EnvironmentResponse(
        @SerializedName("environment") val environment: EnvironmentDto
    )

    data class EnvironmentDto(
        @SerializedName("store_id") var storeID: String? = null
    )
}
