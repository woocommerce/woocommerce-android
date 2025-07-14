package org.wordpress.android.fluxc.wc.utils

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.fluxc.utils.SiteUtils
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals

class DateUtilsTest {
    companion object {
        private const val DATE_FORMAT_DAY = "yyyy-MM-dd"
        private const val DATE_TIME_FORMAT_START = "yyyy-MM-dd'T'00:00:00"
    }

    @Test
    fun testGetDateFromDateString() {
        val dateFormat = SimpleDateFormat(DATE_FORMAT_DAY, Locale.ROOT)

        val d1 = "2018-01-25"
        val date1 = DateUtils.getDateFromString(d1)
        assertEquals(dateFormat.parse(d1), date1)

        val d2 = "2018-01-28"
        val date2 = DateUtils.getDateFromString(d2)
        assertEquals(dateFormat.parse(d2), date2)

        val d3 = "2018-01-01"
        val date3 = DateUtils.getDateFromString(d3)
        assertEquals(dateFormat.parse(d3), date3)
    }

    @Test
    fun testFormatDateFromString() {
        val d1 = DateUtils.formatDate(DATE_FORMAT_DAY, Date())

        val site = SiteModel().apply { id = 1 }
        val date1 = DateUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, d1)
        assertEquals(d1, date1)

