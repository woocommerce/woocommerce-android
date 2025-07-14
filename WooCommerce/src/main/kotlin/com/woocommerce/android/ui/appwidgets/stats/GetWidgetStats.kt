package com.woocommerce.android.ui.appwidgets.stats

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.revenueStatsGranularity
import com.woocommerce.android.ui.analytics.ranges.visitorStatsGranularity
import com.woocommerce.android.ui.appwidgets.IsDeviceBatterySaverActive
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStatsAPINotSupportedFailure
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStatsAuthFailure
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStatsBatterySaverActive
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStatsFailure
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStatsNetworkFailure
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import javax.inject.Inject

class GetWidgetStats @Inject constructor(
    private val accountRepository: AccountRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val statsRepository: StatsRepository,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val isDeviceBatterySaverActive: IsDeviceBatterySaverActive,
    private val networkStatus: NetworkStatus
) {
    suspend operator fun invoke(
        rangeSelection: StatsTimeRangeSelection,
        siteModel: SiteModel?
    ): WidgetStatsResult {
        return withContext(coroutineDispatchers.io) {
            when {
                isDeviceBatterySaverActive() -> WidgetStatsBatterySaverActive
                // If user is not logged in, exit the function with WidgetStatsAuthFailure
                accountRepository.isUserLoggedIn().not() -> WidgetStatsAuthFailure
                // If V4 stats is not supported, exit the function with WidgetStatsAPINotSupportedFailure
                appPrefsWrapper.isV4StatsSupported().not() -> WidgetStatsAPINotSupportedFailure
                // If network is not available, exit the function with WidgetStatsNetworkFailure
                networkStatus.isConnected().not() -> WidgetStatsNetworkFailure
                // If siteModel is null, exit the function with WidgetStatsFailure
                siteModel == null -> WidgetStatsFailure("No site selected")
                else -> {
                    val areVisitorStatsSupported = siteModel.connectionType == SiteConnectionType.Jetpack

                    // Fetch stats, always force to refresh data.
                    val fetchedStats = statsRepository.fetchStats(
                        range = rangeSelection.currentRange,
                        revenueStatsGranularity = rangeSelection.revenueStatsGranularity,
                        visitorStatsGranularity = rangeSelection.visitorStatsGranularity,
                        forced = true,
                        includeVisitorStats = areVisitorStatsSupported,
                        site = siteModel
                    )
                    fetchedStats.fold(
                        onSuccess = { stats ->
                            WidgetStatsResult.WidgetStats(stats)
                        },
                        onFailure = { error ->
                            WidgetStatsFailure(error.message)
                        }
                    )
                }
            }
        }
    }

    sealed class WidgetStatsResult {
        data object WidgetStatsBatterySaverActive : WidgetStatsResult()
        data object WidgetStatsAuthFailure : WidgetStatsResult()
        data object WidgetStatsNetworkFailure : WidgetStatsResult()
        data object WidgetStatsAPINotSupportedFailure : WidgetStatsResult()
        data class WidgetStatsFailure(val errorMessage: String?) : WidgetStatsResult()
        data class WidgetStats(
            private val revenueModel: WCRevenueStatsModel?,
            private val visitorsMap: Map<String, Int>?,
            val currencyCode: String
        ) : WidgetStatsResult() {
            constructor(
                stats: StatsRepository.SiteStats,
            ) : this(stats.revenue, stats.visitors, stats.currencyCode)

            val visitorsTotal: Int?
            val ordersTotal: Int
            val revenueGross: Double

            init {
                var grossRevenue = 0.0
                var orderCount = 0
                revenueModel?.parseTotal()?.let { total ->
                    grossRevenue = total.totalSales ?: 0.0
                    orderCount = total.ordersCount ?: 0
                }

                visitorsTotal = visitorsMap?.values?.sum()
                ordersTotal = orderCount
                revenueGross = grossRevenue
            }
        }
    }
}
