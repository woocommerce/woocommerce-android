package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Factory for canonical booking date range presets used across app and data layers.
 *
 * Notes about time zone:
 * The WC Bookings API stores dates without timezone information. To ensure
 * consistent comparisons and filtering on both client and server, we construct
 * ranges in UTC.
 * See p1759398245019489-slack-C09FHQNQERG
 */
object BookingsDateRangePresets {
    /**
     * Returns a DateRange that spans the current day in UTC, inclusive of the
     * full day when interpreted by the API (midnight to end-of-day).
     */
    fun today(clock: Clock): BookingsFilterOption.DateRange {
        val start = LocalDate.now(clock)
            .atTime(LocalTime.MIDNIGHT)
            .atOffset(ZoneOffset.UTC)
            .toInstant()
        val end = LocalDate.now(clock)
            .atTime(LocalTime.MAX)
            .atOffset(ZoneOffset.UTC)
            .toInstant()
        return BookingsFilterOption.DateRange(before = end, after = start)
    }

    /**
     * Returns a DateRange that includes bookings strictly after the end of the
     * current day in UTC (i.e., upcoming bookings from tomorrow onwards).
     *
     * before = null and after = end-of-today.
     */
    fun upcoming(clock: Clock): BookingsFilterOption.DateRange {
        val endOfToday = LocalDate.now(clock)
            .atTime(LocalTime.MAX)
            .atOffset(ZoneOffset.UTC)
            .toInstant()
        return BookingsFilterOption.DateRange(before = null, after = endOfToday)
    }
}
