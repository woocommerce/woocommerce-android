package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderattribution

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class OrderAttributionRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {
    suspend fun fetchChannelSummary(
        site: SiteModel,
        from: String,
        to: String,
        compareFrom: String,
        compareTo: String
    ): WooPayload<ChannelSummaryResponse> {
        val params = mapOf(
            "from" to from,
            "to" to to,
            "compare_from" to compareFrom,
            "compare_to" to compareTo,
            "interval" to "day"
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = CHANNEL_SUMMARY_PATH,
            clazz = ChannelSummaryResponse::class.java,
            params = params
        )

        return response.toWooPayload()
    }

    companion object {
        private const val CHANNEL_SUMMARY_PATH =
            "/wc/v3/woocommerce-analytics/proxy/reports/order-attribution/channel/summary"
    }
}
