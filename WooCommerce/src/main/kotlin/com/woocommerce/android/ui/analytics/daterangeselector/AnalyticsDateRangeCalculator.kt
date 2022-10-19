package com.woocommerce.android.ui.analytics.daterangeselector

import android.os.Parcelable
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.MultipleDateRange
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.SimpleDateRange
import com.woocommerce.android.util.DateUtils
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class AnalyticsDateRange {
    @Parcelize
    data class SimpleDateRange(
        val from: Date,
        val to: Date
    ) : AnalyticsDateRange(), Parcelable

    @Parcelize
    data class MultipleDateRange(
        val from: SimpleDateRange,
        val to: SimpleDateRange
    ) : AnalyticsDateRange(), Parcelable
}

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
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(TWO), getDateForLastDayOfTwoYearsAgo()),
        SimpleDateRange(dateUtils.getDateForFirstDayOfPreviousYear(), dateUtils.getDateForLastDayOfPreviousYear()),
    )

    private fun getLastQuarterRange() = MultipleDateRange(
        SimpleDateRange(getDateOFirstDayTwoQuartersAgo(), getDateOfLastDayTwoQuartersAgo()),
        SimpleDateRange(getDateOfFirstDayPreviousQuarter(), getDateOfLastDayPreviousQuarter()),
    )

    private fun getDateOfLastDayPreviousQuarter() = dateUtils.getDateForLastDayOfPreviousQuarter()

    private fun getDateOfFirstDayPreviousQuarter() = dateUtils.getDateForFirstDayOfPreviousQuarter()

    private fun getDateOFirstDayTwoQuartersAgo() = dateUtils.getDateForFirstDayOfPreviousQuarter(TWO)

    private fun getDateOfLastDayTwoQuartersAgo() = dateUtils.getDateForLastDayOfPreviousQuarter(TWO)

    private fun getYesterdayRange() = SimpleDateRange(getDateForTwoDaysAgo(), getDateForYesterday())

    private fun getDateForTwoDaysAgo() = Date(dateUtils.getCurrentDateTimeMinusDays(TWO))

    private fun getTodayRange() = SimpleDateRange(getDateForYesterday(), dateUtils.getCurrentDate())

    private fun getDateForYesterday() = Date(dateUtils.getCurrentDateTimeMinusDays(ONE))

    private fun getLastMonthRange() =
        MultipleDateRange(
            SimpleDateRange(getDateOfFirstDayTwoMonthsAgo(), getDateLastDayTwoMonthsAgo()),
            SimpleDateRange(getDateFirstDayPreviousMonth(), getDateForLastDayPreviousMonth()),
        )

    private fun getDateForLastDayPreviousMonth() = dateUtils.getDateForLastDayOfPreviousMonth()

    private fun getDateFirstDayPreviousMonth() = dateUtils.getDateForFirstDayOfPreviousMonth()

    private fun getDateLastDayTwoMonthsAgo() = dateUtils.getDateForLastDayOfPreviousMonth(TWO)

    private fun getDateOfFirstDayTwoMonthsAgo() = dateUtils.getDateForFirstDayOfPreviousMonth(TWO)

    private fun getLastWeekRange() = MultipleDateRange(
        SimpleDateRange(getDateOfFirstDayTwoWeeksAgo(), getDateOfLastDayTwoWeeksAgo()),
        SimpleDateRange(getDateFirstDayOneWeekAgo(), getDateLastDayOneWeekAgo()),
    )

    private fun getDateLastDayOneWeekAgo() = dateUtils.getDateForLastDayOfPreviousWeek()

    private fun getDateFirstDayOneWeekAgo() = dateUtils.getDateForFirstDayOfPreviousWeek()

    private fun getDateOfLastDayTwoWeeksAgo() = dateUtils.getDateForLastDayOfPreviousWeek(TWO)

    private fun getDateOfFirstDayTwoWeeksAgo() = dateUtils.getDateForFirstDayOfPreviousWeek(TWO)

    private fun getMinusOneYearDate() =
        dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.YEAR, MINUS_ONE)

    private fun getDateMinusQuarter() =
        dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.MONTH, MONTHS_IN_QUARTER)

    private fun getDateSevenDaysAgo() =
        dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.DAY_OF_YEAR, DAYS_IN_WEEK)

    private fun getDateMinusOneMonth() =
        dateUtils.getDateTimeAppliedOperation(dateUtils.getCurrentDate(), Calendar.MONTH, MINUS_ONE)

    private fun getDateForLastDayOfTwoYearsAgo() = dateUtils.getDateForLastDayOfPreviousYear(TWO)

    companion object {
        const val DAYS_IN_WEEK = -7
        const val MONTHS_IN_QUARTER = -3
        const val MINUS_ONE = -1
        const val TWO = 2
        const val ONE = 1
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
    YEAR_TO_DATE("Year to Date");

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
