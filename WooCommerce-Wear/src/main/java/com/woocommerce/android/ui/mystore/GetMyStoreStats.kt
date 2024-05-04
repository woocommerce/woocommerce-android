package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.mystore.stats.StatsRepository
import com.woocommerce.commons.extensions.convertedFrom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class GetMyStoreStats @Inject constructor(
    private val statsRepository: StatsRepository,
) {
    private val revenueStats = MutableStateFlow<RevenueData?>(null)
    private val visitorStats = MutableStateFlow<Int?>(null)

    suspend operator fun invoke(selectedSite: SiteModel): Flow<MyStoreStatsData> {
        fetchRevenueStats(selectedSite)
        fetchVisitorsStats(selectedSite)

        return combine(
            revenueStats,
            visitorStats
        ) { revenueStats, visitorStats ->
            MyStoreStatsData(
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

    data class RevenueData(
        val totalRevenue: Double,
        val orderCount: Int
    )

    data class MyStoreStatsData(
        private val revenueData: RevenueData?,
        private val visitorData: Int?
    ) {
        val isFinished
            get() = revenueData != null &&
                visitorData != null
        val revenue
            get() = revenueData?.totalRevenue ?: 0.0

        val ordersCount
            get() = revenueData?.orderCount ?: 0

        val visitorsCount
            get() = visitorData ?: 0

        val conversionRate: String
            get() {
                val ordersCount = revenueData?.orderCount ?: 0
                val visitorsCount = visitorData ?: 0
                return ordersCount convertedFrom visitorsCount
            }
    }
}
