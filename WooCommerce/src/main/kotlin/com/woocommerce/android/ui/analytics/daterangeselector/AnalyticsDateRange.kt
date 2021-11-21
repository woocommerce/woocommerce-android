package com.woocommerce.android.ui.analytics.daterangeselector

import com.woocommerce.android.util.DateUtils
import java.util.*
import javax.inject.Inject

typealias DateRange = Pair<Date, Date>

class AnalyticsDateRange @Inject constructor(
    val dateUtils: DateUtils
) {

    private val currentDate = dateUtils.getCurrentDate()

    fun getAnalyticsDateRangeFrom(selectionRange: AnalyticsDateRanges) {
        when (selectionRange) {
            AnalyticsDateRanges.TODAY ->
                DateRange(currentDate, currentDate)
            AnalyticsDateRanges.YESTERDAY ->
                DateRange(dateUtils.getCurrentDateTimeMinusDays(1),
                    dateUtils.getCurrentDateTimeMinusDays(1))
            AnalyticsDateRanges.LAST_WEEK ->
                DateRange(dateUtils.getDateForFirstDayOfPreviousWeek(),
                    dateUtils.getDateTimeAppliedOperation(dateUtils.getDateForFirstDayOfPreviousWeek(), Calendar.DATE, 6))
            AnalyticsDateRanges.LAST_MONTH ->
                DateRange(dateUtils.getDateForFirstDayOfPreviousMonth(),
                    dateUtils.getDateForLastDayOfPreviousMonth())
            AnalyticsDateRanges.LAST_QUARTER ->
                DateRange(dateUtils.getDateForFirstDayOfPreviousQuarter(),
                    dateUtils.getDateForLastDayOfPreviousQuarter())
            AnalyticsDateRanges.LAST_YEAR ->
                DateRange(dateUtils.getDateForFirstDayOfPreviousYear(),
                    dateUtils.getDateTimeAppliedOperation(dateUtils.getDateForFirstDayOfPreviousYear(), Calendar.DATE, 365))
            AnalyticsDateRanges.WEEK_TO_DATE ->
                DateRange(dateUtils.getDateForFirstDayOfCurrentWeek(), currentDate)
            AnalyticsDateRanges.MONTH_TO_DATE ->
                DateRange(dateUtils.getDateForFirstDayOfCurrentMonth(), currentDate)
            AnalyticsDateRanges.QUARTER_TO_DATE ->
                DateRange(dateUtils.getDateForFirstDayOfCurrentQuarter(), currentDate)
            AnalyticsDateRanges.YEAR_TO_DATE ->
                DateRange(dateUtils.getDateForFirstDayOfCurrentYear(), currentDate)
        }
    }

}

enum class AnalyticsDateRanges(val description: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_WEEK("Last Week"),
    LAST_MONTH("Last Month"),
    LAST_QUARTER("Last Quarter"),
    LAST_YEAR("Last Year"),
    WEEK_TO_DATE("Week to Date"),
    MONTH_TO_DATE("Month to Date"),
    QUARTER_TO_DATE("Quarter to Date"),
    YEAR_TO_DATE("Year to Date");
}

