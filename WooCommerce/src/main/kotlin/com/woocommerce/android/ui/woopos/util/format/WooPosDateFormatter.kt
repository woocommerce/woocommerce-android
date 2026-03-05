package com.woocommerce.android.ui.woopos.util.format

import android.content.Context
import com.woocommerce.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

class WooPosDateFormatter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    private val is24HourFormat: Is24HourFormat,
) {

    private fun getTimeFormatter(): DateTimeFormatter {
        val pattern = if (is24HourFormat()) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    }

    private fun getDateTimeThisYearFormatter(): DateTimeFormatter {
        val timePattern = if (is24HourFormat()) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern("MMM d 'at' $timePattern", Locale.getDefault())
    }

    private fun getDateTimeWithYearFormatter(): DateTimeFormatter {
        val timePattern = if (is24HourFormat()) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern("MMM d, yyyy 'at' $timePattern", Locale.getDefault())
    }

    fun formatShortDate(instant: Instant): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withZone(ZoneOffset.UTC)
            .format(instant)

    /**
     * Formats the older of two timestamps (products and variations) into a user-friendly string.
     * Uses the older timestamp to represent when the catalog was fully synced.
     * Returns "Never" if both timestamps are null.
     */
    fun formatCatalogLastUpdate(productsTimestamp: Long?, variationsTimestamp: Long?): String {
        val lastSyncTimestamp = when {
            productsTimestamp == null && variationsTimestamp == null -> null
            productsTimestamp == null -> variationsTimestamp
            variationsTimestamp == null -> productsTimestamp
            else -> minOf(productsTimestamp, variationsTimestamp)
        }

        return lastSyncTimestamp?.let {
            formatLastUpdateTimestamp(it)
        } ?: context.getString(R.string.woopos_date_never)
    }

    /**
     * Formats the date into a user-friendly string. Returns "Never" if timestamp is null.
     */
    fun formatCatalogLastFullSync(fullSyncTimestamp: Long?): String {
        return fullSyncTimestamp?.let {
            formatLastUpdateTimestamp(it)
        } ?: context.getString(R.string.woopos_date_never)
    }

    /**
     * Formats a timestamp into a user-friendly string representation.
     * Shows:
     * - "Just now" for very recent updates (< 1 minute)
     * - "Today at HH:mm" for today's updates
     * - "Yesterday at HH:mm" for yesterday's updates
     * - "MMM d at HH:mm" for dates within this year
     * - "MMM d, yyyy at HH:mm" for older dates
     */
    private fun formatLastUpdateTimestamp(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val now = clock.instant()
        val duration = Duration.between(instant, now)

        if (duration.toMinutes() < 1) {
            return context.getString(R.string.woopos_date_just_now)
        }

        val dateTime = ZonedDateTime.ofInstant(instant, clock.zone)
        val nowDateTime = ZonedDateTime.ofInstant(now, clock.zone)
        val date = dateTime.toLocalDate()
        val today = nowDateTime.toLocalDate()
        val formattedTime = dateTime.format(getTimeFormatter())

        return when {
            date == today -> {
                context.getString(R.string.woopos_date_today_at, formattedTime)
            }
            date == today.minusDays(1) -> {
                context.getString(R.string.woopos_date_yesterday_at, formattedTime)
            }
            date.year == today.year -> {
                dateTime.format(getDateTimeThisYearFormatter())
            }
            else -> {
                dateTime.format(getDateTimeWithYearFormatter())
            }
        }
    }
}
