package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardsRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    @Suppress("LongParameterList")
    suspend fun fetchLeaderboards(
        site: SiteModel,
        startDate: String,
        endDate: String,
        quantity: Int?,
        forceRefresh: Boolean,
        interval: String = "",
        addProductsPath: Boolean = false,
    ): WooPayload<Array<LeaderboardsApiResponse>> {
        val url = when (addProductsPath) {
            true -> WOOCOMMERCE.leaderboards.products.pathV4Analytics
            else -> WOOCOMMERCE.leaderboards.pathV4Analytics
        }

        val parameters = createParameters(startDate, endDate, quantity, forceRefresh, interval)

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<LeaderboardsApiResponse>::class.java,
            params = parameters
        )

        return response.toWooPayload()
    }

    @Suppress("LongParameterList")
    private fun createParameters(
        startDate: String,
        endDate: String,
        quantity: Int?,
        forceRefresh: Boolean,
        interval: String = ""
    ) = mapOf(
        "before" to endDate,
        "after" to startDate,
        "per_page" to quantity?.toString().orEmpty(),
        "interval" to interval,
        "force_cache_refresh" to forceRefresh.toString()
    ).filter { it.value.isNotEmpty() }
}
