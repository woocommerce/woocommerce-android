package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrdersStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import java.util.UUID
import javax.inject.Inject

internal class AIAnalyticsDataSource @Inject constructor(
    private val selectedSite: SelectedSite,
    private val statsStore: WCStatsStore,
) {
    suspend fun fetchRevenueStats(
        after: String,
        before: String,
        interval: AnalyticsInterval,
        currency: String?,
    ): Result<AnalyticsStats> = fetchStats(rangeIdPrefix = "ai_revenue") { site, rangeId ->
        statsStore.fetchRevenueStats(
            FetchRevenueStatsPayload(
                site = site,
                granularity = interval.statsGranularity,
                startDate = after,
                endDate = before,
                forced = false,
                revenueRangeId = rangeId,
                currency = currency,
            )
        )
    }

    suspend fun fetchOrdersStats(
        after: String,
        before: String,
        interval: AnalyticsInterval,
    ): Result<AnalyticsStats> = fetchStats(rangeIdPrefix = "ai_orders") { site, rangeId ->
        statsStore.fetchOrdersStats(
            FetchOrdersStatsPayload(
                site = site,
                granularity = interval.statsGranularity,
                startDate = after,
                endDate = before,
                forced = false,
                orderStatsRangeId = rangeId,
            )
        )
    }

    private suspend fun fetchStats(
        rangeIdPrefix: String,
        fetch: suspend (SiteModel, String) -> WCStatsStore.OnWCRevenueStatsChanged,
    ): Result<AnalyticsStats> = runCatching {
        val site = selectedSite.get()
        val rangeId = "$rangeIdPrefix:${UUID.randomUUID()}"
        val result = fetch(site, rangeId)
        if (result.isError) {
            throw OnChangedException(requireNotNull(result.error))
        }
        val rawStats = statsStore.getRawRevenueStatsFromRangeId(site, rangeId)
            ?: error("Stats response missing for range $rangeId")
        rawStats.toAnalyticsStats()
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
}
