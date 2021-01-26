package com.woocommerce.android.util

import com.automattic.android.tracks.CrashLogging.CrashLoggingDataProvider
import com.automattic.android.tracks.TracksUser
import com.woocommerce.android.extensions.formatDateToFriendlyDayHour
import com.woocommerce.android.extensions.formatDateToFriendlyLongMonthDate
import com.woocommerce.android.extensions.formatDateToFriendlyLongMonthYear
import com.woocommerce.android.extensions.formatDateToWeeksInYear
import com.woocommerce.android.extensions.formatDateToYear
import com.woocommerce.android.extensions.formatDateToYearMonth
import com.woocommerce.android.extensions.formatToDateOnly
import com.woocommerce.android.extensions.formatToMonthDateOnly
import org.junit.Before
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DateUtilsTest {
    lateinit var dateUtilsUnderTest: DateUtils

    @Before
    fun setUp() {
        dateUtilsUnderTest = DateUtils(
            Locale.US,
            object : CrashLoggingDataProvider {
                override fun sentryDSN() = ""
                override fun getUserHasOptedOut() = false
                override fun buildType() = ""
                override fun releaseName() = ""
                override fun currentUser(): TracksUser? = null
                override fun applicationContext() = emptyMap<String, Any>()
                override fun userContext(): MutableMap<String, Any> = mutableMapOf()
                override fun locale(): Locale? = null
            }
        )
    }

    @Test
    fun `getNumberOfDaysInMonth() returns correct values`() {
        // General case
        assertEquals(31, dateUtilsUnderTest.getNumberOfDaysInMonth("2018-05-22"))

        // February
        assertEquals(28, dateUtilsUnderTest.getNumberOfDaysInMonth("2018-02-23"))

        // Leap year February
        assertEquals(29, dateUtilsUnderTest.getNumberOfDaysInMonth("2020-02-07"))

        // Year and month only
        assertEquals(29, dateUtilsUnderTest.getNumberOfDaysInMonth("2020-02"))
    }

    @Test
    fun `getNumberOfDaysInMonth() returns null on invalid string`() {
        assertNull(dateUtilsUnderTest.getNumberOfDaysInMonth("invalid"))
    }

    @Test
    fun `getShortMonthDayString() returns correct values`() {
        assertEquals("Jul 3", dateUtilsUnderTest.getShortMonthDayString("2018-07-03"))
        assertEquals("Jul 28", dateUtilsUnderTest.getShortMonthDayString("2018-07-28"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getShortMonthDayString("22"))

        assertNull(dateUtilsUnderTest.getShortMonthDayString("2018-22"))

        assertNull(dateUtilsUnderTest.getShortMonthDayString("-07-41"))

        assertNull(dateUtilsUnderTest.getShortMonthDayString(""))
    }

    @Test
    fun `getShortMonthDayStringForWeek() returns correct values`() {
        assertEquals("Mar 12", dateUtilsUnderTest.getShortMonthDayStringForWeek("2018-W11"))
        // Jan 1 2018 happened to be a Monday, so the first day of the first week happens to also be
        // the first day of the year
        assertEquals("Jan 1", dateUtilsUnderTest.getShortMonthDayStringForWeek("2018-W1"))
        assertEquals("Jan 2", dateUtilsUnderTest.getShortMonthDayStringForWeek("2017-W1"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getShortMonthDayString("22"))

        assertNull(dateUtilsUnderTest.getShortMonthDayString("2018-22"))

        assertNull(dateUtilsUnderTest.getShortMonthDayString("-07-41"))

        assertNull(dateUtilsUnderTest.getShortMonthDayString(""))
    }

    @Test
    fun `getShortMonthString() returns correct values`() {
        assertEquals("Jul", dateUtilsUnderTest.getShortMonthString("2018-07"))
        assertEquals("Jan", dateUtilsUnderTest.getShortMonthString("2017-01"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getShortMonthDayString("22"))

        assertNull(dateUtilsUnderTest.getShortMonthDayString("2018-22"))

        assertNull(dateUtilsUnderTest.getShortMonthDayString("-07-41"))

        assertNull(dateUtilsUnderTest.getShortMonthDayString(""))
    }

    @Test
    fun `getDateString() returns correct values`() {
        assertEquals("2019-05-09", dateUtilsUnderTest.getDateString("May 9, 2019"))
        assertEquals("2018-12-31", dateUtilsUnderTest.getDateString("Dec 31, 2018"))
        assertEquals("2019-01-01", dateUtilsUnderTest.getDateString("Jan 01, 2019"))
        assertEquals("2019-02-28", dateUtilsUnderTest.getDateString("Feb 28, 2019"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getDateString("Dec 30 2018"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getDateString("2019-12-31"))

        assertNull(dateUtilsUnderTest.getDateString("-07-41"))

        assertNull(dateUtilsUnderTest.getDateString(""))
    }

    @Test
    fun `getShortHourString() returns correct values`() {
        assertEquals("12am", dateUtilsUnderTest.getShortHourString("2019-05-09 00"))
        assertEquals("12pm", dateUtilsUnderTest.getShortHourString("2019-05-09 12"))
        assertEquals("1am", dateUtilsUnderTest.getShortHourString("2018-12-31 01"))
        assertEquals("5am", dateUtilsUnderTest.getShortHourString("2019-07-15 05"))
        assertEquals("2pm", dateUtilsUnderTest.getShortHourString("2019-01-01 14"))
        assertEquals("11pm", dateUtilsUnderTest.getShortHourString("2019-02-28 23"))
        assertEquals("4pm", dateUtilsUnderTest.getShortHourString("2019-02-28 16"))
        assertEquals("9am", dateUtilsUnderTest.getShortHourString("2019-02-28 09"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getShortHourString("Dec 30 2018"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getShortHourString("2019-12-31"))

        assertNull(dateUtilsUnderTest.getShortHourString("-07-41"))

        assertNull(dateUtilsUnderTest.getShortHourString(""))

        assertNull(dateUtilsUnderTest.getShortHourString("5am"))
    }

    @Test
    fun `getShortMonthYearString() returns correct values`() {
        assertEquals("May 2019", dateUtilsUnderTest.getShortMonthYearString("2019-05"))
        assertEquals("Dec 2018", dateUtilsUnderTest.getShortMonthYearString("2018-12"))
        assertEquals("Jan 2019", dateUtilsUnderTest.getShortMonthYearString("2019-01"))
        assertEquals("Feb 2019", dateUtilsUnderTest.getShortMonthYearString("2019-02"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getShortMonthYearString("Dec 30 2018"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getShortMonthYearString(""))

        assertNull(dateUtilsUnderTest.getShortMonthYearString("22"))
    }

    @Test
    fun `getDayMonthDateString() returns correct values`() {
        assertEquals("Wednesday, May 1", dateUtilsUnderTest.getDayMonthDateString("2019-05-01 12"))
        assertEquals("Tuesday, Dec 4", dateUtilsUnderTest.getDayMonthDateString("2018-12-04 14"))
        assertEquals("Tuesday, Jan 22", dateUtilsUnderTest.getDayMonthDateString("2019-01-22 00"))
        assertEquals("Saturday, Feb 23", dateUtilsUnderTest.getDayMonthDateString("2019-02-23 23"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getShortMonthYearString("Dec 30 2018"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getShortMonthYearString(""))

        assertNull(dateUtilsUnderTest.getShortMonthYearString("22"))
    }

    @Test
    fun `getMonthString() returns correct values`() {
        assertEquals("May", dateUtilsUnderTest.getMonthString("2019-05-01"))
        assertEquals("December", dateUtilsUnderTest.getMonthString("2018-12-04"))
        assertEquals("January", dateUtilsUnderTest.getMonthString("2019-01-22"))
        assertEquals("February", dateUtilsUnderTest.getMonthString("2019-02-23"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getMonthString("Dec 30 2018"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getMonthString(""))

        assertNull(dateUtilsUnderTest.getMonthString("22"))
    }

    @Test
    fun `getYearString() returns correct values`() {
        assertEquals("2019", dateUtilsUnderTest.getYearString("2019-05-01"))
        assertEquals("2018", dateUtilsUnderTest.getYearString("2018-12-04"))
        assertEquals("2019", dateUtilsUnderTest.getYearString("2019-01-22"))
        assertEquals("2019", dateUtilsUnderTest.getYearString("2019-02-23"))
        assertEquals("2017", dateUtilsUnderTest.getYearString("2017-10"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getYearString("Dec 30 2018"))

        assertNull(dateUtilsUnderTest.getYearString("2019"))

        // Test for invalid value handling
        assertNull(dateUtilsUnderTest.getYearString(""))

        assertNull(dateUtilsUnderTest.getYearString("22"))
    }

    @Test
    fun `formatDateToWeeksInYear() returns correct values`() {
        assertEquals("2019-W32", "2019W08W08".formatDateToWeeksInYear(Locale.US))
        assertEquals("2019-W27", "2019W07W01".formatDateToWeeksInYear(Locale.US))
        assertEquals("2019-W26", "2019W06W24".formatDateToWeeksInYear(Locale.US))
        assertEquals("2019-W01", "2019W01W04".formatDateToWeeksInYear(Locale.US))
        assertEquals("2018-W01", "2018W12W31".formatDateToWeeksInYear(Locale.US))
        assertEquals("2018-W52", "2018W12W28".formatDateToWeeksInYear(Locale.US))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2018W12W".formatDateToWeeksInYear(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToWeeksInYear(Locale.US)
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToWeeksInYear(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatDateToWeeksInYear(Locale.US)
        }
    }

    @Test
    fun `formatDateToYear() returns correct values`() {
        assertEquals("2019", "2019-08-02".formatDateToYear(Locale.US))
        assertEquals("2019", "2019-01-02".formatDateToYear(Locale.US))
        assertEquals("2019", "2019-06-04".formatDateToYear(Locale.US))
        assertEquals("2019", "2019-04-11".formatDateToYear(Locale.US))
        assertEquals("2018", "2018-12-22".formatDateToYear(Locale.US))
        assertEquals("2018", "2018-11-12".formatDateToYear(Locale.US))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019-08".formatDateToYear(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToYear(Locale.US)
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToYear(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatDateToYear(Locale.US)
        }
    }

    @Test
    fun `formatDateToYearMonth() returns correct values`() {
        assertEquals("2019-08", "2019-08-02".formatDateToYearMonth(Locale.US))
        assertEquals("2019-01", "2019-01-02".formatDateToYearMonth(Locale.US))
        assertEquals("2019-06", "2019-06-04".formatDateToYearMonth(Locale.US))
        assertEquals("2019-04", "2019-04-11".formatDateToYearMonth(Locale.US))
        assertEquals("2018-12", "2018-12-22".formatDateToYearMonth(Locale.US))
        assertEquals("2018-11", "2018-11-12".formatDateToYearMonth(Locale.US))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019-08".formatDateToYearMonth(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatDateToYearMonth(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToYearMonth(Locale.US)
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToYearMonth(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatDateToYearMonth(Locale.US)
        }
    }

    @Test
    fun `formatDateToDayHour() returns correct values`() {
        assertEquals("Thursday, Aug 08 › 7am", "2019-08-08 07".formatDateToFriendlyDayHour(Locale.US))
        assertEquals("Thursday, Aug 08 › 11pm", "2019-08-08 23".formatDateToFriendlyDayHour(Locale.US))
        assertEquals("Wednesday, Jan 02 › 12am", "2019-01-02 00".formatDateToFriendlyDayHour(Locale.US))
        assertEquals("Tuesday, Jun 04 › 1am", "2019-06-04 01".formatDateToFriendlyDayHour(Locale.US))
        assertEquals("Monday, Sep 09 › 1pm", "2019-09-09 13".formatDateToFriendlyDayHour(Locale.US))
        assertEquals("Saturday, Dec 22 › 5pm", "2018-12-22 17".formatDateToFriendlyDayHour(Locale.US))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatDateToFriendlyDayHour(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToFriendlyDayHour(Locale.US)
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToFriendlyDayHour(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatDateToFriendlyDayHour(Locale.US)
        }
    }

    @Test
    fun `formatDateToFriendlyLongMonth() returns correct values`() {
        assertEquals("2019 › August", "2019-08-02".formatDateToFriendlyLongMonthYear(Locale.US))
        assertEquals("2019 › January", "2019-01-02".formatDateToFriendlyLongMonthYear(Locale.US))
        assertEquals("2019 › June", "2019-06-04".formatDateToFriendlyLongMonthYear(Locale.US))
        assertEquals("2019 › September", "2019-09-11".formatDateToFriendlyLongMonthYear(Locale.US))
        assertEquals("2018 › December", "2018-12-22".formatDateToFriendlyLongMonthYear(Locale.US))
        assertEquals("2018 › November", "2018-11-12".formatDateToFriendlyLongMonthYear(Locale.US))
        assertEquals("2018 › August", "2018-08".formatDateToFriendlyLongMonthYear(Locale.US))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatDateToFriendlyLongMonthYear(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToFriendlyLongMonthYear(Locale.US)
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToFriendlyLongMonthYear(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatDateToFriendlyLongMonthYear(Locale.US)
        }
    }

    @Test
    fun `formatDateToFriendlyLongMonthDate() returns correct values`() {
        assertEquals("August 08", "2019-08-08".formatDateToFriendlyLongMonthDate(Locale.US))
        assertEquals("February 23", "2019-02-23".formatDateToFriendlyLongMonthDate(Locale.US))
        assertEquals("January 02", "2019-01-02".formatDateToFriendlyLongMonthDate(Locale.US))
        assertEquals("June 04", "2019-06-04".formatDateToFriendlyLongMonthDate(Locale.US))
        assertEquals("September 09", "2019-09-09".formatDateToFriendlyLongMonthDate(Locale.US))
        assertEquals("December 22", "2018-12-22".formatDateToFriendlyLongMonthDate(Locale.US))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatDateToFriendlyLongMonthDate(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatDateToFriendlyLongMonthDate(Locale.US)
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatDateToFriendlyLongMonthDate(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatDateToFriendlyLongMonthDate(Locale.US)
        }
    }

    @Test
    fun `formatToDateOnly() returns correct values`() {
        assertEquals("8", "2019-08-08".formatToDateOnly(Locale.US))
        assertEquals("23", "2019-02-23".formatToDateOnly(Locale.US))
        assertEquals("2", "2019-01-02".formatToDateOnly(Locale.US))
        assertEquals("4", "2019-06-04".formatToDateOnly(Locale.US))
        assertEquals("9", "2019-09-09".formatToDateOnly(Locale.US))
        assertEquals("22", "2018-12-22".formatToDateOnly(Locale.US))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatToDateOnly(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatToDateOnly(Locale.US)
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatToDateOnly(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatToDateOnly(Locale.US)
        }
    }

    @Test
    fun `formatToMonthDateOnly() returns correct values`() {
        assertEquals("Aug 8", "2019-08-08".formatToMonthDateOnly(Locale.US))
        assertEquals("Feb 23", "2019-02-23".formatToMonthDateOnly(Locale.US))
        assertEquals("Jan 2", "2019-01-02".formatToMonthDateOnly(Locale.US))
        assertEquals("Jun 4", "2019-06-04".formatToMonthDateOnly(Locale.US))
        assertEquals("Sep 9", "2019-09-09".formatToMonthDateOnly(Locale.US))
        assertEquals("Dec 22", "2018-12-22".formatToMonthDateOnly(Locale.US))

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "2019".formatToMonthDateOnly(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "20-W12".formatToMonthDateOnly(Locale.US)
        }

        // Test for invalid value handling
        assertFailsWith(IllegalArgumentException::class) {
            "".formatToMonthDateOnly(Locale.US)
        }

        assertFailsWith(IllegalArgumentException::class) {
            "21".formatToMonthDateOnly(Locale.US)
        }
    }
}
