package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.endOfCurrentMonth
import com.woocommerce.android.extensions.endOfCurrentQuarter
import com.woocommerce.android.extensions.endOfCurrentWeek
import com.woocommerce.android.extensions.endOfCurrentYear
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_MONTH
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_QUARTER
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_WEEK
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_YEAR
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.QUARTER_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.YEAR_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.YESTERDAY
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal class StatsTimeRangeSelectionTest {
    private lateinit var testTimeZone: TimeZone
    private lateinit var testLocale: Locale
    private lateinit var testCalendar: Calendar

    @Before
    fun setUp() {
        testLocale = Locale.getDefault()
        testTimeZone = TimeZone.getDefault()
        testCalendar = Calendar.getInstance(testLocale)
        testCalendar.timeZone = testTimeZone
        testCalendar.firstDayOfWeek = Calendar.MONDAY
    }

    @Test
    fun `when selection type is year to date, then generate expected date information`() {
        // Given
        val today = midDayFrom("2020-02-29")
        val expectedCurrentEndDate = (testCalendar.clone() as Calendar)
            .apply { time = today }
            .endOfCurrentYear()
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2020-01-01"),
            end = expectedCurrentEndDate
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2019-01-01"),
            end = midDayFrom("2019-02-28")
        )

        // When
        val sut = YEAR_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is last year, then generate expected date information`() {
        // Given
        val today = midDayFrom("2020-02-29")
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2019-01-01"),
            end = dayEndFrom("2019-12-31")
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2018-01-01"),
            end = dayEndFrom("2018-12-31")
        )

        // When
        val sut = LAST_YEAR.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is quarter to date, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-02-15")
        val expectedCurrentEndDate = (testCalendar.clone() as Calendar)
            .apply { time = today }
            .endOfCurrentQuarter()
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2022-01-01"),
            end = expectedCurrentEndDate
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2021-10-01"),
            end = midDayFrom("2021-11-15")
        )

        // When
        val sut = QUARTER_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is last quarter, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-05-15")
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2022-01-01"),
            end = dayEndFrom("2022-03-31")
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2021-10-01"),
            end = dayEndFrom("2021-12-31")
        )

        // When
        val sut = LAST_QUARTER.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is month to date, then generate expected date information`() {
        // Given
        val today = midDayFrom("2010-07-31")
        val expectedCurrentEndDate = (testCalendar.clone() as Calendar)
            .apply { time = today }
            .endOfCurrentMonth()
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2010-07-01"),
            end = expectedCurrentEndDate
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2010-06-01"),
            end = midDayFrom("2010-06-30")
        )

        // When
        val sut = MONTH_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is last month, then generate expected date information`() {
        // Given
        val today = midDayFrom("2010-07-15")
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2010-06-01"),
            end = dayEndFrom("2010-06-30")
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2010-05-01"),
            end = dayEndFrom("2010-05-31")
        )

        // When
        val sut = LAST_MONTH.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is week to date, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val expectedCurrentEndDate = (testCalendar.clone() as Calendar)
            .apply { time = today }
            .endOfCurrentWeek()
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2022-06-27"),
            end = expectedCurrentEndDate
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2022-06-20"),
            end = midDayFrom("2022-06-24")
        )

        // When
        val sut = WEEK_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is last week, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2022-06-20"),
            end = dayEndFrom("2022-06-26")
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2022-06-13"),
            end = dayEndFrom("2022-06-19")
        )

        // When
        val sut = LAST_WEEK.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is today, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val expectedCurrentEndDate = (testCalendar.clone() as Calendar)
            .apply { time = today }
            .endOfCurrentDay()
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2022-07-01"),
            end = expectedCurrentEndDate
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2022-06-30"),
            end = midDayFrom("2022-06-30")
        )

        // When
        val sut = TODAY.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is yesterday, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2022-06-30"),
            end = dayEndFrom("2022-06-30")
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2022-06-29"),
            end = dayEndFrom("2022-06-29")
        )

        // When
        val sut = YESTERDAY.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is custom, then generate expected date information`() {
        // Given
        val expectedCurrentRange = StatsTimeRange(
            start = dayStartFrom("2022-12-05"),
            end = dayEndFrom("2022-12-07")
        )
        val expectedPreviousRange = StatsTimeRange(
            start = dayStartFrom("2022-12-02"),
            end = dayEndFrom("2022-12-04")
        )

        // When
        val sut = CUSTOM.generateSelectionData(
            referenceStartDate = midDayFrom("2022-12-05"),
            referenceEndDate = midDayFrom("2022-12-07"),
            calendar = testCalendar,
            locale = testLocale
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is year to date, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val sut = YEAR_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jan 1 – Jul 1, 2022")
        assertThat(previousRangeDescription).isEqualTo("Jan 1 – Jul 1, 2021")
    }

    @Test
    fun `when selection type is last year, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val sut = LAST_YEAR.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jan 1 – Dec 31, 2021")
        assertThat(previousRangeDescription).isEqualTo("Jan 1 – Dec 31, 2020")
    }

    @Test
    fun `when selection type is quarter to date, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-02-15")
        val sut = QUARTER_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jan 1 – Feb 15, 2022")
        assertThat(previousRangeDescription).isEqualTo("Oct 1 – Nov 15, 2021")
    }

    @Test
    fun `when selection type is last quarter, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-05-15")
        val sut = LAST_QUARTER.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jan 1 – Mar 31, 2022")
        assertThat(previousRangeDescription).isEqualTo("Oct 1 – Dec 31, 2021")
    }

    @Test
    fun `when selection type is month to date, and today is last day of month, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-31")
        val sut = MONTH_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jul 1 – 31, 2022")
        assertThat(previousRangeDescription).isEqualTo("Jun 1 – 30, 2022")
    }

    @Test
    fun `when selection type is month to date, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-20")
        val sut = MONTH_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jul 1 – 20, 2022")
        assertThat(previousRangeDescription).isEqualTo("Jun 1 – 20, 2022")
    }

    @Test
    fun `when selection type is last month, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-31")
        val sut = LAST_MONTH.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jun 1 – 30, 2022")
        assertThat(previousRangeDescription).isEqualTo("May 1 – 31, 2022")
    }

    @Test
    fun `when selection type is week to date, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-29")
        val sut = WEEK_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jul 25 – 29, 2022")
        assertThat(previousRangeDescription).isEqualTo("Jul 18 – 22, 2022")
    }

    @Test
    fun `when selection type is week to date with different months, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-02")
        val sut = WEEK_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jun 27 – Jul 2, 2022")
        assertThat(previousRangeDescription).isEqualTo("Jun 20 – 25, 2022")
    }

    @Test
    fun `when selection type is last week, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-29")
        val sut = LAST_WEEK.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jul 18 – 24, 2022")
        assertThat(previousRangeDescription).isEqualTo("Jul 11 – 17, 2022")
    }

    @Test
    fun `when selection type is last week with different months, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-05")
        val sut = LAST_WEEK.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jun 27 – Jul 3, 2022")
        assertThat(previousRangeDescription).isEqualTo("Jun 20 – 26, 2022")
    }

    @Test
    fun `when selection type is today, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val sut = TODAY.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jul 1, 2022")
        assertThat(previousRangeDescription).isEqualTo("Jun 30, 2022")
    }

    @Test
    fun `when selection type is yesterday, then generate expected descriptions`() {
        // Given
        val today = midDayFrom("2022-07-02")
        val sut = YESTERDAY.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar,
            locale = testLocale,
            referenceEndDate = Date()
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Jul 1, 2022")
        assertThat(previousRangeDescription).isEqualTo("Jun 30, 2022")
    }

    @Test
    fun `when selection type is custom, then generate expected descriptions`() {
        // Given
        val start = dayStartFrom("2022-12-05")
        val end = dayEndFrom("2022-12-07")
        val sut = CUSTOM.generateSelectionData(
            referenceStartDate = start,
            referenceEndDate = end,
            calendar = testCalendar,
            locale = testLocale
        )

        // When
        val currentRangeDescription = sut.currentRangeDescription
        val previousRangeDescription = sut.previousRangeDescription

        // Then
        assertThat(currentRangeDescription).isEqualTo("Dec 5 – 7, 2022")
        assertThat(previousRangeDescription).isEqualTo("Dec 2 – 4, 2022")
    }

    private fun midDayFrom(date: String): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        formatter.timeZone = testTimeZone
        formatter.calendar = testCalendar
        return formatter.parse(date + "T12:00:00+0000")!!
    }

    private fun dayEndFrom(date: String): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        formatter.timeZone = testTimeZone
        val referenceDate = formatter.parse(date)!!
        testCalendar.time = referenceDate
        return testCalendar.endOfCurrentDay()
    }

    private fun dayStartFrom(date: String): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        formatter.timeZone = testTimeZone
        val referenceDate = formatter.parse(date)!!
        testCalendar.time = referenceDate
        return testCalendar.startOfCurrentDay()
    }
}
