package com.woocommerce.android.background

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateAnalyticsDataByRangeSelection @Inject constructor(
    private val analyticsCardsConfiguration: ObserveAnalyticsCardsConfiguration,
    private val analyticsRepository: AnalyticsRepository
) {
    @Suppress("LongMethod")
    suspend operator fun invoke(
        selectedRange: StatsTimeRangeSelection,
        forceCardUpdates: List<AnalyticsCards> = emptyList()
    ): Boolean {
        val visibleCards = analyticsCardsConfiguration.invoke()
            .first()
            .filter { analyticCardConfiguration -> analyticCardConfiguration.isVisible }
            .map { analyticCardConfiguration -> analyticCardConfiguration.card }
            .union(forceCardUpdates)

        return coroutineScope {
            val asyncCalls = visibleCards.map { visibleCard ->
                when (visibleCard) {
                    AnalyticsCards.Revenue -> {
                        async {
                            val result = analyticsRepository.fetchRevenueData(
                                selectedRange,
                                AnalyticsRepository.FetchStrategy.ForceNew
                            )
                            result is AnalyticsRepository.RevenueResult.RevenueData
                        }
                    }
                    AnalyticsCards.Orders -> {
                        async {
                            val result = analyticsRepository.fetchOrdersData(
                                selectedRange,
                                AnalyticsRepository.FetchStrategy.ForceNew
                            )
                            result is AnalyticsRepository.OrdersResult.OrdersData
                        }
                    }
                    AnalyticsCards.Products -> {
                        async {
                            val result = analyticsRepository.fetchProductsData(
                                selectedRange,
                                AnalyticsRepository.FetchStrategy.ForceNew
                            )
                            result is AnalyticsRepository.ProductsResult.ProductsData
                        }
                    }
                    AnalyticsCards.Session -> {
                        async {
                            val result = analyticsRepository.fetchVisitorsData(
                                selectedRange,
                                AnalyticsRepository.FetchStrategy.ForceNew
                            )
                            result is AnalyticsRepository.VisitorsResult.VisitorsData ||
                                result is AnalyticsRepository.VisitorsResult.VisitorsNotSupported
                        }
                    }
                    AnalyticsCards.Bundles -> {
                        async {
                            val result = analyticsRepository.fetchProductBundlesStats(selectedRange)
                            result is AnalyticsRepository.BundlesResult.BundlesData
                        }
                    }
                    AnalyticsCards.GiftCards -> {
                        async {
                            val result = analyticsRepository.fetchGiftCardsStats(selectedRange)
                            result is AnalyticsRepository.GiftCardResult.GiftCardData
                        }
                    }
                }
            }
            asyncCalls.awaitAll().all { succeed -> succeed }
        }
    }
}
