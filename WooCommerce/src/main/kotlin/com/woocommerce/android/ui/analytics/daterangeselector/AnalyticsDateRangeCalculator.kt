package com.woocommerce.android.ui.analytics.daterangeselector

import com.woocommerce.android.ui.analytics.daterangeselector.DateRange.MultipleDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.DateRange.SimpleDateRange
import com.woocommerce.android.util.DateUtils
import java.util.*
import javax.inject.Inject

sealed class DateRange {
    data class SimpleDateRange(val from: Date, val to: Date) : DateRange()
    data class MultipleDateRange(val from: SimpleDateRange, val to: SimpleDateRange) : DateRange()
}

class AnalyticsDateRangeCalculator @Inject constructor(
    val dateUtils: DateUtils
) {
    fun getAnalyticsDateRangeFrom(selectionRange: AnalyticsDateRanges): DateRange = when (selectionRange) {
        AnalyticsDateRanges.TODAY ->
            SimpleDateRange(Date(dateUtils.getCurrentDateTimeMinusDays(1)), dateUtils.getCurrentDate())
        AnalyticsDateRanges.YESTERDAY ->
            SimpleDateRange(Date(dateUtils.getCurrentDateTimeMinusDays(2)), Date(dateUtils.getCurrentDateTimeMinusDays(1)))
        AnalyticsDateRanges.LAST_WEEK ->
            MultipleDateRange(
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousWeek(2), dateUtils.getDateForLastDayOfPreviousWeek(2)),
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousWeek(), dateUtils.getDateForLastDayOfPreviousWeek()),
            )
        AnalyticsDateRanges.LAST_MONTH ->
            MultipleDateRange(
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousMonth(2), dateUtils.getDateForLastDayOfPreviousMonth(2)),
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousMonth(), dateUtils.getDateForLastDayOfPreviousMonth()),
            )
        AnalyticsDateRanges.LAST_QUARTER ->
            MultipleDateRange(
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousQuarter(2), dateUtils.getDateForLastDayOfPreviousQuarter(2)),
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousQuarter(), dateUtils.getDateForLastDayOfPreviousQuarter()),
            )
        AnalyticsDateRanges.LAST_YEAR ->
            MultipleDateRange(
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(2), dateUtils.getDateForLastDayOfPreviousYear(2)),
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(), dateUtils.getDateForLastDayOfPreviousYear()),
            )
        AnalyticsDateRanges.WEEK_TO_DATE ->
            MultipleDateRange(
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousWeek(), dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.DAY_OF_YEAR, -7)),
                SimpleDateRange(dateUtils.getDateForFirstDayOfWeek(), dateUtils.getCurrentDate())
            )
        AnalyticsDateRanges.MONTH_TO_DATE ->
            MultipleDateRange(
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousMonth(), dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.MONTH, -1)),
                SimpleDateRange(dateUtils.getDateForFirstDayOfMonth(), dateUtils.getCurrentDate())
            )
        AnalyticsDateRanges.QUARTER_TO_DATE ->
            MultipleDateRange(
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousQuarter(), dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.MONTH, -3)),
                SimpleDateRange(dateUtils.getDateForFirstDayOfQuarter(), dateUtils.getCurrentDate())
            )
        AnalyticsDateRanges.YEAR_TO_DATE ->
            MultipleDateRange(
                SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(), dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.YEAR, -1)),
                SimpleDateRange(dateUtils.getDateForFirstDayOfYear(), dateUtils.getCurrentDate())
            )
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

    companion object {
        fun from(dateRangeDescription: String): AnalyticsDateRanges = values()
            .find { it.description == dateRangeDescription } ?: TODAY
    }
}

/**
 * Method to convert two dates into a friendly period string
 * i.e. 2021-08-08T08:12:03 and 2021-08-09T08:12:03 is formatted to Aug 8 - 9, 2021
 */
@Throws(IllegalArgumentException::class)
fun SimpleDateRange.formatDatesToFriendlyPeriod(locale: Locale = Locale.getDefault()): String {
    return try {

        val calendar: Calendar = let {
            Calendar.getInstance().apply {
                time = it.from
            }
        }

        val anotherCalendar: Calendar = let {
            Calendar.getInstance().apply {
                time = it.to
            }
        }

        val isSameYearAndMonth =
            calendar.get(Calendar.YEAR) == anotherCalendar.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == anotherCalendar.get(Calendar.MONTH)

        val minDay = kotlin.math.min(calendar.get(Calendar.DAY_OF_MONTH), anotherCalendar.get(Calendar.DAY_OF_MONTH))
        val maxDay = kotlin.math.max(calendar.get(Calendar.DAY_OF_MONTH), anotherCalendar.get(Calendar.DAY_OF_MONTH))
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale)
        val year = calendar.get(Calendar.YEAR)

        if (isSameYearAndMonth) {
            "$month $minDay - $maxDay, $year"
        } else {

            val isSameYear = calendar.get(Calendar.YEAR) == anotherCalendar.get(Calendar.YEAR)
            val anotherMonth = anotherCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale)

            if (isSameYear) {
                "$month $minDay - $anotherMonth $maxDay, $year"
            } else {
                val anotherYear = anotherCalendar.get(Calendar.YEAR)
                "$month $minDay, $year - $anotherMonth $maxDay, $anotherYear"
            }

        }

    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd: $this")
    }
}



