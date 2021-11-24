package com.woocommerce.android.ui.analytics.daterangeselector

import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsDateRangeCalculatorTest : BaseUnitTest() {

    private val dateUtils: DateUtils = mock()
    private val sut = AnalyticsDateRangeCalculator(dateUtils)

    companion object {
        const val DATE_ZERO = 0L
        val date = Date().apply { time = DATE_ZERO }
    }

    @Test
    fun `should return expected date calculating range for today`() {

        whenever(dateUtils.getCurrentDateTimeMinusDays(1)).thenReturn(DATE_ZERO)
        whenever(dateUtils.getCurrentDate()).thenReturn(date)


        val result = sut.getAnalyticsDateRangeFrom(AnalyticsDateRanges.TODAY)

        assertTrue(result is DateRange.SimpleDateRange)
        assertEquals(date, result.from)
        assertEquals(date, result.to)
    }


    @Test
    fun `should return expected date calculating range for yesterday`() {

        whenever(dateUtils.getCurrentDateTimeMinusDays(any())).thenReturn(DATE_ZERO)

        val result = sut.getAnalyticsDateRangeFrom(AnalyticsDateRanges.YESTERDAY)

        assertTrue(result is DateRange.SimpleDateRange)
        assertEquals(date, result.from)
        assertEquals(date, result.to)
    }


    @Test
    fun `should return expected date calculating range for last week`() {

        whenever(dateUtils.getDateForFirstDayOfPreviousWeek(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForLastDayOfPreviousWeek(any(), any())).thenReturn(date)

        val result = sut.getAnalyticsDateRangeFrom(AnalyticsDateRanges.LAST_WEEK)

        assertTrue(result is DateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)

        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `should return expected date calculating range for last month`() {

        whenever(dateUtils.getDateForFirstDayOfPreviousMonth(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForLastDayOfPreviousMonth(any(), any())).thenReturn(date)

        val result = sut.getAnalyticsDateRangeFrom(AnalyticsDateRanges.LAST_MONTH)

        assertTrue(result is DateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)

        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `should return expected date calculating range for last quarter`() {

        whenever(dateUtils.getDateForFirstDayOfPreviousQuarter(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForLastDayOfPreviousQuarter(any(), any())).thenReturn(date)

        val result = sut.getAnalyticsDateRangeFrom(AnalyticsDateRanges.LAST_QUARTER)

        assertTrue(result is DateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)

        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }


    @Test
    fun `should return expected date calculating range for last year`() {

        whenever(dateUtils.getDateForFirstDayOfPreviousYear(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForLastDayOfPreviousYear(any(), any())).thenReturn(date)

        val result = sut.getAnalyticsDateRangeFrom(AnalyticsDateRanges.LAST_YEAR)

        assertTrue(result is DateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)

        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }


    @Test
    fun `should return expected date calculating range for week to date`() {

        whenever(dateUtils.getCurrentDate()).thenReturn(date)
        whenever(dateUtils.getDateTimeAppliedOperation(date, Calendar.DAY_OF_YEAR, -7)).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfPreviousWeek(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfWeek(any())).thenReturn(date)

        val result = sut.getAnalyticsDateRangeFrom(AnalyticsDateRanges.WEEK_TO_DATE)

        assertTrue(result is DateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)

        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `should return expected date calculating range for month to date`() {

        whenever(dateUtils.getCurrentDate()).thenReturn(date)
        whenever(dateUtils.getDateTimeAppliedOperation(date, Calendar.MONTH, -1)).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfPreviousMonth(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfMonth(any())).thenReturn(date)

        val result = sut.getAnalyticsDateRangeFrom(AnalyticsDateRanges.MONTH_TO_DATE)

        assertTrue(result is DateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)

        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `should return expected date calculating range for quarter to date`() {

        whenever(dateUtils.getCurrentDate()).thenReturn(date)
        whenever(dateUtils.getDateTimeAppliedOperation(date, Calendar.MONTH, -3)).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfPreviousQuarter(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfQuarter(any())).thenReturn(date)

        val result = sut.getAnalyticsDateRangeFrom(AnalyticsDateRanges.QUARTER_TO_DATE)

        assertTrue(result is DateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)

        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }

    @Test
    fun `should return expected date calculating range for year to date`() {

        whenever(dateUtils.getCurrentDate()).thenReturn(date)
        whenever(dateUtils.getDateTimeAppliedOperation(date, Calendar.YEAR, -1)).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfPreviousYear(any(), any())).thenReturn(date)
        whenever(dateUtils.getDateForFirstDayOfYear(any())).thenReturn(date)

        val result = sut.getAnalyticsDateRangeFrom(AnalyticsDateRanges.YEAR_TO_DATE)

        assertTrue(result is DateRange.MultipleDateRange)
        assertEquals(date, result.from.from)
        assertEquals(date, result.from.to)

        assertEquals(date, result.to.from)
        assertEquals(date, result.to.to)
    }


}
