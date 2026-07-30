package com.woocommerce.android.extensions

import android.text.format.DateFormat
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateExtTest {
    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun setUp() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun `given a non-UTC device timezone, when formatting a date as GMT, then the UTC wall clock is used`() {
        val date = Date(Instant.parse("2026-07-01T12:30:00Z").toEpochMilli())

        val formatted = date.formatToYYYYmmDDhhmmssGmt()

        assertThat(formatted).isEqualTo("2026-07-01T12:30:00")
    }

    @Test
    fun `given a non-UTC device timezone, when a GMT date string is parsed and formatted back, then it round trips unchanged`() {
        val original = "2026-12-31T23:59:59"

        val roundTrip = original.parseGmtDateFromIso8601DateFormat()?.formatToYYYYmmDDhhmmssGmt()

        assertThat(roundTrip).isEqualTo(original)
    }

    @Test
    fun `given a month-first locale, when formatting to localized month day, then the month comes first`() {
        val date = Date(Instant.parse("2026-08-13T00:00:00Z").toEpochMilli())

        Mockito.mockStatic(DateFormat::class.java).use {
            whenever(DateFormat.getBestDateTimePattern(any(), eq("MMMd"))).thenReturn("MMM d")

            assertThat(date.formatToLocalizedMonthDay(Locale.US)).isEqualTo("Aug 13")
        }
    }

    @Test
    fun `given a day-first locale, when formatting to localized month day, then the day comes first`() {
        val date = Date(Instant.parse("2026-08-13T00:00:00Z").toEpochMilli())

        Mockito.mockStatic(DateFormat::class.java).use {
            whenever(DateFormat.getBestDateTimePattern(any(), eq("MMMd"))).thenReturn("d MMM")

            assertThat(date.formatToLocalizedMonthDay(Locale.UK)).isEqualTo("13 Aug")
        }
    }

    @Test
    fun `given a day-first locale, when formatting to localized medium, then the day comes first`() {
        val date = Date(Instant.parse("2026-08-13T00:00:00Z").toEpochMilli())

        assertThat(date.formatToLocalizedMedium(Locale.UK)).isEqualTo("13 Aug 2026")
    }

    @Test
    fun `given a month-first locale, when formatting to localized medium with time, then the month comes first`() {
        val date = Date(Instant.parse("2026-08-13T00:30:00Z").toEpochMilli())

        val formatted = date.formatToLocalizedMediumWithTime(Locale.US)

        assertThat(formatted.replace('\u202F', ' ')).isEqualTo("Aug 13, 2026, 9:30 AM")
    }

    @Test
    fun `given a day-first locale, when formatting to localized medium with time, then the day comes first`() {
        val date = Date(Instant.parse("2026-08-13T00:30:00Z").toEpochMilli())

        assertThat(date.formatToLocalizedMediumWithTime(Locale.UK)).isEqualTo("13 Aug 2026, 09:30")
    }
}
