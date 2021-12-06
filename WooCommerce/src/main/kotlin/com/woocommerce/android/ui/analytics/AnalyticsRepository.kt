package com.woocommerce.android.ui.analytics

import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRanges
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange
import com.woocommerce.android.ui.mystore.data.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.*
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import kotlin.math.round

class AnalyticsRepository @Inject constructor(
    private val statsRepository: StatsRepository,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
) {

    suspend fun fetchRevenueData(dateRange: DateRange, selectedRange: AnalyticsDateRanges): Flow<RevenueResult> =
        getGranularity(selectedRange).let {
            return getCurrentPeriodStats(dateRange, it)
                .combine(getPreviousPeriodStats(dateRange, it)) { currentPeriodRevenue, previousPeriodRevenue ->

                    if (currentPeriodRevenue.isFailure || currentPeriodRevenue.getOrNull() == null ||
                        previousPeriodRevenue.isFailure || previousPeriodRevenue.getOrNull() == null) {
                        return@combine RevenueError
                    }

                    val previousTotalSales = previousPeriodRevenue.getOrNull()!!.parseTotal()?.totalSales ?: 0.0
                    val previousNetRevenue = previousPeriodRevenue.getOrNull()!!.parseTotal()?.netRevenue ?: 0.0
                    val currentTotalSales = currentPeriodRevenue.getOrNull()!!.parseTotal()?.totalSales!!
                    val currentNetRevenue = currentPeriodRevenue.getOrNull()!!.parseTotal()?.netRevenue!!

                    return@combine RevenueData(
                        RevenueStat(
                            currentTotalSales,
                            calculateDeltaPercentage(previousTotalSales, currentTotalSales),
                            currentNetRevenue,
                            calculateDeltaPercentage(previousNetRevenue, currentNetRevenue),
                            getCurrencyCode()
                        )
                    )
                }
        }

    suspend fun fetchOrdersData(dateRange: DateRange, selectedRange: AnalyticsDateRanges): Flow<OrdersResult> =
        getGranularity(selectedRange).let {
            return getCurrentPeriodStats(dateRange, it)
                .combine(getPreviousPeriodStats(dateRange, it)) { currentPeriodRevenue, previousPeriodRevenue ->
                    if (currentPeriodRevenue.isFailure || currentPeriodRevenue.getOrNull() == null)
                        return@combine OrdersError

                    val previousOrdersCount = previousPeriodRevenue.getOrNull()?.parseTotal()?.ordersCount ?: 0
                    val previousOrderValue = previousPeriodRevenue.getOrNull()?.parseTotal()?.avgOrderValue ?: 0.0
                    val currentOrdersCount = currentPeriodRevenue.getOrNull()!!.parseTotal()?.ordersCount!!
                    val currentAvgOrderValue = currentPeriodRevenue.getOrNull()!!.parseTotal()?.avgOrderValue!!

                    return@combine OrdersResult.OrdersData(
                        OrdersStat(
                            currentOrdersCount,
                            calculateDeltaPercentage(previousOrdersCount.toDouble(), currentOrdersCount.toDouble()),
                            currentAvgOrderValue,
                            calculateDeltaPercentage(previousOrderValue, currentAvgOrderValue),
                            getCurrencyCode()
                        ))
                }
        }

    fun getRevenueAdminPanelUrl() = getAdminPanelUrl() + ANALYTICS_REVENUE_PATH

    private suspend fun getCurrentPeriodStats(dateRange: DateRange, granularity: StatsGranularity) = when (dateRange) {
        is DateRange.SimpleDateRange ->
            fetchStats(dateRange.to.formatToYYYYmmDD(), dateRange.to.formatToYYYYmmDD(), granularity)
        is DateRange.MultipleDateRange ->
            fetchStats(dateRange.to.from.formatToYYYYmmDD(), dateRange.to.to.formatToYYYYmmDD(), granularity)
    }

    private suspend fun getPreviousPeriodStats(dateRange: DateRange, granularity: StatsGranularity) = when (dateRange) {
        is DateRange.SimpleDateRange ->
            fetchStats(dateRange.from.formatToYYYYmmDD(), dateRange.from.formatToYYYYmmDD(), granularity)
        is DateRange.MultipleDateRange ->
            fetchStats(dateRange.from.from.formatToYYYYmmDD(), dateRange.from.to.formatToYYYYmmDD(), granularity)
    }

    private fun getGranularity(selectedRange: AnalyticsDateRanges) = when (selectedRange) {
        AnalyticsDateRanges.TODAY, AnalyticsDateRanges.YESTERDAY -> DAYS
        AnalyticsDateRanges.LAST_WEEK, AnalyticsDateRanges.WEEK_TO_DATE -> WEEKS
        AnalyticsDateRanges.LAST_MONTH, AnalyticsDateRanges.MONTH_TO_DATE -> MONTHS
        AnalyticsDateRanges.LAST_QUARTER, AnalyticsDateRanges.QUARTER_TO_DATE -> MONTHS
        AnalyticsDateRanges.LAST_YEAR, AnalyticsDateRanges.YEAR_TO_DATE -> YEARS
    }

    private fun calculateDeltaPercentage(previousVal: Double, currentVal: Double) = when {
        previousVal <= ZERO_VALUE -> round(currentVal * ONE_H_PERCENT).toInt()
        currentVal <= ZERO_VALUE -> round(MINUS_ONE * previousVal * ONE_H_PERCENT).toInt()
        else -> (round((previousVal - currentVal) / currentVal) * ONE_H_PERCENT).toInt()
    }

    private suspend fun fetchStats(startDate: String, endDate: String, granularity: StatsGranularity) =
        withContext(Dispatchers.IO) {
            statsRepository.fetchRevenueStats(granularity, true, startDate, endDate)
        }

    private fun getCurrencyCode() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    private fun getAdminPanelUrl() = selectedSite.getIfExists()?.adminUrl

    companion object {
        const val ANALYTICS_REVENUE_PATH = "admin.php?page=wc-admin&path=%2Fanalytics%2Frevenue"
        const val ZERO_VALUE = 0.0
        const val MINUS_ONE = -1
        const val ONE_H_PERCENT = 100
    }

    sealed class RevenueResult {
        object RevenueError : RevenueResult()
        data class RevenueData(val revenueStat: RevenueStat) : RevenueResult()
    }

    sealed class OrdersResult {
        object OrdersError : OrdersResult()
        data class OrdersData(val ordersState: OrdersStat) : OrdersResult()
    }
}
