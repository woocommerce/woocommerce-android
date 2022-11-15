package com.woocommerce.android.ui.analytics.daterangeselector

import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AnalyticsDateRangeCalculatorTest : BaseUnitTest() {
    private val dateUtils: DateUtils = mock()
    private val sut = AnalyticsDateRangeCalculator(dateUtils)

    // FluxC DateUtils conflict with com.woocommerce.android.util.DateUtils
    private fun getDateFromString(date: String) = org.wordpress.android.fluxc.utils.DateUtils.getDateFromString(date)

    @Test
    fun `when the current date is given then get the date range for today is the expected`() {
        // Given
        val today = getDateFromString("2022-10-28")
        val yesterday = getDateFromString("2022-10-27")

        whenever(dateUtils.getCurrentDate()).thenReturn(today)
        whenever(dateUtils.getCurrentDateTimeMinusDays(1)).thenReturn(yesterday.time)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.TODAY)

        // Then
        assertTrue(result is SimpleDateRange)
        assertEquals(yesterday, result.from)
        assertEquals(today, result.to)
    }

    @Test
    fun `when a any previous days date is given then get the date range for yesterday is the expected`() {
        // Given
        val yesterday = getDateFromString("2022-10-27")
        val theDayBeforeYesterday = getDateFromString("2022-10-26")

        whenever(dateUtils.getCurrentDateTimeMinusDays(1)).thenReturn(yesterday.time)
        whenever(dateUtils.getCurrentDateTimeMinusDays(2)).thenReturn(theDayBeforeYesterday.time)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.YESTERDAY)

        // Then
        assertTrue(result is SimpleDateRange)
        assertEquals(theDayBeforeYesterday, result.from)
        assertEquals(yesterday, result.to)
    }

    @Test
    fun `when a previous week date is given then get the date range for last week vs two weeks ago`() {
        // Given
        val twoWeeksAgoFirstDay = getDateFromString("2022-10-9")
        val twoWeeksAgoLastDay = getDateFromString("2022-10-16")
        val oneWeekAgoFirstDay = getDateFromString("2022-10-17")
        val oneWeekAgoLastDay = getDateFromString("2022-10-23")

        whenever(dateUtils.getDateForFirstDayOfPreviousWeek(eq(2), any())).thenReturn(twoWeeksAgoFirstDay)
        whenever(dateUtils.getDateForLastDayOfPreviousWeek(eq(2), any())).thenReturn(twoWeeksAgoLastDay)
        whenever(dateUtils.getDateForFirstDayOfPreviousWeek(eq(1), any())).thenReturn(oneWeekAgoFirstDay)
        whenever(dateUtils.getDateForLastDayOfPreviousWeek(eq(1), any())).thenReturn(oneWeekAgoLastDay)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.LAST_WEEK)

        // Then
        assertTrue(result is MultipleDateRange)
        assertEquals(twoWeeksAgoFirstDay, result.from.from)
        assertEquals(twoWeeksAgoLastDay, result.from.to)
        assertEquals(oneWeekAgoFirstDay, result.to.from)
        assertEquals(oneWeekAgoLastDay, result.to.to)
    }

    @Test
    fun `when a previous month date is given then get the date range for last month vs two months ago`() {
        // Given
        val twoMonthsAgoFirstDay = getDateFromString("2022-08-01")
        val twoMonthsAgoLastDay = getDateFromString("2022-08-31")
        val oneMonthAgoFirstDay = getDateFromString("2022-09-01")
        val oneMonthAgoLastDay = getDateFromString("2022-09-30")

        whenever(dateUtils.getDateForFirstDayOfPreviousMonth(eq(2), any())).thenReturn(twoMonthsAgoFirstDay)
        whenever(dateUtils.getDateForLastDayOfPreviousMonth(eq(2), any())).thenReturn(twoMonthsAgoLastDay)
        whenever(dateUtils.getDateForFirstDayOfPreviousMonth(eq(1), any())).thenReturn(oneMonthAgoFirstDay)
        whenever(dateUtils.getDateForLastDayOfPreviousMonth(eq(1), any())).thenReturn(oneMonthAgoLastDay)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.LAST_MONTH)

        // Then
        assertTrue(result is MultipleDateRange)
        assertEquals(twoMonthsAgoFirstDay, result.from.from)
        assertEquals(twoMonthsAgoLastDay, result.from.to)
        assertEquals(oneMonthAgoFirstDay, result.to.from)
        assertEquals(oneMonthAgoLastDay, result.to.to)
    }

    @Test
    fun `when a previous quarter date is given then get the date range for last quarter vs two quarters ago`() {
        // Given
        val twoQuartersAgoFirstDay = getDateFromString("2022-04-01")
        val twoQuartersAgoLastDay = getDateFromString("2022-06-30")
        val oneQuarterAgoFirstDay = getDateFromString("2022-07-01")
        val oneQuarterAgoLastDay = getDateFromString("2022-09-30")

        whenever(dateUtils.getDateForFirstDayOfPreviousQuarter(eq(2), any())).thenReturn(twoQuartersAgoFirstDay)
        whenever(dateUtils.getDateForLastDayOfPreviousQuarter(eq(2), any())).thenReturn(twoQuartersAgoLastDay)
        whenever(dateUtils.getDateForFirstDayOfPreviousQuarter(eq(1), any())).thenReturn(oneQuarterAgoFirstDay)
        whenever(dateUtils.getDateForLastDayOfPreviousQuarter(eq(1), any())).thenReturn(oneQuarterAgoLastDay)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.LAST_QUARTER)

        // Then
        assertTrue(result is MultipleDateRange)
        assertEquals(twoQuartersAgoFirstDay, result.from.from)
        assertEquals(twoQuartersAgoLastDay, result.from.to)
        assertEquals(oneQuarterAgoFirstDay, result.to.from)
        assertEquals(oneQuarterAgoLastDay, result.to.to)
    }

    @Test
    fun `when a previous year date is given then get the date range for last year vs two years ago`() {
        // Given
        val twoYearsAgoFirstDay = getDateFromString("2020-01-01")
        val twoYearsAgoLastDay = getDateFromString("2020-12-31")
        val oneYearAgoFirstDay = getDateFromString("2021-01-01")
        val oneYearAgoLastDay = getDateFromString("2021-12-31")
        whenever(dateUtils.getDateForFirstDayOfPreviousYear(eq(2), any())).thenReturn(twoYearsAgoFirstDay)
        whenever(dateUtils.getDateForLastDayOfPreviousYear(eq(2), any())).thenReturn(twoYearsAgoLastDay)
        whenever(dateUtils.getDateForFirstDayOfPreviousYear(eq(1), any())).thenReturn(oneYearAgoFirstDay)
        whenever(dateUtils.getDateForLastDayOfPreviousYear(eq(1), any())).thenReturn(oneYearAgoLastDay)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.LAST_YEAR)

        // Then
        assertTrue(result is MultipleDateRange)
        assertEquals(twoYearsAgoFirstDay, result.from.from)
        assertEquals(twoYearsAgoLastDay, result.from.to)
        assertEquals(oneYearAgoFirstDay, result.to.from)
        assertEquals(oneYearAgoLastDay, result.to.to)
    }

    @Test
    fun `when week to date given then get the date range for week to date vs last week fist day to a week ago`() {
        // Given
        val oneWeekAgoFirstDay = getDateFromString("2022-10-17")
        val aWeekAgo = getDateFromString("2022-10-21")
        val thisWeekFirstDay = getDateFromString("2022-10-24")
        val today = getDateFromString("2022-10-28")

        whenever(dateUtils.getDateForFirstDayOfPreviousWeek(eq(1), any())).thenReturn(oneWeekAgoFirstDay)
        whenever(dateUtils.getDateTimeAppliedOperation(today, Calendar.DAY_OF_YEAR, -7)).thenReturn(aWeekAgo)
        whenever(dateUtils.getDateForFirstDayOfWeek(any())).thenReturn(thisWeekFirstDay)
        whenever(dateUtils.getCurrentDate()).thenReturn(today)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.WEEK_TO_DATE)

        // Then
        assertTrue(result is MultipleDateRange)
        assertEquals(oneWeekAgoFirstDay, result.from.from)
        assertEquals(aWeekAgo, result.from.to)
        assertEquals(thisWeekFirstDay, result.to.from)
        assertEquals(today, result.to.to)
    }

    @Test
    fun `when date to month given then get the range for month to date vs last month fist day to a month ago`() {
        // Given
        val oneMonthAgoFirstDay = getDateFromString("2022-09-01")
        val aMonthAgo = getDateFromString("2022-09-28")
        val thisMonthFirstDay = getDateFromString("2022-10-01")
        val today = getDateFromString("2022-10-28")

        whenever(dateUtils.getDateForFirstDayOfPreviousMonth(eq(1), any())).thenReturn(oneMonthAgoFirstDay)
        whenever(dateUtils.getDateTimeAppliedOperation(today, Calendar.MONTH, -1)).thenReturn(aMonthAgo)
        whenever(dateUtils.getDateForFirstDayOfMonth(any())).thenReturn(thisMonthFirstDay)
        whenever(dateUtils.getCurrentDate()).thenReturn(today)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.MONTH_TO_DATE)

        // Then
        assertTrue(result is MultipleDateRange)
        assertEquals(oneMonthAgoFirstDay, result.from.from)
        assertEquals(aMonthAgo, result.from.to)
        assertEquals(thisMonthFirstDay, result.to.from)
        assertEquals(today, result.to.to)
    }

    @Test
    fun `when date to quarter then get the range for quarter to date vs last quarter fist day to quarter ago`() {
        // Given
        val oneQuarterAgoFirstDay = getDateFromString("2022-07-01")
        val oneQuarterAgo = getDateFromString("2022-07-28")
        val thisQuarterFirstDay = getDateFromString("2022-10-01")
        val today = getDateFromString("2022-10-28")

        whenever(dateUtils.getDateForFirstDayOfPreviousQuarter(eq(1), any())).thenReturn(oneQuarterAgoFirstDay)
        whenever(dateUtils.getDateTimeAppliedOperation(today, Calendar.MONTH, -3)).thenReturn(oneQuarterAgo)
        whenever(dateUtils.getDateForFirstDayOfQuarter(any())).thenReturn(thisQuarterFirstDay)
        whenever(dateUtils.getCurrentDate()).thenReturn(today)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.QUARTER_TO_DATE)

        // Then
        assertTrue(result is MultipleDateRange)
        assertEquals(oneQuarterAgoFirstDay, result.from.from)
        assertEquals(oneQuarterAgo, result.from.to)
        assertEquals(thisQuarterFirstDay, result.to.from)
        assertEquals(today, result.to.to)
    }

    @Test
    fun `when current date and previous year date then get the date range for year to date is the expected`() {
        // Given
        val oneYearAgoFirstDay = getDateFromString("2021-01-01")
        val oneYearAgo = getDateFromString("2021-10-28")
        val thisYearFirstDay = getDateFromString("2022-01-01")
        val today = getDateFromString("2022-10-28")

        whenever(dateUtils.getDateForFirstDayOfPreviousYear(eq(1), any())).thenReturn(oneYearAgoFirstDay)
        whenever(dateUtils.getDateTimeAppliedOperation(today, Calendar.YEAR, -1)).thenReturn(oneYearAgo)
        whenever(dateUtils.getDateForFirstDayOfYear(any())).thenReturn(thisYearFirstDay)
        whenever(dateUtils.getCurrentDate()).thenReturn(today)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.YEAR_TO_DATE)

        // Then
        assertTrue(result is MultipleDateRange)
        assertEquals(oneYearAgoFirstDay, result.from.from)
        assertEquals(oneYearAgo, result.from.to)
        assertEquals(thisYearFirstDay, result.to.from)
        assertEquals(today, result.to.to)
    }

    @Test
    fun `when dates has same month and year then same year and month is friendly formatted`() {
        // Given
        val today = getDateFromString("2022-10-28")
        val twoDaysAgo = getDateFromString("2022-10-26")

        // When
        val sameYearAndMonthFriendlyFormattedDate = SimpleDateRange(twoDaysAgo, today)
            .formatDatesToFriendlyPeriod(Locale.ROOT)

        // Then
        val expected = "Oct 26 - 28, 2022"
        assertEquals(expected, sameYearAndMonthFriendlyFormattedDate)
    }

    @Test
    fun `when dates has same year then same year with different month is friendly formatted`() {
        // Given
        val aMonthAgo = getDateFromString("2022-09-28")
        val today = getDateFromString("2022-10-28")

        // When
        val expected = "Sep 28 - Oct 28, 2022"
        val sameYearAndDifferentMonthFriendlyFormattedDate = SimpleDateRange(aMonthAgo, today)
            .formatDatesToFriendlyPeriod(Locale.ROOT)

        // Then
        assertEquals(expected, sameYearAndDifferentMonthFriendlyFormattedDate)
    }

    @Test
    fun `when dates are different then different year with different month is friendly formatted`() {
        // Given
        val randomDate = getDateFromString("2021-06-12")
        val today = getDateFromString("2022-10-28")

        // When
        val differentYearAndDifferentMonthFriendlyFormattedDate = SimpleDateRange(randomDate, today)
            .formatDatesToFriendlyPeriod(Locale.ROOT)

        // Then
        val expected = "Jun 12, 2021 - Oct 28, 2022"
        assertEquals(expected, differentYearAndDifferentMonthFriendlyFormattedDate)
    }
}
