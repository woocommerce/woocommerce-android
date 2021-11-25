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
        AnalyticsDateRanges.TODAY -> getTodayRange()
        AnalyticsDateRanges.YESTERDAY -> getYesterdayRange()
        AnalyticsDateRanges.LAST_WEEK -> getLastWeekRange()
        AnalyticsDateRanges.LAST_MONTH -> getLastMonthRange()
        AnalyticsDateRanges.LAST_QUARTER -> getLastQuarterRange()
        AnalyticsDateRanges.LAST_YEAR -> getLastYearRange()
        AnalyticsDateRanges.WEEK_TO_DATE -> getWeekToDateRange()
        AnalyticsDateRanges.MONTH_TO_DATE -> getMonthToDateRange()
        AnalyticsDateRanges.QUARTER_TO_DATE -> getQuarterToRangeDate()
        AnalyticsDateRanges.YEAR_TO_DATE -> getYearToDateRange()
    }

    private fun getYearToDateRange() = MultipleDateRange(
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(), dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.YEAR, MINUS_ONE)), SimpleDateRange(dateUtils.getDateForFirstDayOfYear()
        , dateUtils.getCurrentDate())
    )

    private fun getQuarterToRangeDate() = MultipleDateRange(
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousQuarter(),
            dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.MONTH,
                MONTHS_IN_QUARTER)),
        SimpleDateRange(dateUtils.getDateForFirstDayOfQuarter(), dateUtils.getCurrentDate())
    )

    private fun getMonthToDateRange() = MultipleDateRange(
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousMonth(),
            dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.MONTH, MINUS_ONE)),
        SimpleDateRange(dateUtils.getDateForFirstDayOfMonth(), dateUtils.getCurrentDate())
    )

    private fun getWeekToDateRange() = MultipleDateRange(
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousWeek(),
            dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.DAY_OF_YEAR,
                DAYS_IN_WEEK)),
        SimpleDateRange(dateUtils.getDateForFirstDayOfWeek(), dateUtils.getCurrentDate())
    )

    private fun getLastYearRange() = MultipleDateRange(
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(TWO),
            dateUtils.getDateForLastDayOfPreviousYear(TWO)),
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(),
            dateUtils.getDateForLastDayOfPreviousYear()),
    )

    private fun getLastQuarterRange() = MultipleDateRange(
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousQuarter(TWO),
            dateUtils.getDateForLastDayOfPreviousQuarter(TWO)),
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousQuarter(),
            dateUtils.getDateForLastDayOfPreviousQuarter()),
    )

    private fun getYesterdayRange() = SimpleDateRange(Date(dateUtils.getCurrentDateTimeMinusDays(TWO)),
        Date(dateUtils.getCurrentDateTimeMinusDays(ONE)))

    private fun getTodayRange() =
        SimpleDateRange(Date(dateUtils.getCurrentDateTimeMinusDays(ONE)), dateUtils.getCurrentDate())

    private fun getLastMonthRange() = MultipleDateRange(
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousMonth(TWO),
            dateUtils.getDateForLastDayOfPreviousMonth(TWO)),
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousMonth(),
            dateUtils.getDateForLastDayOfPreviousMonth()),
    )

    private fun getLastWeekRange() = MultipleDateRange(
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousWeek(TWO),
            dateUtils.getDateForLastDayOfPreviousWeek(TWO)),
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousWeek(),
            dateUtils.getDateForLastDayOfPreviousWeek()),
    )

    companion object {
        const val DAYS_IN_WEEK = -7
        const val MONTHS_IN_QUARTER = -3
        const val MINUS_ONE = -1
        const val TWO = 2
        const val ONE = 1
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

    return if (isSameYearAndMonth) {
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
}
