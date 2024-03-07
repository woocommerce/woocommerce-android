package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.model.DeltaPercentage
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
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.YEAR_TO_DATE
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.ui.mystore.data.asRevenueRangeId
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.YEARS
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import kotlin.math.round

@Suppress("TooManyFunctions")
class AnalyticsRepository @Inject constructor(
    private val statsRepository: StatsRepository,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val dispatchers: CoroutineDispatchers,
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
        return getVisitorsCount(rangeSelection, fetchStrategy)
            .fold(
                onFailure = { VisitorsResult.VisitorsError },
                onSuccess = { VisitorsResult.VisitorsData(it.values.sum()) }
            )
    }

    private suspend fun getCurrentPeriodStats(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ): Result<WCRevenueStatsModel?> = coroutineScope {
        val granularity = getGranularity(rangeSelection.selectionType)
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
                    result = async { loadRevenueStats(currentPeriod, granularity, statsIdentifier, fetchStrategy) }
                ).let { revenueStatsCache[statsIdentifier] = it }
            }
        }
        return@coroutineScope revenueStatsCache.getValue(statsIdentifier).result.await()
    }

    private suspend fun getPreviousPeriodStats(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ): Result<WCRevenueStatsModel?> = coroutineScope {
        val granularity = getGranularity(rangeSelection.selectionType)
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
                    result = async { loadRevenueStats(previousPeriod, granularity, statsIdentifier, fetchStrategy) }
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
    ): Result<Map<String, Int>> = coroutineScope {
        statsRepository.fetchVisitorStats(
            range = rangeSelection.currentRange,
            granularity = getGranularity(rangeSelection.selectionType),
            fetchStrategy is ForceNew,
        ).single()
    }

    private fun getGranularity(selectionType: SelectionType) =
        when (selectionType) {
            TODAY, SelectionType.YESTERDAY -> DAYS
            SelectionType.LAST_WEEK, WEEK_TO_DATE -> WEEKS
            SelectionType.LAST_MONTH, MONTH_TO_DATE -> MONTHS
            SelectionType.LAST_QUARTER, SelectionType.QUARTER_TO_DATE -> MONTHS
            SelectionType.LAST_YEAR, YEAR_TO_DATE -> YEARS
            SelectionType.CUSTOM -> DAYS
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
        range: AnalyticsHubTimeRange,
        granularity: StatsGranularity,
        revenueRangeId: String,
        fetchStrategy: FetchStrategy
    ): Result<WCRevenueStatsModel?> {
        if (fetchStrategy == Saved) {
            statsRepository.getRevenueStatsById(revenueRangeId)
                .flowOn(dispatchers.io).single()
                .takeIf { it.isSuccess && it.getOrNull() != null }
                ?.let { return it }
        }

        return statsRepository.fetchRevenueStats(
            range = range,
            granularity = granularity,
            forced = fetchStrategy is ForceNew,
            revenueRangeId = revenueRangeId
        ).flowOn(dispatchers.io).single().mapCatching { it }
    }

    private fun getCurrencyCode() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

    companion object {
        const val ZERO_VALUE = 0.0
        const val MINUS_ONE = -1
        const val ONE_H_PERCENT = 100

        const val TOP_PRODUCTS_LIST_SIZE = 5
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
        object VisitorsError : VisitorsResult()
        data class VisitorsData(val visitorsCount: Int) : VisitorsResult()
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
        private val timeRange: AnalyticsHubTimeRange,
        private val selectionType: SelectionType
    ) {
        val id: String = selectionType.identifier.asRevenueRangeId(timeRange.start, timeRange.end)
    }
}
