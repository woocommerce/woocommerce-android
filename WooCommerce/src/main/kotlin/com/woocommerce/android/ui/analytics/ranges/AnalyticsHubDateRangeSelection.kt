package com.woocommerce.android.ui.analytics.ranges

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
import com.woocommerce.android.ui.analytics.ranges.data.LastQuarterRangeData
import com.woocommerce.android.ui.analytics.ranges.data.LastYearRangeData
import com.woocommerce.android.ui.analytics.ranges.data.QuarterToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.YearToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.LastMonthRangeData
import com.woocommerce.android.ui.analytics.ranges.data.LastWeekRangeData
import com.woocommerce.android.ui.analytics.ranges.data.MonthToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.TodayRangeData
import com.woocommerce.android.ui.analytics.ranges.data.WeekToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.YesterdayRangeData
import java.util.Calendar
import java.util.Date

class AnalyticsHubDateRangeSelection(
    selectionType: SelectionType,
    currentDate: Date = Date(),
    calendar: Calendar = Calendar.getInstance()
) {
    val currentRange: AnalyticsHubTimeRange?
    val previousRange: AnalyticsHubTimeRange?

    init {
        val rangeData = generateTimeRangeData(selectionType, currentDate, calendar)
        this.currentRange = rangeData.currentRange
        this.previousRange = rangeData.previousRange
    }

    private fun generateTimeRangeData(
        selectionType: SelectionType,
        referenceDate: Date,
        calendar: Calendar
    ): AnalyticsHubTimeRangeData {
        return when (selectionType) {
            TODAY -> TodayRangeData(referenceDate, calendar)
            YESTERDAY -> YesterdayRangeData(referenceDate, calendar)
            WEEK_TO_DATE -> WeekToDateRangeData(referenceDate, calendar)
            LAST_WEEK -> LastWeekRangeData(referenceDate, calendar)
            MONTH_TO_DATE -> MonthToDateRangeData(referenceDate, calendar)
            LAST_MONTH -> LastMonthRangeData(referenceDate, calendar)
            QUARTER_TO_DATE -> QuarterToDateRangeData(referenceDate, calendar)
            LAST_QUARTER -> LastQuarterRangeData(referenceDate, calendar)
            YEAR_TO_DATE -> YearToDateRangeData(referenceDate, calendar)
            LAST_YEAR -> LastYearRangeData(referenceDate, calendar)
        }
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
        YEAR_TO_DATE;
    }
}
