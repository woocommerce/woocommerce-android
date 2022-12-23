package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_MONTH
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_QUARTER
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_WEEK
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.LAST_YEAR
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.QUARTER_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.YEAR_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType.YESTERDAY
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal class AnalyticsHubDateRangeSelectionTest {
    private lateinit var testTimeZone: TimeZone
    private lateinit var testCalendar: Calendar

    @Before
    fun setUp() {
        testTimeZone = TimeZone.getDefault()
        testCalendar = Calendar.getInstance(Locale.UK)
        testCalendar.timeZone = testTimeZone
        testCalendar.firstDayOfWeek = Calendar.MONDAY
    }

    @Test
    fun `when selection type is year to date, then generate expected date information`() {
        // Given
        val today = midDayFrom("2020-02-29")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2020-01-01"),
            end = today
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2019-01-01"),
            end = midDayFrom("2019-02-28")
        )

        // When
        val sut = YEAR_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is last year, then generate expected date information`() {
        // Given
        val today = midDayFrom("2020-02-29")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2019-01-01"),
            end = dayEndFrom("2019-12-31")
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2018-01-01"),
            end = dayEndFrom("2018-12-31")
        )

        // When
        val sut = LAST_YEAR.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is quarter to date, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-02-15")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-01-01"),
            end = today
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2021-10-01"),
            end = midDayFrom("2021-11-15")
        )

        // When
        val sut = QUARTER_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is last quarter, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-05-15")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-01-01"),
            end = dayEndFrom("2022-03-31")
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2021-10-01"),
            end = dayEndFrom("2021-12-31")
        )

        // When
        val sut = LAST_QUARTER.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is month to date, then generate expected date information`() {
        // Given
        val today = midDayFrom("2010-07-31")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2010-07-01"),
            end = today
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2010-06-01"),
            end = midDayFrom("2010-06-30")
        )

        // When
        val sut = MONTH_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is last month, then generate expected date information`() {
        // Given
        val today = midDayFrom("2010-07-15")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2010-06-01"),
            end = dayEndFrom("2010-06-30")
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2010-05-01"),
            end = dayEndFrom("2010-05-31")
        )

        // When
        val sut = LAST_MONTH.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is week to date, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-06-27"),
            end = today
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-06-20"),
            end = midDayFrom("2022-06-24")
        )

        // When
        val sut = WEEK_TO_DATE.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is last week, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-06-20"),
            end = dayEndFrom("2022-06-26")
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-06-13"),
            end = dayEndFrom("2022-06-19")
        )

        // When
        val sut = LAST_WEEK.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is today, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-07-01"),
            end = today
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-06-30"),
            end = midDayFrom("2022-06-30")
        )

        // When
        val sut = TODAY.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    @Test
    fun `when selection type is yesterday, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-07-01")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-06-30"),
            end = dayEndFrom("2022-06-30")
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-06-29"),
            end = dayEndFrom("2022-06-29")
        )

        // When
        val sut = YESTERDAY.generateSelectionData(
            referenceStartDate = today,
            calendar = testCalendar
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
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
