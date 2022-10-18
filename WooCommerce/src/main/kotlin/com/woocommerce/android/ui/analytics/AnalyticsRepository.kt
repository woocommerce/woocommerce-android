package com.woocommerce.android.ui.analytics

import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.model.*
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.ProductsResult.ProductsError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.*
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.*
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import kotlin.math.round
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity

class AnalyticsRepository @Inject constructor(
    private val statsRepository: StatsRepository,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val dispatchers: CoroutineDispatchers,
) {
    private val getCurrentRevenueMutex = Mutex()
    private var currentRevenueStats: AnalyticsStatsResultWrapper? = null

    private val getPreviousRevenueMutex = Mutex()
    private var previousRevenueStats: AnalyticsStatsResultWrapper? = null

    suspend fun fetchRevenueData(
        dateRange: AnalyticsDateRange,
        selectedRange: AnalyticTimePeriod,
        fetchStrategy: FetchStrategy
    ): RevenueResult {
        val granularity = getGranularity(selectedRange)
        val currentPeriodTotalRevenue = getCurrentPeriodStats(dateRange, granularity, fetchStrategy).getOrNull()
        val previousPeriodTotalRevenue = getPreviousPeriodStats(dateRange, granularity, fetchStrategy).getOrNull()

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

        return RevenueData(
            RevenueStat(
                currentTotalSales,
                calculateDeltaPercentage(previousTotalSales, currentTotalSales),
                currentNetRevenue,
                calculateDeltaPercentage(previousNetRevenue, currentNetRevenue),
                getCurrencyCode()
            )
        )
    }

    suspend fun fetchOrdersData(
        dateRange: AnalyticsDateRange,
        selectedRange: AnalyticTimePeriod,
        fetchStrategy: FetchStrategy
    ): OrdersResult {
        val granularity = getGranularity(selectedRange)
        val currentPeriodTotalRevenue = getCurrentPeriodStats(dateRange, granularity, fetchStrategy).getOrNull()
        val previousPeriodTotalRevenue = getPreviousPeriodStats(dateRange, granularity, fetchStrategy).getOrNull()

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

        return OrdersResult.OrdersData(
            OrdersStat(
                currentOrdersCount,
                calculateDeltaPercentage(previousOrdersCount.toDouble(), currentOrdersCount.toDouble()),
                currentAvgOrderValue,
                calculateDeltaPercentage(previousOrderValue, currentAvgOrderValue),
                getCurrencyCode()
            )
        )
    }

    suspend fun fetchProductsData(
        dateRange: AnalyticsDateRange,
        selectedRange: AnalyticTimePeriod,
        fetchStrategy: FetchStrategy
    ): ProductsResult {
        val granularity = getGranularity(selectedRange)

        val currentPeriodTotalRevenue = getCurrentPeriodStats(dateRange, granularity, fetchStrategy).getOrNull()
        val previousPeriodTotalRevenue = getPreviousPeriodStats(dateRange, granularity, fetchStrategy).getOrNull()
        val productsStats = getProductStats(dateRange, granularity, TOP_PRODUCTS_LIST_SIZE).getOrNull()

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

    fun getRevenueAdminPanelUrl() = getAdminPanelUrl() + ANALYTICS_REVENUE_PATH
    fun getOrdersAdminPanelUrl() = getAdminPanelUrl() + ANALYTICS_ORDERS_PATH
    fun getProductsAdminPanelUrl() = getAdminPanelUrl() + ANALYTICS_PRODUCTS_PATH

    private suspend fun getCurrentPeriodStats(
        dateRange: AnalyticsDateRange,
        granularity: StatsGranularity,
        fetchStrategy: FetchStrategy
    ): Result<WCRevenueStatsModel.Total> = coroutineScope {
        val startDate = when (dateRange) {
            is SimpleDateRange -> dateRange.to.formatToYYYYmmDD()
            is MultipleDateRange -> dateRange.to.from.formatToYYYYmmDD()
        }
        val endDate = when (dateRange) {
            is SimpleDateRange -> dateRange.to.formatToYYYYmmDD()
            is MultipleDateRange -> dateRange.to.to.formatToYYYYmmDD()
        }

        getCurrentRevenueMutex.withLock {
            if (shouldUpdateCurrentStats(startDate, endDate, fetchStrategy == FetchStrategy.ForceNew)) {
                currentRevenueStats =
                    AnalyticsStatsResultWrapper(
                        startDate = startDate,
                        endDate = endDate,
                        result = async { fetchNetworkStats(startDate, endDate, granularity, fetchStrategy) }
                    )
            }
        }
        return@coroutineScope currentRevenueStats!!.result.await()
    }

    private suspend fun getPreviousPeriodStats(
        dateRange: AnalyticsDateRange,
        granularity: StatsGranularity,
        fetchStrategy: FetchStrategy
    ): Result<WCRevenueStatsModel.Total> = coroutineScope {
        val startDate = when (dateRange) {
            is SimpleDateRange -> dateRange.from.formatToYYYYmmDD()
            is MultipleDateRange -> dateRange.from.from.formatToYYYYmmDD()
        }
        val endDate = when (dateRange) {
            is SimpleDateRange -> dateRange.from.formatToYYYYmmDD()
            is MultipleDateRange -> dateRange.from.to.formatToYYYYmmDD()
        }

        getPreviousRevenueMutex.withLock {
            if (shouldUpdatePreviousStats(startDate, endDate, fetchStrategy == FetchStrategy.ForceNew)) {
                previousRevenueStats =
                    AnalyticsStatsResultWrapper(
                        startDate = startDate,
                        endDate = endDate,
                        result = async { fetchNetworkStats(startDate, endDate, granularity, fetchStrategy) }
                    )
            }
        }
        return@coroutineScope previousRevenueStats!!.result.await()
    }

    private suspend fun getProductStats(
        dateRange: AnalyticsDateRange,
        granularity: StatsGranularity,
        quantity: Int
    ): Result<List<TopPerformerProductEntity>> {
        val startDate = when (dateRange) {
            is SimpleDateRange -> dateRange.to.formatToYYYYmmDD()
            is MultipleDateRange -> dateRange.to.from.formatToYYYYmmDD()
        }
        val endDate = when (dateRange) {
            is SimpleDateRange -> dateRange.to.formatToYYYYmmDD()
            is MultipleDateRange -> dateRange.to.to.formatToYYYYmmDD()
        }

        return fetchProductLeaderboards(startDate, endDate, granularity, quantity)
    }

    private fun getGranularity(selectedRange: AnalyticTimePeriod) =
        when (selectedRange) {
            AnalyticTimePeriod.TODAY, AnalyticTimePeriod.YESTERDAY -> DAYS
            AnalyticTimePeriod.LAST_WEEK, AnalyticTimePeriod.WEEK_TO_DATE -> WEEKS
            AnalyticTimePeriod.LAST_MONTH, AnalyticTimePeriod.MONTH_TO_DATE -> MONTHS
            AnalyticTimePeriod.LAST_QUARTER, AnalyticTimePeriod.QUARTER_TO_DATE -> MONTHS
            AnalyticTimePeriod.LAST_YEAR, AnalyticTimePeriod.YEAR_TO_DATE -> YEARS
            AnalyticTimePeriod.CUSTOM, AnalyticTimePeriod.CUSTOM -> AnalyticTimePeriod.CUSTOM
        }

    private fun calculateDeltaPercentage(previousVal: Double, currentVal: Double): DeltaPercentage = when {
        previousVal <= ZERO_VALUE -> DeltaPercentage.NotExist
        currentVal <= ZERO_VALUE -> DeltaPercentage.Value((MINUS_ONE * ONE_H_PERCENT))
        else -> DeltaPercentage.Value((round((currentVal - previousVal) / previousVal) * ONE_H_PERCENT).toInt())
    }

    private fun shouldUpdatePreviousStats(startDate: String, endDate: String, forceUpdate: Boolean) =
        previousRevenueStats?.startDate != startDate || previousRevenueStats?.endDate != endDate ||
            (forceUpdate && previousRevenueStats?.result?.isCompleted == true)

    private fun shouldUpdateCurrentStats(startDate: String, endDate: String, forceUpdate: Boolean) =
        currentRevenueStats?.startDate != startDate || currentRevenueStats?.endDate != endDate ||
            (forceUpdate && currentRevenueStats?.result?.isCompleted == true)

    private suspend fun fetchNetworkStats(
        startDate: String,
        endDate: String,
        granularity: StatsGranularity,
        fetchStrategy: FetchStrategy
    ): Result<WCRevenueStatsModel.Total> =
        statsRepository.fetchRevenueStats(
            granularity,
            fetchStrategy is FetchStrategy.ForceNew,
            startDate,
            endDate
        ).flowOn(dispatchers.io).single().mapCatching { it!!.parseTotal()!! }

    private suspend fun fetchProductLeaderboards(
        startDate: String,
        endDate: String,
        granularity: StatsGranularity,
        quantity: Int,
    ): Result<List<TopPerformerProductEntity>> = withContext(dispatchers.io) {
        statsRepository.fetchProductLeaderboards(
            true,
            granularity,
            quantity,
//            startDate,
//            endDate
        ).flowOn(dispatchers.io).single()
    }

    private fun getCurrencyCode() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    private fun getAdminPanelUrl() = selectedSite.getIfExists()?.adminUrl

    companion object {
        const val ANALYTICS_REVENUE_PATH = "admin.php?page=wc-admin&path=%2Fanalytics%2Frevenue"
        const val ANALYTICS_ORDERS_PATH = "admin.php?page=wc-admin&path=%2Fanalytics%2Forders"
        const val ANALYTICS_PRODUCTS_PATH = "admin.php?page=wc-admin&path=%2Fanalytics%2Fproducts"

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

    sealed class FetchStrategy {
        object ForceNew : FetchStrategy()
        object Saved : FetchStrategy()
    }

    private data class AnalyticsStatsResultWrapper(
        val startDate: String,
        val endDate: String,
        val result: Deferred<Result<WCRevenueStatsModel.Total>>
    )
}
