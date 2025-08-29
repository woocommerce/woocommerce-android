package org.wordpress.android.fluxc.network.rest.wpcom.wc.system

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class WooSystemRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    companion object {
        private const val NOT_FOUND = 404
        private const val FORBIDDEN = 403
        private const val SAVE_SITE_TITLE_RESPONSE_FIELD = "title"
    }

    suspend fun fetchInstalledPlugins(site: SiteModel): WooPayload<WCSystemPluginResponse> {
        val url = WOOCOMMERCE.system_status.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = mapOf("_fields" to "active_plugins,inactive_plugins"),
            clazz = WCSystemPluginResponse::class.java
        )

        return response.toWooPayload()
    }

    suspend fun fetchSSR(site: SiteModel): WooPayload<SSRResponse> {
        val url = WOOCOMMERCE.system_status.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = mapOf("_fields" to "environment,database,active_plugins,theme,settings,security,pages"),
            clazz = SSRResponse::class.java
        )
        return response.toWooPayload()
    }

    suspend fun saveSiteTitle(site: SiteModel, siteTitle: String): WooPayload<SaveSiteTitleResponse> {
        val url = WPAPI.settings.urlV2

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = SaveSiteTitleResponse::class.java,
            body = mapOf(
                "title" to siteTitle,
                "_fields" to SAVE_SITE_TITLE_RESPONSE_FIELD
            )
        )

        return response.toWooPayload()
    }

    suspend fun fetchPosSettings(site: SiteModel): WooPayload<List<PosSettingResponse>> {
        val url = WOOCOMMERCE.settings.point_of_sale.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<PosSettingResponse>::class.java
        )

        return response.toWooPayload { it.toList() }
    }

    data class SaveSiteTitleResponse(
        @SerializedName("title") val title: String? = null
    )

    data class PosSettingResponse(
        @SerializedName("id") val id: String,
        @SerializedName("label") val label: String,
        @SerializedName("description") val description: String? = null,
        @SerializedName("type") val type: String,
        @SerializedName("default") val default: String? = null,
        @SerializedName("value") val value: String? = null
    )

    data class SSRResponse(
        val environment: JsonElement? = null,
        val database: JsonElement? = null,
        @SerializedName("active_plugins") val activePlugins: JsonElement? = null,
        val theme: JsonElement? = null,
        val settings: JsonElement? = null,
        val security: JsonElement? = null,
        val pages: JsonElement? = null
    )
}
