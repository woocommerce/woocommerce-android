package com.woocommerce.android.ui.analytics

import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.model.AnalyticStat.RevenueStat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRanges
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange
import com.woocommerce.android.ui.mystore.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
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

    suspend fun fetchRevenueStatData(dateRange: DateRange, selectedRange: AnalyticsDateRanges): RevenueStat? {
        val granularity = getGranularity(selectedRange)
        val previousPeriodRevenue = getPreviousPeriodRevenue(dateRange, granularity)
        val currentPeriodRevenue = getCurrentPeriodRevenue(dateRange, granularity)

        if (previousPeriodRevenue == null || currentPeriodRevenue == null ||
            currentPeriodRevenue.totalSales == null || currentPeriodRevenue.netRevenue == null) {
            return null
        }

        val previousTotalSales = if (previousPeriodRevenue.totalSales != null)
            previousPeriodRevenue.totalSales!!
        else
            0.0

        val previousNetRevenue = if (previousPeriodRevenue.netRevenue != null)
            previousPeriodRevenue.netRevenue!!
        else
            0.0

        return RevenueStat(
            currentPeriodRevenue.totalSales!!,
            calculateDeltaPercentage(currentPeriodRevenue.totalSales!!, previousTotalSales),
            currentPeriodRevenue.netRevenue!!,
            calculateDeltaPercentage(currentPeriodRevenue.netRevenue!!, previousNetRevenue),
            getCurrencyCode()
        )
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

    private fun calculateDeltaPercentage(valueX: Double, valueY: Double): Int = if (valueY <= 0.0)
        round(valueX * 100).toInt()
    else
        round((valueX - valueY) / valueY).toInt()

    private suspend fun fetchRevenueStats(startDate: String, endDate: String, granularity: StatsGranularity) =
        withContext(Dispatchers.IO) {
            statsRepository.fetchRevenueStats(granularity, true, startDate, endDate)
                .fold({ it?.parseTotal() }, { null })
        }

    private fun getCurrencyCode() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

}
