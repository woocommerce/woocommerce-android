package com.woocommerce.android.ui.mystore.datasource

import com.woocommerce.android.ui.mystore.stats.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class FetchStatsFromStore @Inject constructor(
    private val statsRepository: StatsRepository,
) {
    private val revenueStats = MutableStateFlow<RevenueData?>(null)
    private val visitorStats = MutableStateFlow<Int?>(null)

    suspend operator fun invoke(
        selectedSite: SiteModel
    ): Flow<MyStoreStatsRequest> {
        fetchRevenueStats(selectedSite)
        fetchVisitorsStats(selectedSite)

        return combine(
            revenueStats,
            visitorStats
        ) { revenueStats, visitorStats ->
            MyStoreStatsRequest.Data(
                revenueData = revenueStats,
                visitorData = visitorStats
            )
        }.filter { it.isFinished }
    }

    private suspend fun fetchRevenueStats(selectedSite: SiteModel) {
        statsRepository.fetchRevenueStats(selectedSite)
            .fold(
                onSuccess = { revenue ->
                    val totals = revenue?.parseTotal()
                    val revenueData = RevenueData(
                        totalRevenue = totals?.totalSales ?: 0.0,
                        orderCount = totals?.ordersCount ?: 0
                    )
                    revenueStats.value = revenueData
                },
                onFailure = {
                    revenueStats.value = null
                }
            )
    }

    private suspend fun fetchVisitorsStats(selectedSite: SiteModel) {
        statsRepository.fetchVisitorStats(selectedSite)
            .fold(
                onSuccess = { visitors ->
                    visitorStats.value = visitors ?: 0
                },
                onFailure = {
                    visitorStats.value = null
                }
            )
    }
}
