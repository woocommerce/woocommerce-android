package org.wordpress.android.fluxc.network.rest.wpcom.wc.settings

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This REST client intentionally lives in fluxc-plugin for now so it can reuse [WooNetwork], including the
 * WooCommerce app's existing connection behavior for direct, Application Passwords, and Jetpack-tunneled sites.
 * Extracting [WooNetwork] or introducing a generic base WPAPI network helper is deferred to a separate PR.
 */
@Singleton
class WPSettingsRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    suspend fun fetchSiteSettings(site: SiteModel): WPAPIResponse<SiteSettingsResponse> {
        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = WPAPI.settings.urlV2,
            clazz = SiteSettingsResponse::class.java,
            params = mapOf(FIELDS_QUERY_PARAM to START_OF_WEEK_FIELD)
        )
    }

    private companion object {
        const val FIELDS_QUERY_PARAM = "_fields"
        const val START_OF_WEEK_FIELD = "start_of_week"
    }
}

data class SiteSettingsResponse(
    @SerializedName("start_of_week") val startOfWeek: Int? = null
)
