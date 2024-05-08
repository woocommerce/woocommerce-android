package com.woocommerce.android.ui.stats.datasource

import com.woocommerce.android.ui.stats.datasource.MyStoreStatsRequest.Data.RevenueData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class FetchStatsFromStore @Inject constructor(
    private val statsRepository: StatsRepository,
    private val wooCommerceStore: WooCommerceStore
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
                visitorData = visitorStats,
            )
        }.filter { it.isFinished }
    }

    private suspend fun fetchRevenueStats(selectedSite: SiteModel) {
        statsRepository.fetchRevenueStats(selectedSite)
            .fold(
                onSuccess = { revenue ->
                    val totals = revenue?.parseTotal()

                    val formattedRevenue = wooCommerceStore.formatCurrencyForDisplay(
                        amount = totals?.totalSales ?: 0.0,
                        site = selectedSite,
                        currencyCode = null,
                        applyDecimalFormatting = true
                    )

                    val revenueData = RevenueData(
                        totalRevenue = formattedRevenue,
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
