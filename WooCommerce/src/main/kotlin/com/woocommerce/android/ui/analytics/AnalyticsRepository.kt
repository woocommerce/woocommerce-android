package com.woocommerce.android.ui.analytics

import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.model.AnalyticStat.RevenueStat
import com.woocommerce.android.tools.SelectedSite
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
    private val wooCommerceStore: WooCommerceStore
) {

    suspend fun fetchRevenueData(dateRange: DateRange, selectedRange: AnalyticsDateRanges): Flow<RevenueResult> =
        getGranularity(selectedRange).let {
            return getCurrentPeriodRevenue(dateRange, it)
                .combine(getPreviousPeriodRevenue(dateRange, it)) { currentPeriodRevenue, previousPeriodRevenue ->
                    if (currentPeriodRevenue.isFailure || currentPeriodRevenue.getOrNull() == null ||
                        currentPeriodRevenue.getOrNull()!!.parseTotal() == null)
                        return@combine RevenueError

                    val previousTotalSales = previousPeriodRevenue.getOrNull()?.parseTotal()?.totalSales ?: 0.0
                    val previousNetRevenue = previousPeriodRevenue.getOrNull()?.parseTotal()?.netRevenue ?: 0.0
                    val currentTotalSales = currentPeriodRevenue.getOrNull()!!.parseTotal()?.totalSales!!
                    val currentNetRevenue = currentPeriodRevenue.getOrNull()!!.parseTotal()?.netRevenue!!

                    return@combine RevenueData(
                        RevenueStat(
                            currentTotalSales,
                            calculateDeltaPercentage(previousTotalSales, currentTotalSales),
                            currentNetRevenue,
                            calculateDeltaPercentage(previousNetRevenue, currentNetRevenue),
                            getCurrencyCode()
                        ))
                }
        }

    private suspend fun getCurrentPeriodRevenue(dateRange: DateRange, granularity: StatsGranularity) =
        when (dateRange) {
            is DateRange.SimpleDateRange ->
                fetchRevenueStats(dateRange.to.formatToYYYYmmDD(), dateRange.to.formatToYYYYmmDD(), granularity)
            is DateRange.MultipleDateRange ->
                fetchRevenueStats(dateRange.to.from.formatToYYYYmmDD(), dateRange.to.to.formatToYYYYmmDD(), granularity)
        }

    private suspend fun getPreviousPeriodRevenue(dateRange: DateRange, granularity: StatsGranularity) =
        when (dateRange) {
            is DateRange.SimpleDateRange ->
                fetchRevenueStats(dateRange.from.formatToYYYYmmDD(), dateRange.from.formatToYYYYmmDD(), granularity)
            is DateRange.MultipleDateRange ->
                fetchRevenueStats(dateRange.from.from.formatToYYYYmmDD(), dateRange.from.to.formatToYYYYmmDD(), granularity)
        }

    private fun getGranularity(selectedRange: AnalyticsDateRanges) =
        when (selectedRange) {
            AnalyticsDateRanges.TODAY, AnalyticsDateRanges.YESTERDAY -> DAYS
            AnalyticsDateRanges.LAST_WEEK, AnalyticsDateRanges.WEEK_TO_DATE -> WEEKS
            AnalyticsDateRanges.LAST_MONTH, AnalyticsDateRanges.MONTH_TO_DATE -> MONTHS
            AnalyticsDateRanges.LAST_QUARTER, AnalyticsDateRanges.QUARTER_TO_DATE -> MONTHS
            AnalyticsDateRanges.LAST_YEAR, AnalyticsDateRanges.YEAR_TO_DATE -> YEARS
        }

    private fun calculateDeltaPercentage(previousVal: Double, currentVal: Double) = when {
        previousVal <= 0.0 -> round(currentVal * 100).toInt()
        currentVal <= 0.0 -> round(-1 * previousVal * 100).toInt()
        else -> (round((previousVal - currentVal) / currentVal) * 100).toInt()
    }

    private suspend fun fetchRevenueStats(startDate: String, endDate: String, granularity: StatsGranularity) =
        withContext(Dispatchers.IO) {
            statsRepository.fetchRevenueStats(granularity, true, startDate, endDate)
        }

    private fun getCurrencyCode() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

    sealed class RevenueResult {
        object RevenueError : RevenueResult()
        data class RevenueData(val revenueStat: RevenueStat) : RevenueResult()
    }

}
