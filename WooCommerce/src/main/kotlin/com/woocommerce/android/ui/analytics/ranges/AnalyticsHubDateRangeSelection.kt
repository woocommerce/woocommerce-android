package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.R
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_MONTH
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_QUARTER
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_WEEK
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_YEAR
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.QUARTER_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.YEAR_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.YESTERDAY
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubLastMonthRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubLastQuarterRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubLastWeekRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubLastYearRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubMonthToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubQuarterToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubTodayRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubWeekToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubYearToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubYesterdayRangeData
import com.woocommerce.android.viewmodel.ResourceProvider
import java.util.Calendar
import java.util.Date

class AnalyticsHubDateRangeSelection(
    selectionType: SelectionType,
    resourceProvider: ResourceProvider,
    currentDate: Date = Date(),
    calendar: Calendar = Calendar.getInstance()
) {
    val currentRange: AnalyticsHubTimeRange?
    val previousRange: AnalyticsHubTimeRange?
    val description: String

    init {
        val rangeData = generateTimeRangeData(selectionType, currentDate, calendar)
        currentRange = rangeData.currentRange
        previousRange = rangeData.previousRange
        description = generateRangeDescription(selectionType, resourceProvider)
    }

    private fun generateTimeRangeData(
        selectionType: SelectionType,
        referenceDate: Date,
        calendar: Calendar
    ): AnalyticsHubTimeRangeData {
        return when (selectionType) {
            TODAY -> AnalyticsHubTodayRangeData(referenceDate, calendar)
            YESTERDAY -> AnalyticsHubYesterdayRangeData(referenceDate, calendar)
            WEEK_TO_DATE -> AnalyticsHubWeekToDateRangeData(referenceDate, calendar)
            LAST_WEEK -> AnalyticsHubLastWeekRangeData(referenceDate, calendar)
            MONTH_TO_DATE -> AnalyticsHubMonthToDateRangeData(referenceDate, calendar)
            LAST_MONTH -> AnalyticsHubLastMonthRangeData(referenceDate, calendar)
            QUARTER_TO_DATE -> AnalyticsHubQuarterToDateRangeData(referenceDate, calendar)
            LAST_QUARTER -> AnalyticsHubLastQuarterRangeData(referenceDate, calendar)
            YEAR_TO_DATE -> AnalyticsHubYearToDateRangeData(referenceDate, calendar)
            LAST_YEAR -> AnalyticsHubLastYearRangeData(referenceDate, calendar)
            // TODO: support custom range
            else -> AnalyticsHubTodayRangeData(referenceDate, calendar)
        }
    }

    private fun generateRangeDescription(
        selectionType: SelectionType,
        resourceProvider: ResourceProvider
    ) = when (selectionType) {
        TODAY -> resourceProvider.getString(R.string.date_timeframe_today)
        YESTERDAY -> resourceProvider.getString(R.string.date_timeframe_yesterday)
        LAST_WEEK -> resourceProvider.getString(R.string.date_timeframe_last_week)
        LAST_MONTH -> resourceProvider.getString(R.string.date_timeframe_last_month)
        LAST_QUARTER -> resourceProvider.getString(R.string.date_timeframe_last_quarter)
        LAST_YEAR -> resourceProvider.getString(R.string.date_timeframe_last_year)
        WEEK_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_week_to_date)
        MONTH_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_month_to_date)
        QUARTER_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_quarter_to_date)
        YEAR_TO_DATE -> resourceProvider.getString(R.string.date_timeframe_year_to_date)
        CUSTOM -> resourceProvider.getString(R.string.date_timeframe_custom)
    }

    enum class SelectionType {
        TODAY,
        YESTERDAY,
        LAST_WEEK,
        LAST_MONTH,
        LAST_QUARTER,
        LAST_YEAR,
        WEEK_TO_DATE,
        MONTH_TO_DATE,
        QUARTER_TO_DATE,
        YEAR_TO_DATE,
        CUSTOM;
    }
}
