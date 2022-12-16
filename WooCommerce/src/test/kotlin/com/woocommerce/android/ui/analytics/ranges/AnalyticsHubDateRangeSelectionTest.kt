package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.extensions.endOfToday
import com.woocommerce.android.extensions.startOfToday
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubRangeSelectionType.LAST_MONTH
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubRangeSelectionType.LAST_WEEK
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubRangeSelectionType.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubRangeSelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubRangeSelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubRangeSelectionType.YESTERDAY
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

internal class AnalyticsHubDateRangeSelectionTest {
    private lateinit var testTimeZone: TimeZone

    private lateinit var testCalendar: Calendar

    @Before
    fun setUp() {
        testTimeZone = TimeZone.getTimeZone("UTC")
        testCalendar = Calendar.getInstance(testTimeZone)
    }

    @Test
    fun `when selection type is last quarter, then generate expected date information`() {
        // Given
        val today = midDayFrom("2022-05-15")
        val expectedCurrentRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2012-01-01"),
            end = dayEndFrom("2022-03-31")
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2021-10-01"),
            end = dayEndFrom("2021-12-31")
        )

        // When
        val sut = AnalyticsHubDateRangeSelection(
            selectionType = LAST_MONTH,
            currentDate = today
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
        val sut = AnalyticsHubDateRangeSelection(
            selectionType = MONTH_TO_DATE,
            currentDate = today
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
        val sut = AnalyticsHubDateRangeSelection(
            selectionType = LAST_MONTH,
            currentDate = today
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
        val sut = AnalyticsHubDateRangeSelection(
            selectionType = WEEK_TO_DATE,
            currentDate = today
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
        val sut = AnalyticsHubDateRangeSelection(
            selectionType = LAST_WEEK,
            currentDate = today
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
        val sut = AnalyticsHubDateRangeSelection(
            selectionType = TODAY,
            currentDate = today
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
            end = midDayFrom("2022-06-30")
        )
        val expectedPreviousRange = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-06-29"),
            end = midDayFrom("2022-06-29")
        )

        // When
        val sut = AnalyticsHubDateRangeSelection(
            selectionType = YESTERDAY,
            currentDate = today
        )

        // Then
        assertThat(sut.currentRange).isEqualTo(expectedCurrentRange)
        assertThat(sut.previousRange).isEqualTo(expectedPreviousRange)
    }

    private fun midDayFrom(date: String): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        formatter.timeZone = testTimeZone
        formatter.calendar = testCalendar
        return formatter.parse(date)!!
    }

    private fun dayEndFrom(date: String): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        formatter.timeZone = testTimeZone
        val referenceDate = formatter.parse(date)!!
        testCalendar.time = referenceDate
        return testCalendar.endOfToday()
    }

    private fun dayStartFrom(date: String): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        formatter.timeZone = testTimeZone
        val referenceDate = formatter.parse(date)!!
        testCalendar.time = referenceDate
        return testCalendar.startOfToday()

    }
}
