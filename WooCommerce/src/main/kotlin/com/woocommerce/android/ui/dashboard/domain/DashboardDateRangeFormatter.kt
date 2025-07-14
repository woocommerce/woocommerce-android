package com.woocommerce.android.ui.dashboard.domain

import android.icu.text.SimpleDateFormat
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.formatToMMMMyyyy
import com.woocommerce.android.extensions.formatToYYYY
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.data.asRevenueRangeId
import com.woocommerce.android.util.DateUtils
import javax.inject.Inject

class DashboardDateRangeFormatter @Inject constructor(private val dateUtils: DateUtils) {
    fun formatRangeDate(rangeSelection: StatsTimeRangeSelection): String {
        val startDate = rangeSelection.currentRange.start
        val endDate = rangeSelection.currentRange.end
        val dateFormatter = SimpleDateFormat.getDateInstance()

        return when (rangeSelection.selectionType) {
            SelectionType.TODAY -> dateFormatter.format(startDate)
            SelectionType.MONTH_TO_DATE -> startDate.formatToMMMMyyyy()
            SelectionType.YEAR_TO_DATE -> startDate.formatToYYYY()
            SelectionType.WEEK_TO_DATE, SelectionType.CUSTOM ->
                "${dateFormatter.format(startDate)} – ${dateFormatter.format(endDate)}"

            else -> error("Unsupported range value used in Dashboard performance card: ${rangeSelection.selectionType}")
        }
    }

    fun formatSelectedDate(dateString: String, rangeSelection: StatsTimeRangeSelection): String? {
        return when (rangeSelection.selectionType) {
            SelectionType.TODAY -> dateUtils.getFriendlyDayHourString(dateString).orEmpty()
            SelectionType.WEEK_TO_DATE -> dateUtils.getShortMonthDayString(dateString).orEmpty()
            SelectionType.MONTH_TO_DATE -> dateUtils.getLongMonthDayString(dateString).orEmpty()
            SelectionType.YEAR_TO_DATE -> dateUtils.getFriendlyLongMonthYear(dateString).orEmpty()
            // For custom ranges, we don't display the selected date
            SelectionType.CUSTOM -> null

            else -> error("Unsupported range value used in dashboard card: ${rangeSelection.selectionType}")
        }?.also { result -> trackUnexpectedFormat(result, dateString, rangeSelection) }
    }

    private fun trackUnexpectedFormat(result: String, dateString: String, rangeSelection: StatsTimeRangeSelection) {
        if (result.isEmpty()) {
            val rangeId = rangeSelection.selectionType.identifier.asRevenueRangeId(
                startDate = rangeSelection.currentRange.start,
                endDate = rangeSelection.currentRange.end
            )
            AnalyticsTracker.track(
                AnalyticsEvent.STATS_UNEXPECTED_FORMAT,
                mapOf(
                    AnalyticsTracker.KEY_DATE to dateString,
                    AnalyticsTracker.KEY_GRANULARITY to rangeSelection.selectionType,
                    AnalyticsTracker.KEY_RANGE to rangeId
                )
            )
        }
    }
}
