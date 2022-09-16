package com.woocommerce.android.ui.appwidgets.stats.today

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WCStatsStore
import javax.inject.Inject

class GetWidgetStats @Inject constructor(
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val statsRepository: StatsRepository,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(
        granularity: WCStatsStore.StatsGranularity,
        siteModel: SiteModel
    ): WooResult<WidgetStats> {
        return withContext(coroutineDispatchers.io) {
            when {
                // If user is not logged in, exit the function with WooError
                accountStore.hasAccessToken().not() -> {
                    val error = WooError(
                        type = WooErrorType.AUTHORIZATION_REQUIRED,
                        original = BaseRequest.GenericErrorType.NOT_AUTHENTICATED,
                        message = "User not logged in"
                    )
                    WooResult(error)
                }
                // If V4 stats is not supported, exit the function with WooError
                appPrefsWrapper.isV4StatsSupported().not() -> {
                    val error = WooError(
                        type = WooErrorType.API_ERROR,
                        original = BaseRequest.GenericErrorType.NOT_FOUND,
                        message = "V4 Stats nor supported "
                    )
                    WooResult(error)
                }
                else -> {
                    // Fetch stats, always force to refresh data
                    val fetchedStats = statsRepository.fetchStats(
                        granularity = granularity,
                        forced = true,
                        site = siteModel
                    )
                    if (fetchedStats.isError) {
                        WooResult(fetchedStats.error)
                    } else {
                        val stats = WidgetStats(fetchedStats.model!!)
                        WooResult(stats)
                    }
                }
            }
        }
    }

    data class WidgetStats(
        private val revenueModel: WCRevenueStatsModel?,
        private val visitorsMap: Map<String, Int>?
    ) {
        constructor(stats: StatsRepository.SiteStats) : this(stats.revenue, stats.visitors)

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
