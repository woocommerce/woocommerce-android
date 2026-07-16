package com.woocommerce.android.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class StringDateFormatExtTest {
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
    fun `given a non-UTC device timezone, when parsing a GMT date, then the UTC instant is returned`() {
        val date = "2026-07-01T12:30:00".parseGmtDateFromIso8601DateFormat()

        assertThat(date?.time).isEqualTo(Instant.parse("2026-07-01T12:30:00Z").toEpochMilli())
    }

    @Test
    fun `given different device timezones, when parsing a GMT date, then the same instant is returned`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        val parsedInTokyo = "2026-07-01T12:30:00".parseGmtDateFromIso8601DateFormat()

        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        val parsedInLosAngeles = "2026-07-01T12:30:00".parseGmtDateFromIso8601DateFormat()

        assertThat(parsedInTokyo).isEqualTo(parsedInLosAngeles)
    }

    @Test
    fun `given a null or empty string, when parsing a GMT date, then null is returned`() {
        assertThat((null as String?).parseGmtDateFromIso8601DateFormat()).isNull()
        assertThat("".parseGmtDateFromIso8601DateFormat()).isNull()
    }
}
