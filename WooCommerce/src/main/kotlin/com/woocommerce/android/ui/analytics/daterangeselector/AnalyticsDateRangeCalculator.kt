package com.woocommerce.android.ui.analytics.daterangeselector

import com.woocommerce.android.util.DateUtils
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class AnalyticsDateRangeCalculator @Inject constructor(
    val dateUtils: DateUtils
) {
    fun getAnalyticsDateRangeFrom(selectionRange: AnalyticTimePeriod): AnalyticsDateRange =
        when (selectionRange) {
            AnalyticTimePeriod.TODAY -> getTodayRange()
            AnalyticTimePeriod.YESTERDAY -> getYesterdayRange()
            AnalyticTimePeriod.LAST_WEEK -> getLastWeekRange()
            AnalyticTimePeriod.LAST_MONTH -> getLastMonthRange()
            AnalyticTimePeriod.LAST_QUARTER -> getLastQuarterRange()
            AnalyticTimePeriod.LAST_YEAR -> getLastYearRange()
            AnalyticTimePeriod.WEEK_TO_DATE -> getWeekToDateRange()
            AnalyticTimePeriod.MONTH_TO_DATE -> getMonthToDateRange()
            AnalyticTimePeriod.QUARTER_TO_DATE -> getQuarterToRangeDate()
            AnalyticTimePeriod.YEAR_TO_DATE -> getYearToDateRange()
            AnalyticTimePeriod.CUSTOM -> getYearToDateRange() // unused - here for completion
        }

    private fun getYearToDateRange() = MultipleDateRange(
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(), getMinusOneYearDate()),
        SimpleDateRange(dateUtils.getDateForFirstDayOfYear(), dateUtils.getCurrentDate())
    )

    private fun getQuarterToRangeDate() = MultipleDateRange(
        SimpleDateRange(getDateOfFirstDayPreviousQuarter(), getDateMinusQuarter()),
        SimpleDateRange(dateUtils.getDateForFirstDayOfQuarter(), dateUtils.getCurrentDate())
    )

    private fun getMonthToDateRange() = MultipleDateRange(
        SimpleDateRange(getDateFirstDayPreviousMonth(), getDateMinusOneMonth()),
        SimpleDateRange(dateUtils.getDateForFirstDayOfMonth(), dateUtils.getCurrentDate())
    )

    private fun getWeekToDateRange() = MultipleDateRange(
        SimpleDateRange(getDateFirstDayOneWeekAgo(), getDateSevenDaysAgo()),
        SimpleDateRange(dateUtils.getDateForFirstDayOfWeek(), dateUtils.getCurrentDate())
    )

    private fun getLastYearRange() = MultipleDateRange(
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(2), getDateForLastDayOfTwoYearsAgo()),
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(), dateUtils.getDateForLastDayOfPreviousYear()),
    )

    private fun getLastQuarterRange() = MultipleDateRange(
        SimpleDateRange(getDateOFirstDayTwoQuartersAgo(), getDateOfLastDayTwoQuartersAgo()),
        SimpleDateRange(getDateOfFirstDayPreviousQuarter(), getDateOfLastDayPreviousQuarter()),
    )

    fun getAnalyticsDateRangeFromCustom(startDate: Date, endDate: Date) = SimpleDateRange(
        from = startDate, to = endDate
    )

    private fun getDateOfLastDayPreviousQuarter() = dateUtils.getDateForLastDayOfPreviousQuarter()

    private fun getDateOfFirstDayPreviousQuarter() = dateUtils.getDateForFirstDayOfPreviousQuarter()

    private fun getDateOFirstDayTwoQuartersAgo() = dateUtils.getDateForFirstDayOfPreviousQuarter(2)

    private fun getDateOfLastDayTwoQuartersAgo() = dateUtils.getDateForLastDayOfPreviousQuarter(2)

    private fun getYesterdayRange() = SimpleDateRange(getCurrentDateTimeMinusDays(2), getDateForYesterday())

    private fun getTodayRange() = SimpleDateRange(getDateForYesterday(), dateUtils.getCurrentDate())

    private fun getLastMonthRange() =
        MultipleDateRange(
            SimpleDateRange(getDateOfFirstDayTwoMonthsAgo(), getDateLastDayTwoMonthsAgo()),
            SimpleDateRange(getDateFirstDayPreviousMonth(), getDateForLastDayPreviousMonth()),
        )

    private fun getDateForLastDayPreviousMonth() = dateUtils.getDateForLastDayOfPreviousMonth()

    private fun getDateFirstDayPreviousMonth() = dateUtils.getDateForFirstDayOfPreviousMonth()

    private fun getDateLastDayTwoMonthsAgo() = dateUtils.getDateForLastDayOfPreviousMonth(2)

    private fun getDateOfFirstDayTwoMonthsAgo() = dateUtils.getDateForFirstDayOfPreviousMonth(2)

    private fun getLastWeekRange() = MultipleDateRange(
        SimpleDateRange(getDateOfFirstDayTwoWeeksAgo(), getDateOfLastDayTwoWeeksAgo()),
        SimpleDateRange(getDateFirstDayOneWeekAgo(), getDateLastDayOneWeekAgo()),
    )

    private fun getDateLastDayOneWeekAgo() = dateUtils.getDateForLastDayOfPreviousWeek()

    private fun getDateFirstDayOneWeekAgo() = dateUtils.getDateForFirstDayOfPreviousWeek()

    private fun getDateOfLastDayTwoWeeksAgo() = dateUtils.getDateForLastDayOfPreviousWeek(2)

    private fun getDateOfFirstDayTwoWeeksAgo() = dateUtils.getDateForFirstDayOfPreviousWeek(2)

    private fun getMinusOneYearDate() =
        dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.YEAR, -1)

    private fun getDateMinusQuarter() =
        dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.MONTH, -MONTHS_IN_QUARTER)

    private fun getDateSevenDaysAgo() =
        dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.DAY_OF_YEAR, -DAYS_IN_WEEK)

    private fun getDateMinusOneMonth() =
        dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.MONTH, -1)

    private fun getDateForLastDayOfTwoYearsAgo() = dateUtils.getDateForLastDayOfPreviousYear(2)

    private fun getDateForYesterday() = getCurrentDateTimeMinusDays(1)

    private fun getCurrentDateTimeMinusDays(days: Int) = Date(dateUtils.getCurrentDateTimeMinusDays(days))

    companion object {
        const val DAYS_IN_WEEK = 7
        const val MONTHS_IN_QUARTER = 3
    }
}

