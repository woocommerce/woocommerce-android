package org.wordpress.android.fluxc.network.rest.wpcom.wc.bundlestats

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class BundleStatsRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    suspend fun fetchBundleStats(
        site: SiteModel,
        startDate: String,
        endDate: String,
        interval: String = "",
    ): WooPayload<BundleStatsApiResponse> {
        val url = WOOCOMMERCE.reports.bundles.stats.pathV4Analytics
        val parameters = mapOf(
            "before" to endDate,
            "after" to startDate,
            "interval" to interval
        ).filter { it.value.isNotEmpty() }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = BundleStatsApiResponse::class.java,
            params = parameters
        )
        return response.toWooPayload()
    }

    suspend fun fetchBundleReport(
        site: SiteModel,
        startDate: String,
        endDate: String,
        quantity: Int = 5
    ): WooPayload<Array<BundlesReportApiResponse>> {
        val url = WOOCOMMERCE.reports.bundles.pathV4Analytics
        val parameters = mapOf(
            "before" to endDate,
            "after" to startDate,
            "orderby" to "items_sold",
            "order" to "desc",
            "page" to "1",
            "per_page" to quantity.toString(),
            "extended_info" to "true"
        ).filter { it.value.isNotEmpty() }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<BundlesReportApiResponse>::class.java,
            params = parameters
        )
        return response.toWooPayload()
    }
}
