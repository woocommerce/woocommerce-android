package com.woocommerce.android.ui.woopos.util.format

import android.content.Context
import com.woocommerce.android.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class WooPosDateFormatterTest {
    private val context: Context = mock()
    private val fixedInstant = Instant.parse("2024-01-15T15:30:00Z")
    private val fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
    private val is24HourFormat: Is24HourFormat = mock()

    private var formatter: WooPosDateFormatter =
        WooPosDateFormatter(context, fixedClock, is24HourFormat)
    private lateinit var originalLocale: Locale

    @Before
    fun setup() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        whenever(is24HourFormat()).thenReturn(false)
        whenever(context.getString(R.string.woopos_date_never)).thenReturn("Never")
        whenever(context.getString(R.string.woopos_date_just_now)).thenReturn("Just now")
        whenever(context.getString(eq(R.string.woopos_date_today_at), any())).thenAnswer { invocation ->
            "Today at ${invocation.arguments[1]}"
        }
        whenever(context.getString(eq(R.string.woopos_date_yesterday_at), any())).thenAnswer { invocation ->
            "Yesterday at ${invocation.arguments[1]}"
        }
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `when both timestamps are null, then formatCatalogLastUpdate returns Never`() {
        // GIVEN
        val productsTimestamp: Long? = null
        val variationsTimestamp: Long? = null

        // WHEN
        val result = formatter.formatCatalogLastUpdate(productsTimestamp, variationsTimestamp)

        // THEN
        assertThat(result).isEqualTo("Never")
    }

    @Test
    fun `when variations timestamp is null, then formatCatalogLastUpdate returns formatted products timestamp`() {
        // GIVEN
        val productsTimestamp = fixedInstant.minusSeconds(30).toEpochMilli()
        val variationsTimestamp: Long? = null

        // WHEN
        val result = formatter.formatCatalogLastUpdate(productsTimestamp, variationsTimestamp)

        // THEN
        assertThat(result).isEqualTo("Just now")
    }

    @Test
    fun `when products timestamp is null, then formatCatalogLastUpdate returns formatted variations timestamp`() {
        // GIVEN
        val productsTimestamp: Long? = null
        val variationsTimestamp = fixedInstant.minusSeconds(30).toEpochMilli()

        // WHEN
        val result = formatter.formatCatalogLastUpdate(productsTimestamp, variationsTimestamp)

        // THEN
        assertThat(result).isEqualTo("Just now")
    }

    @Test
    fun `when both timestamps are present, then formatCatalogLastUpdate returns older timestamp`() {
        // GIVEN
        val productsTimestamp = fixedInstant.minus(Duration.ofMinutes(2)).toEpochMilli()
        val variationsTimestamp = fixedInstant.minusSeconds(30).toEpochMilli()

        // WHEN
        val result = formatter.formatCatalogLastUpdate(productsTimestamp, variationsTimestamp)

        // THEN
        assertThat(result).isEqualTo("Today at 3:28 PM")
    }

    @Test
    fun `when timestamp is very recent, then formatLastUpdateTimestamp returns Just now`() {
        // GIVEN
        val timestamp = fixedInstant.minusSeconds(30).toEpochMilli()

        // WHEN
        val result = formatter.formatCatalogLastUpdate(timestamp, null)

        // THEN
        assertThat(result).isEqualTo("Just now")
    }

    @Test
    fun `when timestamp is from today, then formatLastUpdateTimestamp returns Today at time`() {
        // GIVEN
        val now = ZonedDateTime.ofInstant(fixedInstant, fixedClock.zone)
        val earlierToday = now.minusHours(3)
        val timestamp = earlierToday.toInstant().toEpochMilli()

        // WHEN
        val result = formatter.formatCatalogLastUpdate(timestamp, null)

        // THEN
        assertThat(result).isEqualTo("Today at 12:30 PM")
    }

    @Test
    fun `when timestamp is from yesterday, then formatLastUpdateTimestamp returns Yesterday at time`() {
        // GIVEN
        val now = ZonedDateTime.ofInstant(fixedInstant, fixedClock.zone)
        val yesterday = now.minusDays(1)
        val timestamp = yesterday.toInstant().toEpochMilli()

        // WHEN
        val result = formatter.formatCatalogLastUpdate(timestamp, null)

        // THEN
        assertThat(result).isEqualTo("Yesterday at 3:30 PM")
    }

    @Test
    fun `when timestamp is from this year, then formatLastUpdateTimestamp returns formatted date`() {
        // GIVEN
        val now = ZonedDateTime.ofInstant(fixedInstant, fixedClock.zone)
        val lastWeek = now.minusWeeks(1)
        val timestamp = lastWeek.toInstant().toEpochMilli()

        // WHEN
        val result = formatter.formatCatalogLastUpdate(timestamp, null)

        // THEN
        assertThat(result).isEqualTo("Jan 8 at 3:30 PM")
    }

    @Test
    fun `when timestamp is from previous year, then formatLastUpdateTimestamp returns formatted date with year`() {
        // GIVEN
        val now = ZonedDateTime.ofInstant(fixedInstant, fixedClock.zone)
        val lastYear = now.minusYears(1)
        val timestamp = lastYear.toInstant().toEpochMilli()

        // WHEN
        val result = formatter.formatCatalogLastUpdate(timestamp, null)

        // THEN
        assertThat(result).isEqualTo("Jan 15, 2023 at 3:30 PM")
    }

    @Test
    fun `when two timestamps are provided, then formatCatalogLastUpdate selects minimum timestamp`() {
        // GIVEN
        val olderTimestamp = 1000L
        val newerTimestamp = 2000L

        // WHEN
        val result1 = formatter.formatCatalogLastUpdate(olderTimestamp, newerTimestamp)
        val result2 = formatter.formatCatalogLastUpdate(newerTimestamp, olderTimestamp)

        // THEN
        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `when timestamp is exactly 1 minute ago, then formatLastUpdateTimestamp handles edge case correctly`() {
        // GIVEN
        val timestamp = fixedInstant.minusSeconds(60).toEpochMilli()

        // WHEN
        val result = formatter.formatCatalogLastUpdate(timestamp, null)

        // THEN
        assertThat(result).isEqualTo("Today at 3:29 PM")
    }

    @Test
    fun `when timestamp crosses midnight boundary, then formatLastUpdateTimestamp handles correctly`() {
        // GIVEN
        val now = ZonedDateTime.ofInstant(fixedInstant, fixedClock.zone)
        val todayMidnight = now.toLocalDate().atStartOfDay(fixedClock.zone)
        val justBeforeMidnight = todayMidnight.minusMinutes(1)
        val timestamp = justBeforeMidnight.toInstant().toEpochMilli()

        // WHEN
        val result = formatter.formatCatalogLastUpdate(timestamp, null)

        // THEN
        assertThat(result).isEqualTo("Yesterday at 11:59 PM")
    }

    @Test
    fun `when 24-hour format is enabled, then formatLastUpdateTimestamp uses 24-hour time`() {
        // GIVEN
        whenever(is24HourFormat()).thenReturn(true)
        val now = ZonedDateTime.ofInstant(fixedInstant, fixedClock.zone)
        val earlierToday = now.minusHours(3)
        val timestamp = earlierToday.toInstant().toEpochMilli()

        // WHEN
        val result = formatter.formatCatalogLastUpdate(timestamp, null)

        // THEN
        assertThat(result).isEqualTo("Today at 12:30")
    }

    @Test
    fun `when formatShortDate called, then returns short locale date`() {
        val instant = Instant.parse("2025-03-15T10:00:00Z")

        val result = formatter.formatShortDate(instant)

        assertThat(result).isEqualTo("3/15/25")
    }

    @Test
    fun `when formatShortDate called near midnight UTC, then date uses UTC`() {
        // GIVEN
        val instant = Instant.parse("2025-03-16T03:00:00Z")

        // WHEN
        val result = formatter.formatShortDate(instant)

        // THEN
        assertThat(result).isEqualTo("3/16/25")
    }

    @Test
    fun `when time format changes during runtime, then formatter uses new format`() {
        // GIVEN
        val now = ZonedDateTime.ofInstant(fixedInstant, fixedClock.zone)
        val earlierToday = now.minusHours(3)
        val timestamp = earlierToday.toInstant().toEpochMilli()

        // WHEN - First with 12-hour format
        whenever(is24HourFormat()).thenReturn(false)
        val result12Hour = formatter.formatCatalogLastUpdate(timestamp, null)

        // WHEN - Then with 24-hour format
        whenever(is24HourFormat()).thenReturn(true)
        val result24Hour = formatter.formatCatalogLastUpdate(timestamp, null)

        // THEN
        assertThat(result12Hour).isEqualTo("Today at 12:30 PM")
        assertThat(result24Hour).isEqualTo("Today at 12:30")
    }
}
