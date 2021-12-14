package com.woocommerce.android.ui.analytics

import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticTimePeriod
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.*
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
    suspend fun fetchRevenueData(
        dateRange: AnalyticsDateRange,
        selectedRange: AnalyticTimePeriod
    ): Flow<RevenueResult> =
        getGranularity(selectedRange).let {
            return getCurrentPeriodRevenue(dateRange, it)
                .combine(getPreviousPeriodRevenue(dateRange, it)) { currentPeriodRevenue, previousPeriodRevenue ->
                    if (currentPeriodRevenue.isFailure || currentPeriodRevenue.getOrNull() == null) {
                        return@combine RevenueError
                    }
                    if (previousPeriodRevenue.isFailure || previousPeriodRevenue.getOrNull() == null) {
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

    fun getRevenueAdminPanelUrl() = getAdminPanelUrl() + ANALYTICS_REVENUE_PATH

    private suspend fun getCurrentPeriodRevenue(dateRange: AnalyticsDateRange, granularity: StatsGranularity) =
        when (dateRange) {
            is SimpleDateRange ->
                fetchRevenueStats(dateRange.to.formatToYYYYmmDD(), dateRange.to.formatToYYYYmmDD(), granularity)
            is MultipleDateRange ->
                fetchRevenueStats(
                    dateRange.to.from.formatToYYYYmmDD(), dateRange.to.to.formatToYYYYmmDD(),
                    granularity
                )
        }

    private suspend fun getPreviousPeriodRevenue(dateRange: AnalyticsDateRange, granularity: StatsGranularity) =
        when (dateRange) {
            is SimpleDateRange ->
                fetchRevenueStats(dateRange.from.formatToYYYYmmDD(), dateRange.from.formatToYYYYmmDD(), granularity)
            is MultipleDateRange ->
                fetchRevenueStats(
                    dateRange.from.from.formatToYYYYmmDD(), dateRange.from.to.formatToYYYYmmDD(),
                    granularity
                )
        }

    private fun getGranularity(selectedRange: AnalyticTimePeriod) =
        when (selectedRange) {
            AnalyticTimePeriod.TODAY, AnalyticTimePeriod.YESTERDAY -> DAYS
            AnalyticTimePeriod.LAST_WEEK, AnalyticTimePeriod.WEEK_TO_DATE -> WEEKS
            AnalyticTimePeriod.LAST_MONTH, AnalyticTimePeriod.MONTH_TO_DATE -> MONTHS
            AnalyticTimePeriod.LAST_QUARTER, AnalyticTimePeriod.QUARTER_TO_DATE -> MONTHS
            AnalyticTimePeriod.LAST_YEAR, AnalyticTimePeriod.YEAR_TO_DATE -> YEARS
        }

    private fun calculateDeltaPercentage(previousVal: Double, currentVal: Double) = when {
        previousVal <= ZERO_VALUE -> round(currentVal * ONE_H_PERCENT).toInt()
        currentVal <= ZERO_VALUE -> round(MINUS_ONE * previousVal * ONE_H_PERCENT).toInt()
        else -> (round((previousVal - currentVal) / currentVal) * ONE_H_PERCENT).toInt()
    }

    private suspend fun fetchRevenueStats(startDate: String, endDate: String, granularity: StatsGranularity) =
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
}
