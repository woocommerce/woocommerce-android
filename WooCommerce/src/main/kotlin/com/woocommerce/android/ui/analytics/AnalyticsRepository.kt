package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.AnalyticStat.RevenueStat
import com.woocommerce.android.ui.mystore.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCStatsStore
import javax.inject.Inject

class AnalyticsRepository @Inject constructor(
    private val statsRepository: StatsRepository,
    private val wcStatsStore: WCStatsStore
) {

    init {
        statsRepository.init()
    }

    suspend fun fetchRevenueData(startDate: String, endDate: String): RevenueStat? {
        return withContext(Dispatchers.IO) {
            statsRepository.init()
            statsRepository.fetchRevenueStats(
                WCStatsStore.StatsGranularity.DAYS,
                true,
                startDate,
                endDate
            ).fold({
                val total = it?.parseTotal()
                RevenueStat(total?.totalSales!!, 10, total.netSales!!, -10)
            }, { null })
        }
    }
}
