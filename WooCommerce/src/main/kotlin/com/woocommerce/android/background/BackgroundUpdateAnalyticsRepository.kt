package com.woocommerce.android.background

import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.Companion.TOP_BUNDLES_LIST_SIZE
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.Companion.TOP_PRODUCTS_LIST_SIZE
import com.woocommerce.android.ui.analytics.hub.sync.toIntervalString
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.revenueStatsGranularity
import com.woocommerce.android.ui.analytics.ranges.visitorStatsGranularity
import com.woocommerce.android.ui.analytics.ranges.visitorSummaryStatsGranularity
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.ui.dashboard.data.asRevenueRangeId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.fluxc.store.WCGoogleStore
import javax.inject.Inject

/**
 * Repository responsible for handling analytics data updates in background tasks.
 *
 * Unlike the AnalyticsRepository, this class does not manage cache implementation
 * and focuses on fetching stats and providing detailed error messages for troubleshooting.
 *
 * @see com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository
 */
class BackgroundUpdateAnalyticsRepository @Inject constructor(
    private val statsRepository: StatsRepository,
    private val googleAdsStore: WCGoogleStore,
    private val selectedSite: SelectedSite
) {
    suspend fun fetchRevenueStats(selectedRange: StatsTimeRangeSelection) = coroutineScope {
        val currentPeriod = selectedRange.currentRange
        val statsIdentifier =
            selectedRange.selectionType.identifier.asRevenueRangeId(currentPeriod.start, currentPeriod.end)

        val previousPeriod = selectedRange.previousRange
        val previousStatsIdentifier =
            selectedRange.selectionType.identifier.asRevenueRangeId(previousPeriod.start, previousPeriod.end)

        val currentResult = async {
            statsRepository.fetchRevenueStats(
                range = currentPeriod,
                granularity = selectedRange.revenueStatsGranularity,
                forced = true,
                revenueRangeId = statsIdentifier
            )
        }

        val previousResult = async {
            statsRepository.fetchRevenueStats(
                range = previousPeriod,
                granularity = selectedRange.revenueStatsGranularity,
                forced = true,
                revenueRangeId = previousStatsIdentifier
            )
        }

        val errors = awaitAll(currentResult, previousResult)
            .filter { it.isFailure }
            .map {
                it.exceptionOrNull()
                    ?: Exception("${BackgroundUpdateAnalyticsRepository::class.java.name} Unknown error")
            }

        if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(MultipleErrorsException(errors))
        }
    }

    suspend fun fetchTopPerformers(selectedRange: StatsTimeRangeSelection): Result<Unit> {
        val totalPeriod = selectedRange.currentRange
        val startDate = totalPeriod.start.formatToYYYYmmDDhhmmss()
        val endDate = totalPeriod.end.formatToYYYYmmDDhhmmss()

        return statsRepository.fetchTopPerformerProducts(
            forceRefresh = true,
            startDate = startDate,
            endDate = endDate,
            quantity = TOP_PRODUCTS_LIST_SIZE
        )
    }

    suspend fun fetchVisitorsStats(selectedRange: StatsTimeRangeSelection): Result<Int> {
        return when (selectedRange.selectionType) {
            SelectionType.LAST_QUARTER, SelectionType.QUARTER_TO_DATE -> {
                statsRepository.fetchVisitorStats(
                    range = selectedRange.currentRange,
                    granularity = selectedRange.visitorStatsGranularity,
                    forced = true
                ).map { it.values.sum() }
            }

            else -> {
                statsRepository.fetchTotalVisitorStats(
                    date = selectedRange.currentRange.end,
                    granularity = selectedRange.visitorSummaryStatsGranularity,
                    forced = true,
                )
            }
        }
    }

    suspend fun fetchGiftCardsStats(selectedRange: StatsTimeRangeSelection) = coroutineScope {
        val interval = selectedRange.revenueStatsGranularity.toIntervalString()
        val currentPeriod = selectedRange.currentRange
        val currentStartDate = currentPeriod.start.formatToYYYYmmDDhhmmss()
        val currentEndDate = currentPeriod.end.formatToYYYYmmDDhhmmss()

        val previousPeriod = selectedRange.previousRange
        val previousStartDate = previousPeriod.start.formatToYYYYmmDDhhmmss()
        val previousEndDate = previousPeriod.end.formatToYYYYmmDDhhmmss()

        val currentGiftCardsStats = async {
            statsRepository.fetchGiftCardStats(
                startDate = currentStartDate,
                endDate = currentEndDate,
                interval = interval
            )
        }

        val previousGiftCardsStats = async {
            statsRepository.fetchGiftCardStats(
                startDate = previousStartDate,
                endDate = previousEndDate,
                interval = interval
            )
        }

        val errors = awaitAll(currentGiftCardsStats, previousGiftCardsStats)
            .filter { it.isError }
            .map { Exception("${BackgroundUpdateAnalyticsRepository::class.java.name} ${it.error.message}") }

        if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(MultipleErrorsException(errors))
        }
    }

    suspend fun fetchProductBundlesStats(selectedRange: StatsTimeRangeSelection) = coroutineScope {
        val currentPeriod = selectedRange.currentRange
        val currentStartDate = currentPeriod.start.formatToYYYYmmDDhhmmss()
        val currentEndDate = currentPeriod.end.formatToYYYYmmDDhhmmss()

        val previousPeriod = selectedRange.previousRange
        val previousStartDate = previousPeriod.start.formatToYYYYmmDDhhmmss()
        val previousEndDate = previousPeriod.end.formatToYYYYmmDDhhmmss()

        val currentBundleStats = async {
            statsRepository.fetchProductBundlesStats(
                startDate = currentStartDate,
                endDate = currentEndDate
            )
        }

        val previousBundleStats = async {
            statsRepository.fetchProductBundlesStats(
                startDate = previousStartDate,
                endDate = previousEndDate
            )
        }

        val bundlesReport = async {
            statsRepository.fetchBundleReport(
                startDate = currentStartDate,
                endDate = currentEndDate,
                quantity = TOP_BUNDLES_LIST_SIZE
            )
        }

        val errors = awaitAll(currentBundleStats, previousBundleStats, bundlesReport)
            .filter { it.isError }
            .map { Exception("${BackgroundUpdateAnalyticsRepository::class.java.name} ${it.error.message}") }

        if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(MultipleErrorsException(errors))
        }
    }

    suspend fun fetchGoogleAdsStats(selectedRange: StatsTimeRangeSelection) = coroutineScope {
        val currentPeriod = selectedRange.currentRange
        val currentStartDate = currentPeriod.start.formatToYYYYmmDDhhmmss()
        val currentEndDate = currentPeriod.end.formatToYYYYmmDDhhmmss()

        val previousPeriod = selectedRange.previousRange
        val previousStartDate = previousPeriod.start.formatToYYYYmmDDhhmmss()
        val previousEndDate = previousPeriod.end.formatToYYYYmmDDhhmmss()

        val currentRangeGoogleAdsStats = async {
            googleAdsStore.fetchAllPrograms(
                site = selectedSite.get(),
                startDate = currentStartDate,
                endDate = currentEndDate,
                totals = WCGoogleStore.TotalsType.entries
            )
        }

        val previousRangeGoogleAdsStats = async {
            googleAdsStore.fetchAllPrograms(
                site = selectedSite.get(),
                startDate = previousStartDate,
                endDate = previousEndDate,
                totals = WCGoogleStore.TotalsType.entries
            )
        }

        val errors = awaitAll(currentRangeGoogleAdsStats, previousRangeGoogleAdsStats)
            .filter { it.isError }
            .map { Exception("${BackgroundUpdateAnalyticsRepository::class.java.name} ${it.error.message}") }

        if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(MultipleErrorsException(errors))
        }
    }
}

enum class BackgroundAPICalls {
    REVENUE_STATS, TOP_PERFORMERS, VISITORS_STATS, GIFT_CARDS_STATS, PRODUCT_BUNDLES_STATS, GOOGLE_ADS_STATS
}
