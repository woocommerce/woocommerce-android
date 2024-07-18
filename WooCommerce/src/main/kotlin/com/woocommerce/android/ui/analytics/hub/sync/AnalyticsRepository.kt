package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.model.BundleItem
import com.woocommerce.android.model.BundleStat
import com.woocommerce.android.model.GoogleAdsCampaign
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.GiftCardsStat
import com.woocommerce.android.model.GoogleAdsStat
import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductItem
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.FetchStrategy.ForceNew
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.FetchStrategy.Saved
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.OrdersResult.OrdersError
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.ProductsResult.ProductsError
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.ranges.NotSupportedGranularity
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.revenueStatsGranularity
import com.woocommerce.android.ui.analytics.ranges.visitorStatsGranularity
import com.woocommerce.android.ui.analytics.ranges.visitorSummaryStatsGranularity
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.ui.dashboard.data.asRevenueRangeId
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCGoogleStore
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import kotlin.math.round

@Suppress("TooManyFunctions")
class AnalyticsRepository @Inject constructor(
    private val statsRepository: StatsRepository,
    private val selectedSite: SelectedSite,
    private val googleAdsStore: WCGoogleStore,
    private val wooCommerceStore: WooCommerceStore
) {
    private val getCurrentRevenueMutex = Mutex()
    private val getPreviousRevenueMutex = Mutex()
    private var revenueStatsCache: MutableMap<String, AnalyticsStatsResultWrapper> = mutableMapOf()

    suspend fun fetchRevenueData(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ): RevenueResult {
        val currentPeriod = getCurrentPeriodStats(rangeSelection, fetchStrategy).getOrNull()
        val previousPeriod = getPreviousPeriodStats(rangeSelection, fetchStrategy).getOrNull()

        val currentPeriodTotalRevenue = currentPeriod?.parseTotal()
        val previousPeriodTotalRevenue = previousPeriod?.parseTotal()

        if (listOf(currentPeriodTotalRevenue, previousPeriodTotalRevenue).any { it == null } ||
            currentPeriodTotalRevenue?.totalSales == null ||
            currentPeriodTotalRevenue.netRevenue == null
        ) {
            return RevenueError
        }

        val previousTotalSales = previousPeriodTotalRevenue?.totalSales ?: 0.0
        val previousNetRevenue = previousPeriodTotalRevenue?.netRevenue ?: 0.0
        val currentTotalSales = currentPeriodTotalRevenue.totalSales!!
        val currentNetRevenue = currentPeriodTotalRevenue.netRevenue!!

        val intervals = currentPeriod.getIntervalList()

        val totalRevenueByInterval = intervals.map {
            it.subtotals?.totalSales ?: 0.0
        }

        val netRevenueByInterval = intervals.map {
            it.subtotals?.netRevenue ?: 0.0
        }

        return RevenueData(
            RevenueStat(
                currentTotalSales,
                calculateDeltaPercentage(previousTotalSales, currentTotalSales),
                currentNetRevenue,
                calculateDeltaPercentage(previousNetRevenue, currentNetRevenue),
                getCurrencyCode(),
                totalRevenueByInterval,
                netRevenueByInterval
            )
        )
    }

    suspend fun fetchOrdersData(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ): OrdersResult {
        val currentPeriod = getCurrentPeriodStats(rangeSelection, fetchStrategy).getOrNull()
        val previousPeriod = getPreviousPeriodStats(rangeSelection, fetchStrategy).getOrNull()

        val currentPeriodTotalRevenue = currentPeriod?.parseTotal()
        val previousPeriodTotalRevenue = previousPeriod?.parseTotal()

        if (listOf(currentPeriodTotalRevenue, previousPeriodTotalRevenue).any { it == null } ||
            currentPeriodTotalRevenue?.ordersCount == null ||
            currentPeriodTotalRevenue.avgOrderValue == null
        ) {
            return OrdersError
        }

        val previousOrdersCount = previousPeriodTotalRevenue?.ordersCount ?: 0
        val previousOrderValue = previousPeriodTotalRevenue?.avgOrderValue ?: 0.0
        val currentOrdersCount = currentPeriodTotalRevenue.ordersCount!!
        val currentAvgOrderValue = currentPeriodTotalRevenue.avgOrderValue!!

        val intervals = currentPeriod.getIntervalList()

        val ordersCountByInterval = intervals.map {
            it.subtotals?.ordersCount ?: 0
        }

        val avgOrderValueByInterval = intervals.map {
            it.subtotals?.avgOrderValue ?: 0.0
        }

        return OrdersResult.OrdersData(
            OrdersStat(
                currentOrdersCount,
                calculateDeltaPercentage(
                    previousOrdersCount.toDouble(),
                    currentOrdersCount.toDouble()
                ),
                currentAvgOrderValue,
                calculateDeltaPercentage(previousOrderValue, currentAvgOrderValue),
                getCurrencyCode(),
                ordersCountByInterval,
                avgOrderValueByInterval
            )
        )
    }

    suspend fun fetchProductsData(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ): ProductsResult {
        val currentPeriod = getCurrentPeriodStats(rangeSelection, fetchStrategy).getOrNull()
        val previousPeriod = getPreviousPeriodStats(rangeSelection, fetchStrategy).getOrNull()
        val currentPeriodTotalRevenue = currentPeriod?.parseTotal()
        val previousPeriodTotalRevenue = previousPeriod?.parseTotal()

        val productsStats = getProductStats(rangeSelection, fetchStrategy, TOP_PRODUCTS_LIST_SIZE).getOrNull()

        if (listOf(currentPeriodTotalRevenue, previousPeriodTotalRevenue, productsStats).any { it == null } ||
            currentPeriodTotalRevenue?.itemsSold == null ||
            previousPeriodTotalRevenue?.itemsSold == null
        ) {
            return ProductsError
        }

        val previousItemsSold = previousPeriodTotalRevenue.itemsSold!!
        val currentItemsSold = currentPeriodTotalRevenue.itemsSold!!
        val productItems = productsStats?.map {
            ProductItem(
                it.name,
                it.total,
                it.imageUrl,
                it.quantity,
                it.currency
            )
        } ?: emptyList()

        return ProductsResult.ProductsData(
            ProductsStat(
                currentItemsSold,
                calculateDeltaPercentage(previousItemsSold.toDouble(), currentItemsSold.toDouble()),
                productItems
            )
        )
    }

    suspend fun fetchVisitorsData(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ): VisitorsResult {
        val result = when (rangeSelection.selectionType) {
            SelectionType.LAST_QUARTER, SelectionType.QUARTER_TO_DATE -> {
                getVisitorsCount(rangeSelection, fetchStrategy)
            }
            else -> {
                getVisitorsSummaryCount(rangeSelection, fetchStrategy)
            }
        }
        return result.fold(
            onFailure = {
                when (it) {
                    is NotSupportedGranularity -> {
                        VisitorsResult.VisitorsNotSupported
                    }

                    else -> {
                        VisitorsResult.VisitorsError
                    }
                }
            },
            onSuccess = { VisitorsResult.VisitorsData(it) }
        )
    }

    private suspend fun getCurrentPeriodStats(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ): Result<WCRevenueStatsModel?> = coroutineScope {
        val currentPeriod = rangeSelection.currentRange
        val startDate = currentPeriod.start.formatToYYYYmmDDhhmmss()
        val endDate = currentPeriod.end.formatToYYYYmmDDhhmmss()
        val statsIdentifier = RevenueRangeId(currentPeriod, rangeSelection.selectionType).id
        val cachedRevenueStat = revenueStatsCache[statsIdentifier]

        getCurrentRevenueMutex.withLock {
            if (cachedRevenueStat.shouldBeUpdated(fetchStrategy)) {
                AnalyticsStatsResultWrapper(
                    startDate = startDate,
                    endDate = endDate,
                    result = async {
                        loadRevenueStats(
                            range = currentPeriod,
                            granularity = rangeSelection.revenueStatsGranularity,
                            revenueRangeId = statsIdentifier,
                            fetchStrategy = fetchStrategy
                        )
                    }
                ).let { revenueStatsCache[statsIdentifier] = it }
            }
        }
        return@coroutineScope revenueStatsCache.getValue(statsIdentifier).result.await()
    }

    private suspend fun getPreviousPeriodStats(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ): Result<WCRevenueStatsModel?> = coroutineScope {
        val previousPeriod = rangeSelection.previousRange
        val startDate = previousPeriod.start.formatToYYYYmmDDhhmmss()
        val endDate = previousPeriod.end.formatToYYYYmmDDhhmmss()
        val statsIdentifier = RevenueRangeId(previousPeriod, rangeSelection.selectionType).id
        val cachedRevenueStat = revenueStatsCache[statsIdentifier]

        getPreviousRevenueMutex.withLock {
            if (cachedRevenueStat.shouldBeUpdated(fetchStrategy)) {
                AnalyticsStatsResultWrapper(
                    startDate = startDate,
                    endDate = endDate,
                    result = async {
                        loadRevenueStats(
                            range = previousPeriod,
                            granularity = rangeSelection.revenueStatsGranularity,
                            revenueRangeId = statsIdentifier,
                            fetchStrategy = fetchStrategy
                        )
                    }
                ).let { revenueStatsCache[statsIdentifier] = it }
            }
        }
        return@coroutineScope revenueStatsCache.getValue(statsIdentifier).result.await()
    }

    private suspend fun getProductStats(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy,
        quantity: Int
    ): Result<List<TopPerformerProductEntity>> {
        val totalPeriod = rangeSelection.currentRange
        val startDate = totalPeriod.start.formatToYYYYmmDDhhmmss()
        val endDate = totalPeriod.end.formatToYYYYmmDDhhmmss()

        return statsRepository.fetchTopPerformerProducts(
            forceRefresh = fetchStrategy is ForceNew,
            startDate = startDate,
            endDate = endDate,
            quantity = quantity
        ).map {
            statsRepository.getTopPerformers(startDate, endDate)
        }
    }

    private suspend fun getVisitorsCount(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ): Result<Int> = statsRepository.fetchVisitorStats(
        range = rangeSelection.currentRange,
        granularity = rangeSelection.visitorStatsGranularity,
        fetchStrategy is ForceNew,
    ).map { it.values.sum() }

    private suspend fun getVisitorsSummaryCount(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ): Result<Int> {
        return try {
            statsRepository.fetchTotalVisitorStats(
                date = rangeSelection.currentRange.end,
                granularity = rangeSelection.visitorSummaryStatsGranularity,
                fetchStrategy is ForceNew,
            )
        } catch (e: NotSupportedGranularity) {
            Result.failure(e)
        }
    }

    private fun calculateDeltaPercentage(previousVal: Double, currentVal: Double): DeltaPercentage = when {
        previousVal <= ZERO_VALUE -> DeltaPercentage.NotExist
        currentVal <= ZERO_VALUE -> DeltaPercentage.Value((MINUS_ONE * ONE_H_PERCENT))
        else -> round((currentVal - previousVal) / previousVal * ONE_H_PERCENT)
            .let { DeltaPercentage.Value(it.toInt()) }
    }

    private fun AnalyticsStatsResultWrapper?.shouldBeUpdated(fetchStrategy: FetchStrategy) =
        this?.let { fetchStrategy == ForceNew && it.result.isCompleted } ?: true

    private suspend fun loadRevenueStats(
        range: StatsTimeRange,
        granularity: StatsGranularity,
        revenueRangeId: String,
        fetchStrategy: FetchStrategy
    ): Result<WCRevenueStatsModel?> {
        if (fetchStrategy == Saved) {
            statsRepository.getRevenueStatsById(revenueRangeId)
                .takeIf { it.isSuccess && it.getOrNull() != null }
                ?.let { return it }
        }

        return statsRepository.fetchRevenueStats(
            range = range,
            granularity = granularity,
            forced = fetchStrategy is ForceNew,
            revenueRangeId = revenueRangeId
        ).mapCatching { it }
    }

    private fun getCurrencyCode() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

    suspend fun fetchProductBundlesStats(rangeSelection: StatsTimeRangeSelection) = coroutineScope {
        val currentPeriod = rangeSelection.currentRange
        val currentStartDate = currentPeriod.start.formatToYYYYmmDDhhmmss()
        val currentEndDate = currentPeriod.end.formatToYYYYmmDDhhmmss()

        val previousPeriod = rangeSelection.previousRange
        val previousStartDate = previousPeriod.start.formatToYYYYmmDDhhmmss()
        val previousEndDate = previousPeriod.end.formatToYYYYmmDDhhmmss()

        val currentBundleStatsCall = async {
            statsRepository.fetchProductBundlesStats(
                startDate = currentStartDate,
                endDate = currentEndDate
            )
        }

        val previousBundleStatsCall = async {
            statsRepository.fetchProductBundlesStats(
                startDate = previousStartDate,
                endDate = previousEndDate
            )
        }

        val bundlesReportCall = async {
            statsRepository.fetchBundleReport(
                startDate = currentStartDate,
                endDate = currentEndDate,
                quantity = TOP_BUNDLES_LIST_SIZE
            )
        }

        val currentBundleStats = currentBundleStatsCall.await().model
        val previousBundleStats = previousBundleStatsCall.await().model
        val bundlesReport = bundlesReportCall.await().model

        if (currentBundleStats == null || previousBundleStats == null || bundlesReport == null) {
            BundlesResult.BundlesError
        } else {
            val delta = calculateDeltaPercentage(
                previousVal = previousBundleStats.itemsSold.toDouble(),
                currentVal = currentBundleStats.itemsSold.toDouble(),
            )
            val bundles = bundlesReport.map { item ->
                BundleItem(
                    netSales = item.netRevenue,
                    name = item.name,
                    image = item.image,
                    quantity = item.itemsSold,
                    currencyCode = getCurrencyCode()
                )
            }
            BundlesResult.BundlesData(
                BundleStat(
                    bundlesSold = currentBundleStats.itemsSold,
                    bundlesSoldDelta = delta,
                    bundles = bundles
                )
            )
        }
    }

    suspend fun fetchGiftCardsStats(rangeSelection: StatsTimeRangeSelection) = coroutineScope {
        val interval = rangeSelection.revenueStatsGranularity.toIntervalString()
        val currentPeriod = rangeSelection.currentRange
        val currentStartDate = currentPeriod.start.formatToYYYYmmDDhhmmss()
        val currentEndDate = currentPeriod.end.formatToYYYYmmDDhhmmss()

        val previousPeriod = rangeSelection.previousRange
        val previousStartDate = previousPeriod.start.formatToYYYYmmDDhhmmss()
        val previousEndDate = previousPeriod.end.formatToYYYYmmDDhhmmss()

        val currentGiftCardsStatsCall = async {
            statsRepository.fetchGiftCardStats(
                startDate = currentStartDate,
                endDate = currentEndDate,
                interval = interval
            )
        }

        val previousGiftCardsStatsCall = async {
            statsRepository.fetchGiftCardStats(
                startDate = previousStartDate,
                endDate = previousEndDate,
                interval = interval
            )
        }

        val currentGiftCardsStats = currentGiftCardsStatsCall.await().model
        val previousGiftCardsStats = previousGiftCardsStatsCall.await().model

        if (currentGiftCardsStats == null || previousGiftCardsStats == null) {
            GiftCardResult.GiftCardError
        } else {
            val deltaNetValue = calculateDeltaPercentage(
                previousVal = previousGiftCardsStats.netValue,
                currentVal = currentGiftCardsStats.netValue,
            )
            val deltaUsed = calculateDeltaPercentage(
                previousVal = previousGiftCardsStats.usedValue.toDouble(),
                currentVal = currentGiftCardsStats.usedValue.toDouble(),
            )

            val usedByInterval = currentGiftCardsStats.intervals.map { it.usedValue }
            val usedByRevenue = currentGiftCardsStats.intervals.map { it.netValue }

            GiftCardResult.GiftCardData(
                GiftCardsStat(
                    usedValue = currentGiftCardsStats.usedValue,
                    usedDelta = deltaUsed,
                    netValue = currentGiftCardsStats.netValue,
                    netDelta = deltaNetValue,
                    currencyCode = getCurrencyCode(),
                    usedByInterval = usedByInterval,
                    netRevenueByInterval = usedByRevenue
                )
            )
        }
    }

    suspend fun fetchGoogleAdsStats(rangeSelection: StatsTimeRangeSelection) = coroutineScope {
        val currentPeriod = rangeSelection.currentRange
        val currentStartDate = currentPeriod.start.formatToYYYYmmDDhhmmss()
        val currentEndDate = currentPeriod.end.formatToYYYYmmDDhhmmss()

        val currentGoogleAdsStatsCall = async {
            googleAdsStore.fetchAllPrograms(
                site = selectedSite.get(),
                startDate = currentStartDate,
                endDate = currentEndDate,
                metricType = WCGoogleStore.MetricType.SALES
            )
        }

        currentGoogleAdsStatsCall.await()
            .model?.campaigns?.let { campaigns ->
                GoogleAdsResult.GoogleAdsData(
                    GoogleAdsStat(
                        googleAdsCampaigns = campaigns.map {
                            GoogleAdsCampaign(it.id ?: 0L)
                        }
                    )
                )
            } ?: GoogleAdsResult.GoogleAdsError
    }

    companion object {
        const val ZERO_VALUE = 0.0
        const val MINUS_ONE = -1
        const val ONE_H_PERCENT = 100

        const val TOP_PRODUCTS_LIST_SIZE = 5
        const val TOP_BUNDLES_LIST_SIZE = 5
    }

    sealed class RevenueResult {
        object RevenueError : RevenueResult()
        data class RevenueData(val revenueStat: RevenueStat) : RevenueResult()
    }

    sealed class OrdersResult {
        object OrdersError : OrdersResult()
        data class OrdersData(val ordersStat: OrdersStat) : OrdersResult()
    }

    sealed class ProductsResult {
        object ProductsError : ProductsResult()
        data class ProductsData(val productsStat: ProductsStat) : ProductsResult()
    }

    sealed class VisitorsResult {
        data object VisitorsNotSupported : VisitorsResult()
        data object VisitorsError : VisitorsResult()
        data class VisitorsData(val visitorsCount: Int) : VisitorsResult()
    }

    sealed class BundlesResult {
        object BundlesError : BundlesResult()
        data class BundlesData(val bundleStat: BundleStat) : BundlesResult()
    }

    sealed class GiftCardResult {
        object GiftCardError : GiftCardResult()
        data class GiftCardData(val giftCardStat: GiftCardsStat) : GiftCardResult()
    }

    sealed class GoogleAdsResult {
        data object GoogleAdsError : GoogleAdsResult()
        data class GoogleAdsData(val googleAdsStat: GoogleAdsStat) : GoogleAdsResult()
    }

    sealed class FetchStrategy {
        object ForceNew : FetchStrategy()
        object Saved : FetchStrategy()
    }

    private data class AnalyticsStatsResultWrapper(
        val startDate: String,
        val endDate: String,
        val result: Deferred<Result<WCRevenueStatsModel?>>
    )

    private data class RevenueRangeId(
        private val timeRange: StatsTimeRange,
        private val selectionType: SelectionType
    ) {
        val id: String = selectionType.identifier.asRevenueRangeId(timeRange.start, timeRange.end)
    }
}

fun StatsGranularity.toIntervalString(): String {
    return when (this) {
        StatsGranularity.HOURS -> "hour"
        StatsGranularity.DAYS -> "day"
        StatsGranularity.WEEKS -> "week"
        StatsGranularity.MONTHS -> "month"
        StatsGranularity.YEARS -> "year"
    }
}