enum class AnalyticTimePeriod(val description: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_WEEK("Last Week"),
    LAST_MONTH("Last Month"),
    LAST_QUARTER("Last Quarter"),
    LAST_YEAR("Last Year"),
    WEEK_TO_DATE("Week to Date"),
    MONTH_TO_DATE("Month to Date"),
    QUARTER_TO_DATE("Quarter to Date"),
    YEAR_TO_DATE("Year to Date"),
    CUSTOM("Custom");

    companion object {
        fun from(datePeriod: String): AnalyticTimePeriod = values()
            .find { it.description == datePeriod } ?: TODAY
    }
}

/**
 * Method to convert two dates into a friendly period string
 * i.e. 2021-08-08T08:12:03 and 2021-08-09T08:12:03 is formatted to Aug 8 - 9, 2021
 */
@Throws(IllegalArgumentException::class)
fun SimpleDateRange.formatDatesToFriendlyPeriod(locale: Locale = Locale.getDefault()): String {
    val firstCalendar: Calendar = let {
        Calendar.getInstance().apply {
            time = it.from
        }
    }

    val secondCalendar: Calendar = let {
        Calendar.getInstance().apply {
            time = it.to
        }
    }

    val sortedDates = listOf(firstCalendar, secondCalendar).sorted()
    val firstDate = sortedDates.first()
    val secondDate = sortedDates[1]

    val isSameYearAndMonth =
        firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR) &&
            firstCalendar.get(Calendar.MONTH) == secondCalendar.get(Calendar.MONTH)

    val firstCalendarDay = firstDate.get(Calendar.DAY_OF_MONTH)
    val secondCalendarDay = secondDate.get(Calendar.DAY_OF_MONTH)

    val firstDisplayMonth = firstCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale)
    val firstYear = firstCalendar.get(Calendar.YEAR)

    return if (isSameYearAndMonth) {
        "$firstDisplayMonth $firstCalendarDay - $secondCalendarDay, $firstYear"
    } else {
        val isSameYear = firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
        val secondDisplayMonth = secondDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale)
        if (isSameYear) {
            "$firstDisplayMonth $firstCalendarDay - $secondDisplayMonth $secondCalendarDay, $firstYear"
        } else {
            val secondYear = secondCalendar.get(Calendar.YEAR)
            "$firstDisplayMonth $firstCalendarDay, $firstYear - $secondDisplayMonth $secondCalendarDay, $secondYear"
        }
    }
}
