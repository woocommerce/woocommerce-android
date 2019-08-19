package com.woocommerce.android.util

import com.woocommerce.android.extensions.formatDateToFriendlyDayHour
import com.woocommerce.android.extensions.formatDateToFriendlyLongMonthDate
import com.woocommerce.android.extensions.formatDateToFriendlyLongMonthYear
import com.woocommerce.android.extensions.formatDateToWeeksInYear
import com.woocommerce.android.extensions.formatDateToYear
import com.woocommerce.android.extensions.formatDateToYearMonth
import com.woocommerce.android.extensions.formatToDateOnly
import com.woocommerce.android.extensions.formatToMonthDateOnly
import org.junit.Test
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
        assertEquals("2018-W52", "2018W12W28".formatDateToWeeksInYear())

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2018W12W".formatDateToWeeksInYear()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToWeeksInYear()
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToWeeksInYear()
        }

        assertFailsWith(IllegalArgumentException::class) {
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
        assertFailsWith(IllegalArgumentException::class) {
            "2019-08".formatDateToYear()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToYear()
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToYear()
        }

        assertFailsWith(IllegalArgumentException::class) {
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
        assertFailsWith(IllegalArgumentException::class) {
            "2019-08".formatDateToYearMonth()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatDateToYearMonth()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToYearMonth()
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToYearMonth()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatDateToYearMonth()
        }
    }

    @Test
    fun `formatDateToDayHour() returns correct values`() {
        assertEquals("Thursday, Aug 08 › 7am", "2019-08-08 07".formatDateToFriendlyDayHour())
        assertEquals("Thursday, Aug 08 › 11pm", "2019-08-08 23".formatDateToFriendlyDayHour())
        assertEquals("Wednesday, Jan 02 › 12am", "2019-01-02 00".formatDateToFriendlyDayHour())
        assertEquals("Tuesday, Jun 04 › 1am", "2019-06-04 01".formatDateToFriendlyDayHour())
        assertEquals("Monday, Sep 09 › 1pm", "2019-09-09 13".formatDateToFriendlyDayHour())
        assertEquals("Saturday, Dec 22 › 5pm", "2018-12-22 17".formatDateToFriendlyDayHour())

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatDateToFriendlyDayHour()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToFriendlyDayHour()
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToFriendlyDayHour()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatDateToFriendlyDayHour()
        }
    }

    @Test
    fun `formatDateToFriendlyLongMonth() returns correct values`() {
        assertEquals("2019 › August", "2019-08-02".formatDateToFriendlyLongMonthYear())
        assertEquals("2019 › January", "2019-01-02".formatDateToFriendlyLongMonthYear())
        assertEquals("2019 › June", "2019-06-04".formatDateToFriendlyLongMonthYear())
        assertEquals("2019 › September", "2019-09-11".formatDateToFriendlyLongMonthYear())
        assertEquals("2018 › December", "2018-12-22".formatDateToFriendlyLongMonthYear())
        assertEquals("2018 › November", "2018-11-12".formatDateToFriendlyLongMonthYear())
        assertEquals("2018 › August", "2018-08".formatDateToFriendlyLongMonthYear())

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatDateToFriendlyLongMonthYear()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToFriendlyLongMonthYear()
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToFriendlyLongMonthYear()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatDateToFriendlyLongMonthYear()
        }
    }

    @Test
    fun `formatDateToFriendlyLongMonthDate() returns correct values`() {
        assertEquals("August 08", "2019-08-08".formatDateToFriendlyLongMonthDate())
        assertEquals("February 23", "2019-02-23".formatDateToFriendlyLongMonthDate())
        assertEquals("January 02", "2019-01-02".formatDateToFriendlyLongMonthDate())
        assertEquals("June 04", "2019-06-04".formatDateToFriendlyLongMonthDate())
        assertEquals("September 09", "2019-09-09".formatDateToFriendlyLongMonthDate())
        assertEquals("December 22", "2018-12-22".formatDateToFriendlyLongMonthDate())

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatDateToFriendlyLongMonthDate()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToFriendlyLongMonthDate()
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToFriendlyLongMonthDate()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatDateToFriendlyLongMonthDate()
        }
    }

    @Test
    fun `formatToDateOnly() returns correct values`() {
        assertEquals("8", "2019-08-08".formatToDateOnly())
        assertEquals("23", "2019-02-23".formatToDateOnly())
        assertEquals("2", "2019-01-02".formatToDateOnly())
        assertEquals("4", "2019-06-04".formatToDateOnly())
        assertEquals("9", "2019-09-09".formatToDateOnly())
        assertEquals("22", "2018-12-22".formatToDateOnly())

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatToDateOnly()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatToDateOnly()
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatToDateOnly()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatToDateOnly()
        }
    }

    @Test
    fun `formatToMonthDateOnly() returns correct values`() {
        assertEquals("Aug 8", "2019-08-08".formatToMonthDateOnly())
        assertEquals("Feb 23", "2019-02-23".formatToMonthDateOnly())
        assertEquals("Jan 2", "2019-01-02".formatToMonthDateOnly())
        assertEquals("Jun 4", "2019-06-04".formatToMonthDateOnly())
        assertEquals("Sep 9", "2019-09-09".formatToMonthDateOnly())
        assertEquals("Dec 22", "2018-12-22".formatToMonthDateOnly())

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatToMonthDateOnly()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatToMonthDateOnly()
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatToMonthDateOnly()
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatToMonthDateOnly()
        }
    }
}
