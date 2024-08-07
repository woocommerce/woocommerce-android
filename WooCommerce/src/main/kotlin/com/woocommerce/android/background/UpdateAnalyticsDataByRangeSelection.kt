package com.woocommerce.android.background

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateAnalyticsDataByRangeSelection @Inject constructor(
    private val analyticsCardsConfiguration: ObserveAnalyticsCardsConfiguration,
    private val analyticsRepository: AnalyticsRepository,
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore
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
                            val isSuccess = result is AnalyticsRepository.RevenueResult.RevenueData
                            if (isSuccess) {
                                analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                                    selectedRange,
                                    AnalyticsUpdateDataStore.AnalyticData.REVENUE
                                )
                            }
                            isSuccess
                        }
                    }

                    AnalyticsCards.Orders -> {
                        async {
                            val result = analyticsRepository.fetchOrdersData(
                                selectedRange,
                                AnalyticsRepository.FetchStrategy.ForceNew
                            )
                            val isSuccess = result is AnalyticsRepository.OrdersResult.OrdersData
                            if (isSuccess) {
                                analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                                    selectedRange,
                                    AnalyticsUpdateDataStore.AnalyticData.ORDERS
                                )
                            }
                            isSuccess
                        }
                    }

                    AnalyticsCards.Products -> {
                        async {
                            val result = analyticsRepository.fetchProductsData(
                                selectedRange,
                                AnalyticsRepository.FetchStrategy.ForceNew
                            )
                            val isSuccess = result is AnalyticsRepository.ProductsResult.ProductsData
                            if (isSuccess) {
                                analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                                    selectedRange,
                                    AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMERS
                                )
                            }
                            isSuccess
                        }
                    }

                    AnalyticsCards.Session -> {
                        async {
                            val result = analyticsRepository.fetchVisitorsData(
                                selectedRange,
                                AnalyticsRepository.FetchStrategy.ForceNew
                            )
                            when (result) {
                                is AnalyticsRepository.VisitorsResult.VisitorsData -> {
                                    analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                                        selectedRange,
                                        AnalyticsUpdateDataStore.AnalyticData.VISITORS
                                    )
                                    true
                                }
                                is AnalyticsRepository.VisitorsResult.VisitorsNotSupported -> true
                                else -> {
                                    false
                                }
                            }
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

                    AnalyticsCards.GoogleAds -> {
                        async {
                            val result = analyticsRepository.fetchGoogleAdsStats(selectedRange)
                            result is AnalyticsRepository.GoogleAdsResult.GoogleAdsData
                        }
                    }
                }
            }
            asyncCalls.awaitAll().all { it }
        }
    }
}
