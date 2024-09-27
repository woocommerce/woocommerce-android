package com.woocommerce.android.background

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateAnalyticsDataByRangeSelection @Inject constructor(
    private val analyticsCardsConfiguration: ObserveAnalyticsCardsConfiguration,
    private val backgroundUpdateAnalyticsRepository: BackgroundUpdateAnalyticsRepository,
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore
) {
    suspend operator fun invoke(
        selectedRange: StatsTimeRangeSelection,
        forceCardUpdates: List<AnalyticsCards> = emptyList()
    ): Result<Unit> {
        val visibleCards = analyticsCardsConfiguration.invoke()
            .first()
            .filter { analyticCardConfiguration -> analyticCardConfiguration.isVisible }
            .map { analyticCardConfiguration -> analyticCardConfiguration.card }
            .union(forceCardUpdates)

        return coroutineScope {
            val apiCalls = getAPICalls(visibleCards)
            val apiCallsResults = mutableMapOf<BackgroundAPICalls, Result<Any>>()
            val asyncCalls = apiCalls.map { call ->
                when (call) {
                    BackgroundAPICalls.REVENUE_STATS -> {
                        async {
                            apiCallsResults[call] =
                                backgroundUpdateAnalyticsRepository.fetchRevenueStats(selectedRange)
                        }
                    }

                    BackgroundAPICalls.TOP_PERFORMERS -> {
                        async {
                            apiCallsResults[call] =
                                backgroundUpdateAnalyticsRepository.fetchTopPerformers(selectedRange)
                        }
                    }

                    BackgroundAPICalls.VISITORS_STATS -> {
                        async {
                            apiCallsResults[call] =
                                backgroundUpdateAnalyticsRepository.fetchVisitorsStats(selectedRange)
                        }
                    }

                    BackgroundAPICalls.GIFT_CARDS_STATS -> TODO()
                    BackgroundAPICalls.PRODUCT_BUNDLES_STATS -> TODO()
                    BackgroundAPICalls.GOOGLE_ADS_STATS -> TODO()
                }
            }

            asyncCalls.awaitAll()

            updateLastUpdatedTime(selectedRange, apiCallsResults)

            val errors = apiCallsResults.values.filter { it.isFailure }.map {
                it.exceptionOrNull()
                    ?: Exception("${UpdateAnalyticsDataByRangeSelection::class.java.name} Unknown error")
            }

            when {
                errors.isEmpty() -> {
                    Result.success(Unit)
                }
                errors.size == 1 -> {
                    Result.failure(errors.first())
                }
                else -> {
                    Result.failure(MultipleErrorsException(errors))
                }
            }
        }
    }

    private suspend fun updateLastUpdatedTime(
        selectedRange: StatsTimeRangeSelection,
        apiCallsResults: MutableMap<BackgroundAPICalls, Result<Any>>
    ) {
        if (apiCallsResults[BackgroundAPICalls.REVENUE_STATS]?.isSuccess == true) {
            analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                selectedRange,
                AnalyticsUpdateDataStore.AnalyticData.REVENUE
            )
            analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                selectedRange,
                AnalyticsUpdateDataStore.AnalyticData.ORDERS
            )
        }
        if (apiCallsResults[BackgroundAPICalls.REVENUE_STATS]?.isSuccess == true &&
            apiCallsResults[BackgroundAPICalls.TOP_PERFORMERS]?.isSuccess == true
        ) {
            analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                selectedRange,
                AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMERS
            )
        }
        if (apiCallsResults[BackgroundAPICalls.VISITORS_STATS]?.isSuccess == true) {
            analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                selectedRange,
                AnalyticsUpdateDataStore.AnalyticData.VISITORS
            )
        }
    }

    private fun getAPICalls(visibleCards: Set<AnalyticsCards>): Set<BackgroundAPICalls> {
        val apiCalls = mutableSetOf<BackgroundAPICalls>()
        visibleCards.forEach { visibleCard ->
            when (visibleCard) {
                AnalyticsCards.Revenue -> {
                    apiCalls.add(BackgroundAPICalls.REVENUE_STATS)
                }

                AnalyticsCards.Orders -> {
                    apiCalls.add(BackgroundAPICalls.REVENUE_STATS)
                }

                AnalyticsCards.Products -> {
                    apiCalls.add(BackgroundAPICalls.REVENUE_STATS)
                    apiCalls.add(BackgroundAPICalls.TOP_PERFORMERS)
                }

                AnalyticsCards.Session -> {
                    apiCalls.add(BackgroundAPICalls.VISITORS_STATS)
                }

                AnalyticsCards.Bundles -> {
                    // apiCalls.add(BackgroundAPICalls.PRODUCT_BUNDLES_STATS)
                }

                AnalyticsCards.GiftCards -> {
                    // apiCalls.add(BackgroundAPICalls.GIFT_CARDS_STATS)
                }

                AnalyticsCards.GoogleAds -> {
                    // apiCalls.add(BackgroundAPICalls.GOOGLE_ADS_STATS)
                }
            }
        }
        return apiCalls
    }
}