        site.timezone = "12"
        val date2 = DateUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, d1)
        assertEquals(SiteUtils.getCurrentDateTimeForSite(site, "yyyy-MM-dd"), date2)

        site.timezone = "-12"
        val date3 = DateUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, d1)
        assertEquals(SiteUtils.getCurrentDateTimeForSite(site, "yyyy-MM-dd"), date3)

        site.timezone = "0"
        val date4 = DateUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, d1)
        assertEquals(d1, date4)

        site.timezone = ""
        val date5 = DateUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, d1)
        assertEquals(d1, date5)
    }

    @Test
    fun testGetQuantityInDays() {
        val startDate = DateUtils.getDateFromString("2018-01-25")
        val endDate = DateUtils.getDateFromString("2018-01-28")
        val startDateCalendar = DateUtils.getStartDateCalendar(startDate)
        val endDateCalendar = DateUtils.getEndDateCalendar(endDate)
        val quantity = DateUtils.getQuantityInDays(startDateCalendar, endDateCalendar)
        assertEquals(4, quantity)

        val startDate2 = DateUtils.getDateFromString("2018-01-25")
        val endDate2 = DateUtils.getDateFromString("2018-01-25")
        val startDateCalendar2 = DateUtils.getStartDateCalendar(startDate2)
        val endDateCalendar2 = DateUtils.getEndDateCalendar(endDate2)
        val quantity2 = DateUtils.getQuantityInDays(startDateCalendar2, endDateCalendar2)
        assertEquals(1, quantity2)

        val startDate3 = DateUtils.getDateFromString("2018-01-01")
        val endDate3 = DateUtils.getDateFromString("2018-01-31")
        val startDateCalendar3 = DateUtils.getStartDateCalendar(startDate3)
        val endDateCalendar3 = DateUtils.getEndDateCalendar(endDate3)
        val quantity3 = DateUtils.getQuantityInDays(startDateCalendar3, endDateCalendar3)
        assertEquals(31, quantity3)
    }

    @Test
    fun testGetQuantityInWeeksUSLocale() {
        val startDate = DateUtils.getDateFromString("2019-01-13")
        val endDate = DateUtils.getDateFromString("2019-01-20")
        val startDateCalendar = DateUtils.getStartDateCalendar(startDate, Locale.US)
        val endDateCalendar = DateUtils.getEndDateCalendar(endDate, Locale.US)
        val quantity = DateUtils.getQuantityInWeeks(startDateCalendar, endDateCalendar)
        assertEquals(2, quantity)

        val startDate2 = DateUtils.getDateFromString("2018-12-01")
        val endDate2 = DateUtils.getDateFromString("2018-12-31")
        val startDateCalendar2 = DateUtils.getStartDateCalendar(startDate2, Locale.US)
        val endDateCalendar2 = DateUtils.getEndDateCalendar(endDate2, Locale.US)
        val quantity2 = DateUtils.getQuantityInWeeks(startDateCalendar2, endDateCalendar2)
        assertEquals(6, quantity2)

        val startDate3 = DateUtils.getDateFromString("2018-10-22")
        val endDate3 = DateUtils.getDateFromString("2018-10-22")
        val startDateCalendar3 = DateUtils.getStartDateCalendar(startDate3, Locale.US)
        val endDateCalendar3 = DateUtils.getEndDateCalendar(endDate3, Locale.US)
        val quantity3 = DateUtils.getQuantityInWeeks(startDateCalendar3, endDateCalendar3)
        assertEquals(1, quantity3)

        val startDate4 = DateUtils.getDateFromString("2019-01-14")
        val endDate4 = DateUtils.getDateFromString("2019-01-20")
        val startDateCalendar4 = DateUtils.getStartDateCalendar(startDate4, Locale.US)
        val endDateCalendar4 = DateUtils.getEndDateCalendar(endDate4, Locale.US)
        val quantity4 = DateUtils.getQuantityInWeeks(startDateCalendar4, endDateCalendar4)
        assertEquals(2, quantity4)
    }

    @Test
    fun testGetQuantityInWeeksFranceLocale() {
        val startDate = DateUtils.getDateFromString("2019-01-13")
        val endDate = DateUtils.getDateFromString("2019-01-20")
        val startDateCalendar = DateUtils.getStartDateCalendar(startDate, Locale.FRANCE)
        val endDateCalendar = DateUtils.getEndDateCalendar(endDate, Locale.FRANCE)
        val quantity = DateUtils.getQuantityInWeeks(startDateCalendar, endDateCalendar)
        assertEquals(2, quantity)

        val startDate2 = DateUtils.getDateFromString("2018-12-01")
        val endDate2 = DateUtils.getDateFromString("2018-12-31")
        val startDateCalendar2 = DateUtils.getStartDateCalendar(startDate2, Locale.FRANCE)
        val endDateCalendar2 = DateUtils.getEndDateCalendar(endDate2, Locale.FRANCE)
        val quantity2 = DateUtils.getQuantityInWeeks(startDateCalendar2, endDateCalendar2)
        assertEquals(6, quantity2)

        val startDate3 = DateUtils.getDateFromString("2018-10-22")
        val endDate3 = DateUtils.getDateFromString("2018-10-22")
        val startDateCalendar3 = DateUtils.getStartDateCalendar(startDate3, Locale.FRANCE)
        val endDateCalendar3 = DateUtils.getEndDateCalendar(endDate3, Locale.FRANCE)
        val quantity3 = DateUtils.getQuantityInWeeks(startDateCalendar3, endDateCalendar3)
        assertEquals(1, quantity3)

        val startDate4 = DateUtils.getDateFromString("2019-01-14")
        val endDate4 = DateUtils.getDateFromString("2019-01-20")
        val startDateCalendar4 = DateUtils.getStartDateCalendar(startDate4, Locale.FRANCE)
        val endDateCalendar4 = DateUtils.getEndDateCalendar(endDate4, Locale.FRANCE)
        val quantity4 = DateUtils.getQuantityInWeeks(startDateCalendar4, endDateCalendar4)
        assertEquals(1, quantity4)
    }

    @Test
    fun testGetQuantityInMonths() {
        val startDate = DateUtils.getDateFromString("2018-12-13")
        val endDate = DateUtils.getDateFromString("2019-01-20")
        val startDateCalendar = DateUtils.getStartDateCalendar(startDate)
        val endDateCalendar = DateUtils.getEndDateCalendar(endDate)
        val quantity = DateUtils.getQuantityInMonths(startDateCalendar, endDateCalendar)
        assertEquals(2, quantity)

        val startDate2 = DateUtils.getDateFromString("2018-12-01")
        val endDate2 = DateUtils.getDateFromString("2018-12-31")
        val startDateCalendar2 = DateUtils.getStartDateCalendar(startDate2)
        val endDateCalendar2 = DateUtils.getEndDateCalendar(endDate2)
        val quantity2 = DateUtils.getQuantityInMonths(startDateCalendar2, endDateCalendar2)
        assertEquals(1, quantity2)

        val startDate3 = DateUtils.getDateFromString("2017-10-22")
        val endDate3 = DateUtils.getDateFromString("2018-10-22")
        val startDateCalendar3 = DateUtils.getStartDateCalendar(startDate3)
        val endDateCalendar3 = DateUtils.getEndDateCalendar(endDate3)
        val quantity3 = DateUtils.getQuantityInMonths(startDateCalendar3, endDateCalendar3)
        assertEquals(13, quantity3)
    }

    @Test
    fun testGetQuantityInYears() {
        val startDate = DateUtils.getDateFromString("2018-12-13")
        val endDate = DateUtils.getDateFromString("2019-01-20")
        val startDateCalendar = DateUtils.getStartDateCalendar(startDate)
        val endDateCalendar = DateUtils.getEndDateCalendar(endDate)
        val quantity = DateUtils.getQuantityInYears(startDateCalendar, endDateCalendar)
        assertEquals(2, quantity)

        val startDate2 = DateUtils.getDateFromString("2018-12-01")
        val endDate2 = DateUtils.getDateFromString("2018-12-31")
        val startDateCalendar2 = DateUtils.getStartDateCalendar(startDate2)
        val endDateCalendar2 = DateUtils.getEndDateCalendar(endDate2)
        val quantity2 = DateUtils.getQuantityInYears(startDateCalendar2, endDateCalendar2)
        assertEquals(1, quantity2)

        val startDate3 = DateUtils.getDateFromString("2016-10-22")
        val endDate3 = DateUtils.getDateFromString("2018-10-22")
        val startDateCalendar3 = DateUtils.getStartDateCalendar(startDate3)
        val endDateCalendar3 = DateUtils.getEndDateCalendar(endDate3)
        val quantity3 = DateUtils.getQuantityInYears(startDateCalendar3, endDateCalendar3)
        assertEquals(3, quantity3)
    }

    @Test
    fun testGetFormattedDateString() {
        /*
         * Testing for all dates from 2011-2019
         * */
        val startDateString = "2011-01-01"
        val endDateString = "2019-02-17"
        val formatter = SimpleDateFormat(DATE_FORMAT_DAY)
        val startDate = formatter.parse(startDateString)
        val endDate = formatter.parse(endDateString)

        val start = Calendar.getInstance()
        start.time = startDate
        val end = Calendar.getInstance()
        end.time = endDate

        var date = start.time
        while (start.before(end)) {
            val testDateString = DateUtils.formatDate(DATE_FORMAT_DAY, date)
            val testCalendar = DateUtils.getCalendarInstance(testDateString)
            assertEquals(start.get(Calendar.YEAR), testCalendar.get(Calendar.YEAR))
            assertEquals(start.get(Calendar.MONTH), testCalendar.get(Calendar.MONTH))
            assertEquals(start.get(Calendar.DATE), testCalendar.get(Calendar.DATE))
            assertEquals(
                    testDateString, DateUtils.getFormattedDateString(
                    testCalendar.get(Calendar.YEAR),
                    testCalendar.get(Calendar.MONTH), testCalendar.get(Calendar.DATE)
            )
            )
            start.add(Calendar.DATE, 1)
            date = start.time
        }
    }

    @Test
    fun testGetStartDateForSiteFromString() {
        val site = SiteModel().apply { id = 1 }

        // test get start date for current day
        for (i in -15..15) {
            site.timezone = i.toString()
            // format the current date to string
            // get the formatted date string for the site in the format yyyy-MM-ddThh:mm:ss
            // get the expected start date string for the site in the format yyyy-MM-ddThh:mm:ss
            val dateString1 = DateUtils.formatDate(DATE_FORMAT_DAY, Date())
            val expectedDate1 = "${SiteUtils.getDateTimeForSite(site, "yyyy-MM-dd", Date())}T00:00:00"
            val actualDate1 = DateUtils.getStartDateForSite(site, dateString1)
            assertEquals(expectedDate1, actualDate1)
        }
    }

    @Test
    fun testGetEndDateForSiteFromString() {
        val site = SiteModel().apply { id = 1 }
        for (i in -15..15) {
            site.timezone = i.toString()
            // format the current date to string
            // get the formatted date string for the site in the format yyyy-MM-ddThh:mm:ss
            // get the expected start date string for the site in the format yyyy-MM-ddThh:mm:ss
            val expectedDate1 = "${SiteUtils.getCurrentDateTimeForSite(site, "yyyy-MM-dd")}T23:59:59"
            val actualDate1 = DateUtils.getEndDateForSite(site)
            assertEquals(expectedDate1, actualDate1)
        }
    }

    @Test
    fun testGetStartDayOfCurrentMonthForSite() {
        val site = SiteModel().apply { id = 1 }

        // test get start date for current day
        for (i in -15..15) {
            site.timezone = i.toString()
            // format the current date to string
            // get the formatted date string for the site in the format yyyy-MM-ddThh:mm:ss
            // get the expected start date string for the site in the format yyyy-MM-ddThh:mm:ss
            val expectedDate = LocalDate.now(SiteUtils.getNormalizedTimezone(site.timezone).toZoneId())
                    .withDayOfMonth(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
            val expectedDateString = DateUtils.formatDate(DATE_TIME_FORMAT_START, Date.from(expectedDate))

            val dateString1 = DateUtils.getFirstDayOfCurrentMonthBySite(site)
            assertEquals(expectedDateString, dateString1)
        }
    }

    @Test
    fun testGetStartDayOfCurrentYearForSite() {
        val site = SiteModel().apply { id = 1 }
        val expectedDate = LocalDate.now()
                .withDayOfYear(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        val expectedDateString = DateUtils.formatDate(DATE_TIME_FORMAT_START, Date.from(expectedDate))

        // test get start date for current day
        for (i in -20..20) {
            site.timezone = i.toString()
            // format the current date to string
            // get the formatted date string for the site in the format yyyy-MM-ddThh:mm:ss
            // get the expected start date string for the site in the format yyyy-MM-ddThh:mm:ss
            val dateString1 = DateUtils.getFirstDayOfCurrentYearBySite(site)
            assertEquals(expectedDateString, dateString1)
        }
    }

    @Test
    fun testGetConstantForLastDayOfWeekUSLocale() {
        val constant = DateUtils.getConstantForLastDayOfWeek(Calendar.getInstance(Locale.US))
        assertEquals(Calendar.SATURDAY, constant)
    }

    @Test
    fun testGetConstantForLastDayOfWeekFranceLocale() {
        val constant = DateUtils.getConstantForLastDayOfWeek(Calendar.getInstance(Locale.FRANCE))
        assertEquals(Calendar.SUNDAY, constant)
    }

    @Test
    fun testGetFirstDayOfWeekForWednesdayUSLocale() {
        val cal = Calendar.getInstance(Locale.US)
        // 2021-03-08 00:00:00 Monday
        cal.time = Date(1615183200000)

        val result = DateUtils.getFirstDayOfCurrentWeek(cal)

        // Monday
        assertEquals("2021-03-07", result)
    }

    @Test
    fun testGetFirstDayOfWeekForSundayUSLocale() {
        val cal = Calendar.getInstance(Locale.US)
        // 2021-03-07 00:00:00 Sunday
        cal.time = Date(1615096800000)

        val result = DateUtils.getFirstDayOfCurrentWeek(cal)

        // Monday
        assertEquals("2021-03-07", result)
    }

    @Test
    fun testGetFirstDayOfWeekForSaturdayUSLocale() {
        val cal = Calendar.getInstance(Locale.US)
        // 2021-03-13 00:00:00 Saturday
        cal.time = Date(1615615200000)

        val result = DateUtils.getFirstDayOfCurrentWeek(cal)

        // Monday
        assertEquals("2021-03-07", result)
    }

    @Test
    fun testGetFirstDayOfWeekAcrossMonthsUSLocale() {
        val cal = Calendar.getInstance(Locale.US)
        // 2021-04-01 00:00:00 Thursday April
        cal.time = Date(1617256800000)

        val result = DateUtils.getFirstDayOfCurrentWeek(cal)

        // Monday March
        assertEquals("2021-03-28", result)
    }

    @Test
    fun testGetFirstDayOfWeekForWednesdayFranceLocale() {
        val cal = Calendar.getInstance(Locale.FRANCE)
        // 2021-03-08 00:00:00 Monday
        cal.time = Date(1615183200000)

        val result = DateUtils.getFirstDayOfCurrentWeek(cal)

        // Monday
        assertEquals("2021-03-08", result)
    }

    @Test
    fun testGetFirstDayOfWeekForSundayFranceLocale() {
        val cal = Calendar.getInstance(Locale.FRANCE)
        // 2021-03-07 00:00:00 Sunday
        cal.time = Date(1615096800000)

        val result = DateUtils.getFirstDayOfCurrentWeek(cal)

        // Monday
        assertEquals("2021-03-01", result)
    }

    @Test
    fun testGetFirstDayOfWeekForSaturdayFranceLocale() {
        val cal = Calendar.getInstance(Locale.FRANCE)
        // 2021-03-13 00:00:00 Saturday
        cal.time = Date(1615615200000)

        val result = DateUtils.getFirstDayOfCurrentWeek(cal)

        // Monday
        assertEquals("2021-03-08", result)
    }

    @Test
    fun testGetFirstDayOfWeekAcrossMonthsFranceLocale() {
        val cal = Calendar.getInstance(Locale.FRANCE)
        // 2021-04-01 00:00:00 Thursday April
        cal.time = Date(1617256800000)

        val result = DateUtils.getFirstDayOfCurrentWeek(cal)

        // Monday March
        assertEquals("2021-03-29", result)
    }
}
