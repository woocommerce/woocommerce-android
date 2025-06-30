package org.wordpress.android.fluxc.network.rest.wpcom.wc.system

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
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

    /**
     * Test the settings endpoint of WooCommerce to confirm if the plugin is available or not.
     * We pass an empty _fields just to reduce the response payload size, as we don't care about the contents
     *
     * @return JetpackSuccess(true) if WooCommerce is installed, JetpackSuccess(false) is we get a 404,
     *         and JetpackError otherwise
     */
    suspend fun checkIfWooCommerceIsAvailable(site: SiteModel): WooPayload<Boolean> {
        val url = WOOCOMMERCE.settings.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = mapOf("_fields" to ""),
            clazz = Any::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> WooPayload(true)
            is WPAPIResponse.Error -> {
                when (response.error.volleyError?.networkResponse?.statusCode) {
                    NOT_FOUND -> WooPayload(false)
                    FORBIDDEN -> {
                        // If we get a 403 status code, we can infer that Woo is installed,
                        // the user just doesn't have permission to access the Woo API
                        WooPayload(true)
                    }
                    else -> WooPayload(response.error.toWooError())
                }
            }
        }
    }

    suspend fun fetchSiteSettings(site: SiteModel): WooPayload<WPSiteSettingsResponse> {
        val url = WPAPI.settings.urlV2

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = WPSiteSettingsResponse::class.java
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

    data class SaveSiteTitleResponse(
        @SerializedName("title") val title: String? = null
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

    data class WPSiteSettingsResponse(
        @SerializedName("title") val title: String? = null,
        @SerializedName("description") val description: String? = null,
        @SerializedName("url") val url: String? = null,
        @SerializedName("show_on_front") val showOnFront: String? = null,
        @SerializedName("page_on_front") val pageOnFront: Long? = null
    )
}
