package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.mystore.GetMyStoreStats.StatResult.RevenueStatResult
import com.woocommerce.android.ui.mystore.GetMyStoreStats.StatResult.VisitorStatResult
import com.woocommerce.android.ui.mystore.stats.StatsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel

class GetMyStoreStats @Inject constructor(
    private val statsRepository: StatsRepository,
    private val coroutineScope: CoroutineScope
) {
    private val revenueStats = MutableStateFlow<RevenueStatResult?>(null)
    private val visitorStats = MutableStateFlow<VisitorStatResult?>(null)

    suspend operator fun invoke(selectedSite: SiteModel): Flow<MyStoreStatsData> {
        listOf(
            coroutineScope.fetchRevenueStats(selectedSite)
        ).awaitAll()

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

    private fun CoroutineScope.fetchRevenueStats(selectedSite: SiteModel) =
        async {
            statsRepository.fetchRevenueStats(selectedSite)
                .fold(
                    onSuccess = { revenue ->
                        val totals = revenue?.parseTotal()?.netRevenue ?: 0.0
                        revenueStats.value = RevenueStatResult(Result.success(totals))
                    },
                    onFailure = {
                        revenueStats.value = RevenueStatResult(Result.failure(Exception()))
                    }
                )
        }

    data class MyStoreStatsData(
        val revenueData: RevenueStatResult?,
        val visitorData: VisitorStatResult?
    ) {
        val isFinished
            get() = revenueData != null
                && visitorData != null
    }

    sealed class StatResult<T>(
        val result: Result<T>
    ) {
        data class RevenueStatResult(
            val revenueStats: Result<Double>
        ) : StatResult<Double>(revenueStats)

        data class VisitorStatResult(
            val visitorStats: Result<Int>
        ) : StatResult<Int>(visitorStats)
    }
}
