package com.woocommerce.android.ui.analytics.daterangeselector

import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRange.SimpleDateRange
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AnalyticsDateRangeCalculatorTest : BaseUnitTest() {
    private val dateUtils: DateUtils = mock()
    private val sut = AnalyticsDateRangeCalculator(dateUtils)

    companion object {
        const val DATE_ZERO = 0L
        private const val THREE_JAN_1970_TIME = 234083000L
        private const val THREE_FEB_1970_TIME = 2912483000L
        private const val TWTY_FOUR_NOV_2021 = 1637776266465L
        private const val TWTY8_NOV_2021 = 1638087612000L
        private const val TWO_DEC_2021 = 1638433212000L

        val date = Date().apply { time = DATE_ZERO }
        val threeJan1970 = Date().apply { time = THREE_JAN_1970_TIME }
        val threeFeb1970 = Date().apply { time = THREE_FEB_1970_TIME }
        val twtyFourNov2021 = Date().apply { time = TWTY_FOUR_NOV_2021 }
        val twtyEightNov2021 = Date().apply { time = TWTY8_NOV_2021 }
        val oneDec2021 = Date().apply { time = TWO_DEC_2021 }

        const val SAME_YEAR_SAME_MONTH_EXPECTED = "Jan 1 - 3, 1970"
        const val SAME_YEAR_DIFFERENT_MONTH_EXPECTED = "Jan 3 - Feb 3, 1970"
        const val DIFFERENT_YEAR_DIFFERENT_MONTH_EXPECTED = "Jan 3, 1970 - Nov 24, 2021"
        const val DIFFERENT_YEAR_DIFFERENT_MONTH_EXPECTED_TWO = "Nov 28 - Dec 2, 2021"
    }

    @Test
    fun `when the current date is given then get the date range for today is the expected`() {
        // Given
        whenever(dateUtils.getCurrentDateTimeMinusDays(1)).thenReturn(DATE_ZERO)
        whenever(dateUtils.getCurrentDate()).thenReturn(date)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.TODAY)

        // Then
        assertTrue(result is SimpleDateRange)
        assertEquals(date, result.from)
        assertEquals(date, result.to)
    }

    @Test
    fun `when a any previous days date is given then get the date range for yesterday is the expected`() {
        // Given
        whenever(dateUtils.getCurrentDateTimeMinusDays(any())).thenReturn(DATE_ZERO)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.YESTERDAY)

        // Then
        assertTrue(result is SimpleDateRange)
        assertEquals(date, result.from)
        assertEquals(date, result.to)
    }

    @Test
    fun `when a previous week date is given then get the date range for last week is the expected`() {
        // Given
        whenever(dateUtils.getDateForFirstDayOfPreviousWeek(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForLastDayOfPreviousWeek(any(), any())).thenReturn(date)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.LAST_WEEK)

        // Then
        assertTrue(result is AnalyticsDateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)
        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `when a previous month date is given then get the date range for last month is the expected`() {
        // Given
        whenever(dateUtils.getDateForFirstDayOfPreviousMonth(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForLastDayOfPreviousMonth(any(), any())).thenReturn(date)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.LAST_MONTH)

        // Then
        assertTrue(result is AnalyticsDateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)
        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `when a previous quarter date is given then get the date range for last quarter is the expected`() {
        // Given
        whenever(dateUtils.getDateForFirstDayOfPreviousQuarter(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForLastDayOfPreviousQuarter(any(), any())).thenReturn(date)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.LAST_QUARTER)

        // Then
        assertTrue(result is AnalyticsDateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)
        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `when a previous year date is given then get the date range for last quarter is the expected`() {
        // Given
        whenever(dateUtils.getDateForFirstDayOfPreviousYear(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForLastDayOfPreviousYear(any(), any())).thenReturn(date)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.LAST_YEAR)

        // Then
        assertTrue(result is AnalyticsDateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)
        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `when current date and previous week date is given then get the date range for week to date is the expected`() {
        // Given
        whenever(dateUtils.getCurrentDate()).thenReturn(date)
        whenever(dateUtils.getDateTimeAppliedOperation(date, Calendar.DAY_OF_YEAR, -7)).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfPreviousWeek(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfWeek(any())).thenReturn(date)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.WEEK_TO_DATE)

        // Then
        assertTrue(result is AnalyticsDateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)
        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `when current date and previous month date is given then get the range for month to date is the expected`() {
        // Given
        whenever(dateUtils.getCurrentDate()).thenReturn(date)
        whenever(dateUtils.getDateTimeAppliedOperation(date, Calendar.MONTH, -1)).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfPreviousMonth(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfMonth(any())).thenReturn(date)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.MONTH_TO_DATE)

        // Then
        assertTrue(result is AnalyticsDateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)
        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `when current date and quarter week date is given then get the range for quarter to date is the expected`() {
        // Given
        whenever(dateUtils.getCurrentDate()).thenReturn(date)
        whenever(dateUtils.getDateTimeAppliedOperation(date, Calendar.MONTH, -3)).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfPreviousQuarter(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfQuarter(any())).thenReturn(date)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.QUARTER_TO_DATE)

        // Then
        assertTrue(result is AnalyticsDateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)
        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `when current date and previous year date is given then get the date range for year to date is the expected`() {
        // Given
        whenever(dateUtils.getCurrentDate()).thenReturn(date)
        whenever(dateUtils.getDateTimeAppliedOperation(date, Calendar.YEAR, -1)).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfPreviousYear(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfYear(any())).thenReturn(date)

        // When
        val result = sut.getAnalyticsDateRangeFrom(AnalyticTimePeriod.YEAR_TO_DATE)

        // Then
        assertTrue(result is AnalyticsDateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)

        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `get the friendly period date range is the expected`() {
        val sameYearAndMonthFriendlyFormattedDate = SimpleDateRange(date, threeJan1970)
            .formatDatesToFriendlyPeriod(Locale.UK)
        assertEquals(SAME_YEAR_SAME_MONTH_EXPECTED, sameYearAndMonthFriendlyFormattedDate)

        val sameYearAndDifferentMonthFriendlyFormattedDate = SimpleDateRange(threeJan1970, threeFeb1970)
            .formatDatesToFriendlyPeriod(Locale.UK)
        assertEquals(SAME_YEAR_DIFFERENT_MONTH_EXPECTED, sameYearAndDifferentMonthFriendlyFormattedDate)

        val differentYearAndDifferentMonthFriendlyFormattedDate = SimpleDateRange(threeJan1970, twtyFourNov2021)
            .formatDatesToFriendlyPeriod(Locale.UK)
        assertEquals(DIFFERENT_YEAR_DIFFERENT_MONTH_EXPECTED, differentYearAndDifferentMonthFriendlyFormattedDate)

        val differentYearAndDifferentMonthFriendlyFormattedDateTwo =
            SimpleDateRange(twtyEightNov2021, oneDec2021).formatDatesToFriendlyPeriod(Locale.UK)
        assertEquals(
            DIFFERENT_YEAR_DIFFERENT_MONTH_EXPECTED_TWO,
            differentYearAndDifferentMonthFriendlyFormattedDateTwo
        )
    }
}
