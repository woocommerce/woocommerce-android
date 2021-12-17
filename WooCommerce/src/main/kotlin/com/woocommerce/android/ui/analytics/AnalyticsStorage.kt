package com.woocommerce.android.ui.analytics

import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class AnalyticsStorage @Inject constructor() {
    private val statsConsumptions = mutableMapOf<Int, AtomicInteger>()
    private val statsCache = mutableMapOf<Int, WCRevenueStatsModel>()

    /**
     * This in memory cache allows only MAX_STATS_CACHE_CONSUMPTIONS hits.
     * After reach that number is restarted and next gets will miss until is populated again
     */
    fun getStats(startDate: String, endDate: String): WCRevenueStatsModel? {
        val cacheKey = getStatsCacheKey(startDate, endDate)
        val cacheConsumptionTimes = statsConsumptions.getOrElse(cacheKey) { AtomicInteger(0) }
        if (cacheConsumptionTimes.get() < MAX_STATS_CACHE_CONSUMPTIONS) {
            statsCache[cacheKey]?.let {
                statsConsumptions[cacheKey]?.incrementAndGet() ?: statsConsumptions.put(cacheKey, AtomicInteger(1))
                return it
            }
        }
        return null
    }

    fun saveStats(startDate: String, endDate: String, it: WCRevenueStatsModel) {
        val cacheKey = getStatsCacheKey(startDate, endDate)
        statsConsumptions[cacheKey] = AtomicInteger(0)
        statsCache[cacheKey] = it
    }

    companion object {
        const val MAX_STATS_CACHE_CONSUMPTIONS = 2
    }

    private fun getStatsCacheKey(startDate: String, endDate: String) = Objects.hash(startDate + endDate)
}
