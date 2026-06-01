package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.WCAnalyticsOrderDateType
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooCommerceRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    companion object {
        const val TAX_SETTING_GROUP = "tax"
        const val TAX_SETTING_ID = "woocommerce_tax_based_on"
        const val ROUND_TAX_AT_SUBTOTAL_SETTING_ID = "woocommerce_tax_round_at_subtotal"
        const val ADVANCED_SETTING_GROUP = "advanced"
        const val ANALYTICS_ENABLED_SETTING_ID = "woocommerce_analytics_enabled"
        const val ANALYTICS_SETTING_GROUP = "wc_admin"
        const val ANALYTICS_DATE_TYPE_SETTING_ID = "woocommerce_date_type"

        private const val ROOT_ENDPOINT_TIMEOUT_MS = 15000
    }

    /**
     * Makes a GET call to the root wp-json endpoint (`/`) via the Jetpack tunnel (see [JetpackTunnelGsonRequest])
     * for the given [SiteModel]
     *
     */
    suspend fun fetchSiteRootAPIEndpoint(
        site: SiteModel,
        overrideRetryPolicy: Boolean = false
    ): WooPayload<RootWPAPIRestResponse> {
        val url = "/"
        val timeout = when (overrideRetryPolicy) {
            true -> ROOT_ENDPOINT_TIMEOUT_MS
            false -> BaseRequest.DEFAULT_REQUEST_TIMEOUT
        }
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = mapOf("_fields" to "authentication,namespaces"),
            clazz = RootWPAPIRestResponse::class.java,
            requestTimeout = timeout
        )
        return response.toWooPayload()
    }

    suspend fun fetchSiteRootAPIRoutes(site: SiteModel): WooPayload<RootWPAPIRestResponse> {
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = "/",
            params = mapOf("_fields" to "routes"),
            clazz = RootWPAPIRestResponse::class.java
        )
        return response.toWooPayload()
    }

    /**
     * Makes a GET call to `/wc/v3/settings/general` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving site settings for the given WooCommerce [SiteModel].
     *
     */
    suspend fun fetchSiteSettingsGeneral(site: SiteModel): WooPayload<List<SiteSettingsResponse>> {
        val url = WOOCOMMERCE.settings.general.pathV3
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<SiteSettingsResponse>::class.java
        )
        return response.toWooPayload { it.toList() }
    }

    suspend fun fetchSiteSettingsProducts(site: SiteModel): WooPayload<List<SiteSettingsResponse>> {
        val url = WOOCOMMERCE.settings.products.pathV3
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<SiteSettingsResponse>::class.java
        )
        return response.toWooPayload { it.toList() }
    }

    suspend fun enableAnalytics(site: SiteModel): WooPayload<Boolean> {
        val url = WOOCOMMERCE.settings.group(ADVANCED_SETTING_GROUP).id(ANALYTICS_ENABLED_SETTING_ID).pathV3
        val param = mapOf("value" to "yes")

        val response = wooNetwork.executePutGsonRequest(
            site = site,
            path = url,
            clazz = SiteSettingOptionResponse::class.java,
            body = param
        )

        return response.toWooPayload { it.value == "yes" }
    }

    suspend fun fetchAnalyticsEnabled(site: SiteModel): WooPayload<Boolean> {
        val url = WOOCOMMERCE.settings.group(ADVANCED_SETTING_GROUP).id(ANALYTICS_ENABLED_SETTING_ID).pathV3
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = SiteSettingOptionResponse::class.java
        )
        return response.toWooPayload { it.value == "yes" }
    }

    suspend fun fetchSiteSettingsTaxBasedOn(site: SiteModel): WooPayload<SiteSettingOptionResponse> {
        val url = WOOCOMMERCE.settings.group(TAX_SETTING_GROUP).id(TAX_SETTING_ID).pathV3
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = SiteSettingOptionResponse::class.java
        )
        return response.toWooPayload { it }
    }

    suspend fun fetchSiteSettingsTaxRoundTaxAtSubtotal(site: SiteModel): WooPayload<Boolean> {
        val url = WOOCOMMERCE.settings.group(TAX_SETTING_GROUP).id(ROUND_TAX_AT_SUBTOTAL_SETTING_ID).pathV3
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = SiteSettingOptionResponse::class.java,
        )
        return response.toWooPayload { it.value == "yes" }
    }

    suspend fun fetchAnalyticsOrderDateType(site: SiteModel): WooPayload<WCAnalyticsOrderDateType> {
        val url = WOOCOMMERCE.settings.group(ANALYTICS_SETTING_GROUP).id(ANALYTICS_DATE_TYPE_SETTING_ID).pathV3
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = SiteSettingOptionResponse::class.java,
        )
        return response.toWooPayload { WCAnalyticsOrderDateType.fromValue(it.value) }
    }

    suspend fun updateAnalyticsOrderDateType(
        site: SiteModel,
        orderDateType: WCAnalyticsOrderDateType
    ): WooPayload<WCAnalyticsOrderDateType> {
        val url = WOOCOMMERCE.settings.group(ANALYTICS_SETTING_GROUP).id(ANALYTICS_DATE_TYPE_SETTING_ID).pathV3
        val param = mapOf("value" to orderDateType.value)

        val response = wooNetwork.executePutGsonRequest(
            site = site,
            path = url,
            clazz = SiteSettingOptionResponse::class.java,
            body = param
        )

        return response.toWooPayload { WCAnalyticsOrderDateType.fromValue(it.value) }
    }
}
