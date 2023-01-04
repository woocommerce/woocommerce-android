package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.LAST_MONTH
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.LAST_WEEK
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.YESTERDAY
import com.woocommerce.android.ui.analytics.ranges.data.LastMonthRangeData
import com.woocommerce.android.ui.analytics.ranges.data.LastWeekRangeData
import com.woocommerce.android.ui.analytics.ranges.data.MonthToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.TodayRangeData
import com.woocommerce.android.ui.analytics.ranges.data.WeekToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.YesterdayRangeData
import java.util.Calendar
import java.util.Date

class AnalyticsHubDateRangeSelection(
    selectionType: AnalyticsHubRangeSelectionType,
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
        selectedType: AnalyticsHubRangeSelectionType,
        referenceDate: Date,
        calendar: Calendar
    ): AnalyticsHubTimeRangeData {
        return when (selectedType) {
            TODAY -> TodayRangeData(referenceDate, calendar)
            YESTERDAY -> YesterdayRangeData(referenceDate, calendar)
            WEEK_TO_DATE -> WeekToDateRangeData(referenceDate, calendar)
            LAST_WEEK -> LastWeekRangeData(referenceDate, calendar)
            MONTH_TO_DATE -> MonthToDateRangeData(referenceDate, calendar)
            LAST_MONTH -> LastMonthRangeData(referenceDate, calendar)
        }
    }

    enum class AnalyticsHubRangeSelectionType {
        TODAY,
        YESTERDAY,
        WEEK_TO_DATE,
        LAST_WEEK,
        MONTH_TO_DATE,
        LAST_MONTH;
    }
}
