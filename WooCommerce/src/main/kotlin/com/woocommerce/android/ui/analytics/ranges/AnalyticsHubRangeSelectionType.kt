package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubTodayRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubWeekToDateRangeData
import com.woocommerce.android.ui.analytics.ranges.data.AnalyticsHubYesterdayRangeData
import com.woocommerce.android.util.DateUtils
import java.util.*

enum class AnalyticsHubRangeSelectionType {
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

    fun generateTimeRangeData(
        referenceDate: Date,
        calendar: Calendar,
        dateUtils: DateUtils): AnalyticsHubTimeRangeData {
        return when (this) {
            TODAY -> AnalyticsHubTodayRangeData(referenceDate, calendar, dateUtils)
            YESTERDAY -> AnalyticsHubYesterdayRangeData(referenceDate, calendar, dateUtils)
            WEEK_TO_DATE -> AnalyticsHubWeekToDateRangeData(referenceDate, calendar, dateUtils)
            else -> AnalyticsHubTodayRangeData(referenceDate, calendar, dateUtils)

        }
//        return when (this) {
//            TODAY -> AnalyticsHubTodayTimeRangeData()
//            YESTERDAY -> AnalyticsHubYesterdayTimeRangeData()
//            LAST_WEEK -> AnalyticsHubLastWeekTimeRangeData()
//            LAST_MONTH -> AnalyticsHubLastMonthTimeRangeData()
//            LAST_QUARTER -> AnalyticsHubLastQuarterTimeRangeData()
//            LAST_YEAR -> AnalyticsHubLastYearTimeRangeData()
//            WEEK_TO_DATE -> AnalyticsHubWeekToDateRangeData()
//            MONTH_TO_DATE -> AnalyticsHubMonthToDateRangeData()
//            QUARTER_TO_DATE -> AnalyticsHubQuarterToDateRangeData()
//            YEAR_TO_DATE -> AnalyticsHubYearToDateRangeData()
//            CUSTOM -> AnalyticsHubCustomTimeRangeData()
//        }
    }
}
