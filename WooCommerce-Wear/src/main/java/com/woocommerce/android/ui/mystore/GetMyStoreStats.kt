package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.mystore.GetMyStoreStats.StatResult.RevenueStatResult
import com.woocommerce.android.ui.mystore.GetMyStoreStats.StatResult.VisitorStatResult
import com.woocommerce.android.ui.mystore.stats.StatsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel

class GetMyStoreStats @Inject constructor(
    private val statsRepository: StatsRepository,
    private val coroutineScope: CoroutineScope
) {
    suspend operator fun invoke(selectedSite: SiteModel) {
        listOf(
            coroutineScope.fetchRevenueStats(selectedSite)
        ).awaitAll()
    }

    private fun CoroutineScope.fetchRevenueStats(selectedSite: SiteModel) =
        async {
            statsRepository.fetchRevenueStats(selectedSite)
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
            val revenueStats: Result<WCRevenueStatsModel>
        ) : StatResult<WCRevenueStatsModel>(revenueStats)

        data class VisitorStatResult(
            val visitorStats: Result<WCRevenueStatsModel>
        ) : StatResult<WCRevenueStatsModel>(visitorStats)
    }
}
