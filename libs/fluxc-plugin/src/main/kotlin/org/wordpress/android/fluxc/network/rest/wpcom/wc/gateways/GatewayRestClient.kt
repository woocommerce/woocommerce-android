package org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.store.Settings
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class GatewayRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchGateway(
        site: SiteModel,
        gatewayId: String
    ): WooPayload<GatewayResponse> {
        val url = WOOCOMMERCE.payment_gateways.gateway(gatewayId).pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = GatewayResponse::class.java
        )
        return response.toWooPayload()
    }

    suspend fun updatePaymentGateway(
        site: SiteModel,
        gatewayId: GatewayId,
        enabled: Boolean? = null,
        title: String? = null,
        description: String? = null,
        settings: Settings? = null
    ): WooPayload<GatewayResponse> {
        val url = WOOCOMMERCE.payment_gateways.gateway(gatewayId.apiKey).pathV3
        val body = mutableMapOf<String, Any>().apply {
            enabled?.let { put("enabled", enabled) }
            title?.let { put("title", title) }
            description?.let { put("description", description) }
            settings?.let { put("settings", settings) }
        }
        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = GatewayResponse::class.java,
            body = body
        )

        return response.toWooPayload()
    }

    suspend fun fetchAllGateways(
        site: SiteModel
    ): WooPayload<Array<GatewayResponse>> {
        val url = WOOCOMMERCE.payment_gateways.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<GatewayResponse>::class.java
        )
        return response.toWooPayload()
    }

    data class GatewayResponse(
        @SerializedName("id") val gatewayId: String,
        @SerializedName("title") val title: String?,
        @SerializedName("description") val description: String?,
        @SerializedName("order") val order: String?,
        @SerializedName("enabled") val enabled: Boolean?,
        @SerializedName("method_title") val methodTitle: String?,
        @SerializedName("method_description") val methodDescription: String?,
        @SerializedName("method_supports") val features: List<String>?
    )

    enum class GatewayId(val apiKey: String) {
        CASH_ON_DELIVERY("cod")
    }
}
