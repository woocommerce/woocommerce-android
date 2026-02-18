package com.woocommerce.android.util

import android.content.Context
import android.text.format.DateFormat
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

class DateFormatterTest {

    private val context: Context = mock()

    private lateinit var formatter: DateFormatter

    private lateinit var dateFormatMock: MockedStatic<DateFormat>

    private val fixedInstant = Instant.parse("2025-12-12T23:00:00Z")
    private val fixedLocalDateTime = LocalDateTime.ofInstant(fixedInstant, ZoneOffset.UTC)

    @Before
    fun setup() {
        dateFormatMock = Mockito.mockStatic(DateFormat::class.java)

        formatter = DateFormatter(context)

        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        dateFormatMock.close()
    }

    @Test
    fun `when system is 12h, then formatDateTime uses 12h skeleton`() {
        // GIVEN
        whenever(DateFormat.is24HourFormat(context)).thenReturn(false)

        whenever(DateFormat.getBestDateTimePattern(any(), eq("MMMdyyyyhm")))
            .thenReturn("MMM d, yyyy h:mm a") // 12h Pattern

        // WHEN
        val result = formatter.formatDateTime(fixedInstant)

        // THEN
        assertThat(result).isEqualTo("Dec 12, 2025 11:00 PM")
    }

    @Test
    fun `when system is 24h, then formatDateTime uses 24h skeleton`() {
        // GIVEN
        whenever(DateFormat.is24HourFormat(context)).thenReturn(true)

        whenever(DateFormat.getBestDateTimePattern(any(), eq("MMMdyyyyHm")))
            .thenReturn("MMM d, yyyy HH:mm") // 24h Pattern

        // WHEN
        val result = formatter.formatDateTime(fixedInstant)

        // THEN
        assertThat(result).isEqualTo("Dec 12, 2025 23:00")
    }

    @Test
    fun `when system is 12h, then formatTime uses 12h skeleton`() {
        // GIVEN
        whenever(DateFormat.is24HourFormat(context)).thenReturn(false)

        whenever(DateFormat.getBestDateTimePattern(any(), eq("hm")))
            .thenReturn("h:mm a")

        // WHEN
        val result = formatter.formatTime(fixedInstant)

        // THEN
        assertThat(result).isEqualTo("11:00 PM")
    }

    @Test
    fun `when system is 24h, then formatTime uses 24h skeleton`() {
        // GIVEN
        whenever(DateFormat.is24HourFormat(context)).thenReturn(true)

        whenever(DateFormat.getBestDateTimePattern(any(), eq("Hm")))
            .thenReturn("HH:mm")

        // WHEN
        val result = formatter.formatTime(fixedInstant)

        // THEN
        assertThat(result).isEqualTo("23:00")
    }

    @Test
    fun `when formatDate with Instant, then returns localized medium date`() {
        // GIVEN
        val instant = Instant.parse("2025-12-12T23:00:00Z")

        // WHEN
        val result = formatter.formatDate(instant)

        // THEN
        assertThat(result).isEqualTo("Dec 12, 2025")
    }

    @Test
    fun `when formatTime invokes, then LocalDateTime respects 24h setting`() {
        // GIVEN
        whenever(DateFormat.is24HourFormat(context)).thenReturn(true)
        whenever(DateFormat.getBestDateTimePattern(any(), eq("Hm")))
            .thenReturn("HH:mm")

        // WHEN
        val result = formatter.formatTime(fixedLocalDateTime)

        // THEN
        assertThat(result).isEqualTo("23:00")
    }
}
