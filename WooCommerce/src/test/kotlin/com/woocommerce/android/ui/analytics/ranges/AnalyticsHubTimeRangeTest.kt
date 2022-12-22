package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.startOfCurrentDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before

internal class AnalyticsHubTimeRangeTest {
    private lateinit var testTimeZone: TimeZone
    private lateinit var testCalendar: Calendar

    @Before
    fun setUp() {
        testTimeZone = TimeZone.getTimeZone("UTC")
        testCalendar = Calendar.getInstance(Locale.UK)
        testCalendar.timeZone = testTimeZone
        testCalendar.firstDayOfWeek = Calendar.MONDAY
    }

//    func test_when_time_range_format_is_simplified_then_describes_only_first_date() {
//        // Given
//        let startDate = startDate(from: "2022-07-01")!
//        let endDate = endDate(from: "2022-07-02")!
//        let timeRange = AnalyticsHubTimeRange(start: startDate, end: endDate)
//
//        // When
//        let description = timeRange.formatToString(simplified: true, timezone: testTimezone, calendar: testCalendar)
//
//        //Then
//        XCTAssertEqual(description, "Jul 1, 2022")
//    }

    fun `when time range format is simplified then describes only first date`() {
        // Given
        val sut = AnalyticsHubTimeRange(
            start = dayStartFrom("2022-07-01"),
            end = dayEndFrom("2022-07-02")
        )

        // When
        val description = sut.generateDescription(simplified = true)

        //Then
        assertThat(description).isEqualTo("Jul 1, 2022")
    }
//
//    func test_when_time_range_format_is_in_different_months_then_describes_both_dates() {
//        // Given
//        let startDate = startDate(from: "2022-07-01")!
//        let endDate = endDate(from: "2022-08-02")!
//        let timeRange = AnalyticsHubTimeRange(start: startDate, end: endDate)
//
//        // When
//        let description = timeRange.formatToString(simplified: false, timezone: testTimezone, calendar: testCalendar)
//
//        //Then
//        XCTAssertEqual(description, "Jul 1 - Aug 2, 2022")
//    }
//
//    func test_when_time_range_is_in_the_same_month_then_describe_month_only_in_the_start_date() {
//        // Given
//        let startDate = startDate(from: "2022-07-01")!
//        let endDate = endDate(from: "2022-07-05")!
//        let timeRange = AnalyticsHubTimeRange(start: startDate, end: endDate)
//
//        // When
//        let description = timeRange.formatToString(simplified: false, timezone: testTimezone, calendar: testCalendar)
//
//        //Then
//        XCTAssertEqual(description, "Jul 1 - 5, 2022")
//    }
//
//    func test_when_time_range_is_in_different_years_then_fully_describe_both_dates() {
//        // Given
//        let startDate = startDate(from: "2021-04-15")!
//        let endDate = endDate(from: "2022-04-15")!
//        let timeRange = AnalyticsHubTimeRange(start: startDate, end: endDate)
//
//        // When
//        let description = timeRange.formatToString(simplified: false, timezone: testTimezone, calendar: testCalendar)
//
//        //Then
//        XCTAssertEqual(description, "Apr 15, 2021 - Apr 15, 2022")
//    }

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
