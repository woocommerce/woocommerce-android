package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.mystore.GetMyStoreStats.StatResult.RevenueStatResult
import com.woocommerce.android.ui.mystore.GetMyStoreStats.StatResult.VisitorStatResult
import com.woocommerce.android.ui.mystore.stats.StatsRepository
import com.woocommerce.commons.extensions.convertedFrom
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import org.wordpress.android.fluxc.model.SiteModel

class GetMyStoreStats @Inject constructor(
    private val statsRepository: StatsRepository,
) {
    private val revenueStats = MutableStateFlow<RevenueStatResult?>(null)
    private val visitorStats = MutableStateFlow<VisitorStatResult?>(null)

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
                        totalRevenue = totals?.netRevenue ?: 0.0,
                        orderCount = totals?.ordersCount ?: 0
                    )
                    revenueStats.value = RevenueStatResult(Result.success(revenueData))
                },
                onFailure = {
                    revenueStats.value = RevenueStatResult(Result.failure(Exception()))
                }
            )
    }

    private suspend fun fetchVisitorsStats(selectedSite: SiteModel) {
        statsRepository.fetchVisitorStats(selectedSite)
            .fold(
                onSuccess = { visitors ->
                    visitorStats.value = VisitorStatResult(Result.success(visitors ?: 0))
                },
                onFailure = {
                    visitorStats.value = VisitorStatResult(Result.failure(Exception()))
                }
            )
    }

    data class RevenueData(
        val totalRevenue: Double,
        val orderCount: Int
    )

    sealed class StatResult<T>(
        val result: Result<T>
    ) {
        data class RevenueStatResult(
            val revenueStats: Result<RevenueData>
        ) : StatResult<RevenueData>(revenueStats)

        data class VisitorStatResult(
            val visitorStats: Result<Int>
        ) : StatResult<Int>(visitorStats)
    }

    data class MyStoreStatsData(
        private val revenueData: RevenueStatResult?,
        private val visitorData: VisitorStatResult?
    ) {
        val isFinished
            get() = revenueData != null
                && visitorData != null
        val revenue
            get() = revenueData?.result?.getOrNull()?.totalRevenue ?: 0.0

        val ordersCount
            get() = revenueData?.result?.getOrNull()?.orderCount ?: 0

        val visitorsCount
            get() = visitorData?.result?.getOrNull() ?: 0

        val conversionRate: String
            get() {
                val ordersCount = revenueData?.result?.getOrNull()?.orderCount ?: 0
                val visitorsCount = visitorData?.result?.getOrNull() ?: 0
                return ordersCount convertedFrom visitorsCount
            }
    }
}
