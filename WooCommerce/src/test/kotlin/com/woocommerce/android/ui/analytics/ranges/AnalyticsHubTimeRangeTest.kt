package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.startOfCurrentDay
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal class AnalyticsHubTimeRangeTest {
    private lateinit var testTimeZone: TimeZone
    private lateinit var testCalendar: Calendar
    private lateinit var testLocale: Locale

    @Before
    fun setUp() {
        testLocale = Locale.US
        testTimeZone = TimeZone.getDefault()
        testCalendar = Calendar.getInstance(testLocale)
        testCalendar.timeZone = testTimeZone
        testCalendar.firstDayOfWeek = Calendar.MONDAY
    }

    @Test
    fun `when time range format is simplified then describes only first date`() {
        // Given
        val sut = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-07-01"),
            end = dayEndFrom("2022-07-02")
        )

        // When
        val description = sut.generateDescription(simplified = true, testLocale, testCalendar)

        // Then
        assertThat(description).isEqualTo("Jul 1, 2022")
    }

    @Test
    fun `when time range format is in different months then describes both dates`() {
        // Given
        val sut = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-07-01"),
            end = dayEndFrom("2022-08-02")
        )

        // When
        val description = sut.generateDescription(simplified = false, testLocale, testCalendar)

        // Then
        assertThat(description).isEqualTo("Jul 1 - Aug 2, 2022")
    }

    @Test
    fun `when time range is in the same month then describe month only in the start date`() {
        // Given
        val sut = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-07-01"),
            end = dayEndFrom("2022-07-05")
        )

        // When
        val description = sut.generateDescription(simplified = false, testLocale, testCalendar)

        // Then
        assertThat(description).isEqualTo("Jul 1 - 5, 2022")
    }

    @Test
    fun `when time range is in different years then fully describe both dates`() {
        // Given
        val sut = AnalyticsHubTimeRange(
            start = dayStartFrom("2021-04-15"),
            end = dayEndFrom("2022-04-15")
        )

        // When
        val description = sut.generateDescription(simplified = false, testLocale, testCalendar)

        // Then
        assertThat(description).isEqualTo("Apr 15, 2021 - Apr 15, 2022")
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
