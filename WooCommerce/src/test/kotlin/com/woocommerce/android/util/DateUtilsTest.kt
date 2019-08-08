package com.woocommerce.android.util

import com.woocommerce.android.extensions.formatDateToWeeksInYear
import com.woocommerce.android.extensions.formatDateToYear
import com.woocommerce.android.extensions.formatDateToYearMonth
import org.junit.Test
import java.lang.IndexOutOfBoundsException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateUtilsTest {
    @Test
    fun `getNumberOfDaysInMonth() returns correct values`() {
        // General case
        assertEquals(31, DateUtils.getNumberOfDaysInMonth("2018-05-22"))

        // February
        assertEquals(28, DateUtils.getNumberOfDaysInMonth("2018-02-23"))

        // Leap year February
        assertEquals(29, DateUtils.getNumberOfDaysInMonth("2020-02-07"))

        // Year and month only
        assertEquals(29, DateUtils.getNumberOfDaysInMonth("2020-02"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getNumberOfDaysInMonth() throws exception on invalid string`() {
        assertEquals(29, DateUtils.getNumberOfDaysInMonth("invalid"))
    }

    @Test
    fun `getShortMonthDayString() returns correct values`() {
        assertEquals("Jul 3", DateUtils.getShortMonthDayString("2018-07-03"))
        assertEquals("Jul 28", DateUtils.getShortMonthDayString("2018-07-28"))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("22")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("2018-22")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("-07-41")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("")
        }
    }

    @Test
    fun `getShortMonthDayStringForWeek() returns correct values`() {
        assertEquals("Mar 12", DateUtils.getShortMonthDayStringForWeek("2018-W11"))
        // Jan 1 2018 happened to be a Monday, so the first day of the first week happens to also be
        // the first day of the year
        assertEquals("Jan 1", DateUtils.getShortMonthDayStringForWeek("2018-W1"))
        assertEquals("Jan 2", DateUtils.getShortMonthDayStringForWeek("2017-W1"))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("22")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("2018-22")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("-07-41")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("")
        }
    }

    @Test
    fun `getShortMonthString() returns correct values`() {
        assertEquals("Jul", DateUtils.getShortMonthString("2018-07"))
        assertEquals("Jan", DateUtils.getShortMonthString("2017-01"))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("22")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("2018-22")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("-07-41")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthDayString("")
        }
    }

    @Test
    fun `getDateString() returns correct values`() {
        assertEquals("2019-05-09", DateUtils.getDateString("May 9, 2019"))
        assertEquals("2018-12-31", DateUtils.getDateString("Dec 31, 2018"))
        assertEquals("2019-01-01", DateUtils.getDateString("Jan 01, 2019"))
        assertEquals("2019-02-28", DateUtils.getDateString("Feb 28, 2019"))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getDateString("Dec 30 2018")
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getDateString("2019-12-31")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getDateString("-07-41")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getDateString("")
        }
    }

    @Test
    fun `getShortHourString() returns correct values`() {
        assertEquals("12am", DateUtils.getShortHourString("2019-05-09 00"))
        assertEquals("12pm", DateUtils.getShortHourString("2019-05-09 12"))
        assertEquals("1am", DateUtils.getShortHourString("2018-12-31 01"))
        assertEquals("5am", DateUtils.getShortHourString("2019-07-15 05"))
        assertEquals("2pm", DateUtils.getShortHourString("2019-01-01 14"))
        assertEquals("11pm", DateUtils.getShortHourString("2019-02-28 23"))
        assertEquals("4pm", DateUtils.getShortHourString("2019-02-28 16"))
        assertEquals("9am", DateUtils.getShortHourString("2019-02-28 09"))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortHourString("Dec 30 2018")
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortHourString("2019-12-31")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortHourString("-07-41")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortHourString("")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortHourString("5am")
        }
    }

    @Test
    fun `getShortMonthYearString() returns correct values`() {
        assertEquals("May 2019", DateUtils.getShortMonthYearString("2019-05"))
        assertEquals("Dec 2018", DateUtils.getShortMonthYearString("2018-12"))
        assertEquals("Jan 2019", DateUtils.getShortMonthYearString("2019-01"))
        assertEquals("Feb 2019", DateUtils.getShortMonthYearString("2019-02"))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthYearString("Dec 30 2018")
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthYearString("")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthYearString("22")
        }
    }

    @Test
    fun `getDayMonthDateString() returns correct values`() {
        assertEquals("Wednesday, May 1", DateUtils.getDayMonthDateString("2019-05-01 12"))
        assertEquals("Tuesday, Dec 4", DateUtils.getDayMonthDateString("2018-12-04 14"))
        assertEquals("Tuesday, Jan 22", DateUtils.getDayMonthDateString("2019-01-22 00"))
        assertEquals("Saturday, Feb 23", DateUtils.getDayMonthDateString("2019-02-23 23"))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthYearString("Dec 30 2018")
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthYearString("")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getShortMonthYearString("22")
        }
    }

    @Test
    fun `getMonthString() returns correct values`() {
        assertEquals("May", DateUtils.getMonthString("2019-05-01"))
        assertEquals("December", DateUtils.getMonthString("2018-12-04"))
        assertEquals("January", DateUtils.getMonthString("2019-01-22"))
        assertEquals("February", DateUtils.getMonthString("2019-02-23"))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getMonthString("Dec 30 2018")
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getMonthString("")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getMonthString("22")
        }
    }

    @Test
    fun `getYearString() returns correct values`() {
        assertEquals("2019", DateUtils.getYearString("2019-05-01"))
        assertEquals("2018", DateUtils.getYearString("2018-12-04"))
        assertEquals("2019", DateUtils.getYearString("2019-01-22"))
        assertEquals("2019", DateUtils.getYearString("2019-02-23"))
        assertEquals("2017", DateUtils.getYearString("2017-10"))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getYearString("Dec 30 2018")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getYearString("2019")
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getYearString("")
        }

        assertFailsWith(IllegalArgumentException::class) {
            DateUtils.getYearString("22")
        }
    }

    @Test
    fun `formatDateToWeeksInYear() returns correct values`() {
        assertEquals("2019-W32", "2019W08W08".formatDateToWeeksInYear())
        assertEquals("2019-W27", "2019W07W01".formatDateToWeeksInYear())
        assertEquals("2019-W26", "2019W06W24".formatDateToWeeksInYear())
        assertEquals("2019-W01", "2019W01W04".formatDateToWeeksInYear())
        assertEquals("2018-W01", "2018W12W31".formatDateToWeeksInYear())
        assertEquals("2018-W52", "2018W12W30".formatDateToWeeksInYear())

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2018W12W".formatDateToWeeksInYear()
        }

        assertFailsWith(IndexOutOfBoundsException::class) {
            "20-W12".formatDateToWeeksInYear()
        }

        // Test for invalid value handling
        assertFailsWith(IndexOutOfBoundsException::class) {
            "".formatDateToWeeksInYear()
        }

        assertFailsWith(IndexOutOfBoundsException::class) {
            "21".formatDateToWeeksInYear()
        }
    }

    @Test
    fun `formatDateToYear() returns correct values`() {
        assertEquals("2019", "2019-08-02".formatDateToYear())
        assertEquals("2019", "2019-01-02".formatDateToYear())
        assertEquals("2019", "2019-06-04".formatDateToYear())
        assertEquals("2019", "2019-04-11".formatDateToYear())
        assertEquals("2018", "2018-12-22".formatDateToYear())
        assertEquals("2018", "2018-11-12".formatDateToYear())

        // Test for invalid value handling
        assertFailsWith(IndexOutOfBoundsException::class) {
            "2019-08".formatDateToYear()
        }

        assertFailsWith(IndexOutOfBoundsException::class) {
            "20-W12".formatDateToYear()
        }

        // Test for invalid value handling
        assertFailsWith(IndexOutOfBoundsException::class) {
            "".formatDateToYear()
        }

        assertFailsWith(IndexOutOfBoundsException::class) {
            "21".formatDateToYear()
        }
    }

    @Test
    fun `formatDateToYearMonth() returns correct values`() {
        assertEquals("2019-08", "2019-08-02".formatDateToYearMonth())
        assertEquals("2019-01", "2019-01-02".formatDateToYearMonth())
        assertEquals("2019-06", "2019-06-04".formatDateToYearMonth())
        assertEquals("2019-04", "2019-04-11".formatDateToYearMonth())
        assertEquals("2018-12", "2018-12-22".formatDateToYearMonth())
        assertEquals("2018-11", "2018-11-12".formatDateToYearMonth())

        // Test for invalid value handling
        assertFailsWith(IndexOutOfBoundsException::class) {
            "2019-08".formatDateToYearMonth()
        }

        assertFailsWith(IndexOutOfBoundsException::class) {
            "2019".formatDateToYearMonth()
        }

        assertFailsWith(IndexOutOfBoundsException::class) {
            "20-W12".formatDateToYearMonth()
        }

        // Test for invalid value handling
        assertFailsWith(IndexOutOfBoundsException::class) {
            "".formatDateToYearMonth()
        }

        assertFailsWith(IndexOutOfBoundsException::class) {
            "21".formatDateToYearMonth()
        }
    }
}
