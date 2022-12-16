package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.extensions.endOfToday
import com.woocommerce.android.extensions.startOfToday
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubRangeSelectionType.TODAY
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

internal class AnalyticsHubDateRangeSelectionTest {
    private val testTimeZone: TimeZone by lazy {
        TimeZone.getTimeZone("UTC")
    }

    private val testCalendar: Calendar by lazy {
        Calendar.getInstance(testTimeZone)
    }

    @Test
    fun `when selection type is today, then generate expected date information`() {
        val today = Date()
        val sut = AnalyticsHubDateRangeSelection(selectionType = TODAY)

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
        return testCalendar.startOfToday()
    }

    private fun dayStartFrom(date: String): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        formatter.timeZone = testTimeZone
        val referenceDate = formatter.parse(date)!!
        testCalendar.time = referenceDate
        return testCalendar.endOfToday()

    }
}
