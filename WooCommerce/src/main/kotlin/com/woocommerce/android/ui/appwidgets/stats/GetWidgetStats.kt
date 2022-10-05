package com.woocommerce.android.ui.appwidgets.stats

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WCStatsStore
import javax.inject.Inject

class GetWidgetStats @Inject constructor(
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val statsRepository: StatsRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus
) {
    suspend operator fun invoke(
        granularity: WCStatsStore.StatsGranularity,
        siteModel: SiteModel?
    ): WidgetStatsResult {
        return withContext(coroutineDispatchers.io) {
            when {
                // If user is not logged in, exit the function with WidgetStatsAuthFailure
                accountStore.hasAccessToken().not() -> WidgetStatsResult.WidgetStatsAuthFailure
                // If V4 stats is not supported, exit the function with WidgetStatsAPINotSupportedFailure
                appPrefsWrapper.isV4StatsSupported().not() -> WidgetStatsResult.WidgetStatsAPINotSupportedFailure
                // If network is not available, exit the function with WidgetStatsNetworkFailure
                networkStatus.isConnected().not() -> WidgetStatsResult.WidgetStatsNetworkFailure
                // If siteModel is null, exit the function with WidgetStatsFailure
                siteModel == null -> WidgetStatsResult.WidgetStatsFailure("No site selected")
                else -> {
                    // Fetch stats, always force to refresh data
                    val fetchedStats = statsRepository.fetchStats(
                        granularity = granularity,
                        forced = true,
                        site = siteModel
                    )
                    if (fetchedStats.isError) {
                        WidgetStatsResult.WidgetStatsFailure(fetchedStats.error.message)
                    } else {
                        WidgetStatsResult.WidgetStats(fetchedStats.model!!)
                    }
                }
            }
        }
    }

    sealed class WidgetStatsResult {
        object WidgetStatsAuthFailure : WidgetStatsResult()
        object WidgetStatsNetworkFailure : WidgetStatsResult()
        object WidgetStatsAPINotSupportedFailure : WidgetStatsResult()
        data class WidgetStatsFailure(val errorMessage: String?) : WidgetStatsResult()
        data class WidgetStats(
            private val revenueModel: WCRevenueStatsModel?,
            private val visitorsMap: Map<String, Int>?,
            val currencyCode: String
        ) : WidgetStatsResult() {
            constructor(stats: StatsRepository.SiteStats) : this(stats.revenue, stats.visitors, stats.currencyCode)

            val visitorsTotal: Int
            val ordersTotal: Int
            val revenueGross: Double

            init {
                var grossRevenue = 0.0
                var orderCount = 0
                revenueModel?.parseTotal()?.let { total ->
                    grossRevenue = total.totalSales ?: 0.0
                    orderCount = total.ordersCount ?: 0
                }

                visitorsTotal = visitorsMap?.values?.sum() ?: 0
                ordersTotal = orderCount
                revenueGross = grossRevenue
            }
        }
    }
}
