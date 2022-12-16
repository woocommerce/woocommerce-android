package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.extensions.endOfToday
import com.woocommerce.android.extensions.startOfToday
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubRangeSelectionType.TODAY
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
        val sut = AnalyticsHubDateRangeSelection(selectionType = TODAY)

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
