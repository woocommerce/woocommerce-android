package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.LAST_MONTH
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.LAST_WEEK
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.AnalyticsHubRangeSelectionType.YESTERDAY
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubLastMonthRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubLastWeekRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubMonthToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubTodayRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubWeekToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubYesterdayRangeData
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
            TODAY -> AnalyticsHubTodayRangeData(referenceDate, calendar)
            YESTERDAY -> AnalyticsHubYesterdayRangeData(referenceDate, calendar)
            WEEK_TO_DATE -> AnalyticsHubWeekToDateRangeData(referenceDate, calendar)
            LAST_WEEK -> AnalyticsHubLastWeekRangeData(referenceDate, calendar)
            MONTH_TO_DATE -> AnalyticsHubMonthToDateRangeData(referenceDate, calendar)
            LAST_MONTH -> AnalyticsHubLastMonthRangeData(referenceDate, calendar)
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
