package com.woocommerce.android.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Date
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
}
