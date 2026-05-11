package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsResponsePayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

internal class AIAnalyticsDataSource @Inject constructor(
    private val selectedSite: SelectedSite,
    // Do not route AI analytics through WCStatsStore: it persists stats in RevenueStatsEntity, while shared
    // date/interval cache readers ignore rangeId/source/currency. AI-only responses could then pollute those reads.
    private val orderStatsRestClient: OrderStatsRestClient,
    private val wooCommerceStore: WooCommerceStore,
) {
    suspend fun fetchRevenueStats(
        after: String,
        before: String,
        interval: AnalyticsInterval,
        currency: String?,
    ): Result<AnalyticsStats> = fetchStats { site ->
        orderStatsRestClient.fetchRevenueStats(
            site = site,
            granularity = interval.statsGranularity,
            startDate = after,
            endDate = before,
            perPage = PER_PAGE,
            forceRefresh = false,
            revenueRangeId = REVENUE_RANGE_ID,
            currency = currency,
        )
    }

    suspend fun fetchOrdersStats(
        after: String,
        before: String,
        interval: AnalyticsInterval,
    ): Result<AnalyticsStats> = fetchStats { site ->
        orderStatsRestClient.fetchOrdersStats(
            site = site,
            granularity = interval.statsGranularity,
            startDate = after,
            endDate = before,
            perPage = PER_PAGE,
            forceRefresh = false,
            orderStatsRangeId = ORDERS_RANGE_ID,
        )
    }

    fun getSelectedSiteCurrencyCode(): String? =
        wooCommerceStore.getSiteSettings(selectedSite.get())
            ?.currencyCode
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private suspend fun fetchStats(
        fetch: suspend (SiteModel) -> FetchRevenueStatsResponsePayload,
    ): Result<AnalyticsStats> = runCatching {
        val site = selectedSite.get()
        val result = fetch(site)
        if (result.isError) {
            throw OnChangedException(requireNotNull(result.error))
        }
        val stats = result.stats ?: error("Stats response missing")
        stats.toAnalyticsStats()
    }

    private fun WCRevenueStatsModel.toAnalyticsStats(): AnalyticsStats {
        val totals = parseJson(total)
        val intervals = (parseJson(data) as? JsonArray)
            ?.mapNotNull { it as? JsonObject }

        return AnalyticsStats(
            totals = totals.takeUnless { it is JsonNull },
            intervals = intervals,
        )
    }

    private fun parseJson(value: String) = Json.parseToJsonElement(value)

    private companion object {
        private const val PER_PAGE = 100
        private const val REVENUE_RANGE_ID = "ai_revenue"
        private const val ORDERS_RANGE_ID = "ai_orders"
    }
}
