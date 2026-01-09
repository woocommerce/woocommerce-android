package com.woocommerce.android.util

import android.content.Context
import android.text.format.DateFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

class DateFormatter @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Formats date and time respecting system 12h/24h preference.
     * Example: "Dec 12, 2025, 10:47 PM" or "Dec 12, 2025, 22:47"
     */
    fun formatDateTime(
        instant: Instant
    ): String = createSkeletonFormatter(skeleton = "MMMdyyyyhm", skeleton24h = "MMMdyyyyHm").format(instant)

    /**
     * Formats time only respecting system 12h/24h preference.
     * Example: "10:47 PM" or "22:47"
     */
    fun formatTime(instant: Instant): String = getTimeFormatter().format(instant)

    /**
     * Formats date only using localized medium style.
     * Example: "Dec 12, 2025"
     */
    fun formatDate(
        localDateTime: LocalDateTime
    ): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(localDateTime)

    /**
     * Formats time only respecting system 12h/24h preference.
     * Example: "10:47 PM" or "22:47"
     */
    fun formatTime(localDateTime: LocalDateTime): String = getTimeFormatter().format(localDateTime)

    private fun createSkeletonFormatter(skeleton: String, skeleton24h: String? = null): DateTimeFormatter {
        val isSystem24h = DateFormat.is24HourFormat(context)

        val finalSkeleton = if (isSystem24h && skeleton24h != null) skeleton24h else skeleton

        val bestPattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), finalSkeleton)

        return DateTimeFormatter.ofPattern(bestPattern, Locale.getDefault()).withZone(ZoneOffset.UTC)
    }

    private fun getTimeFormatter() = createSkeletonFormatter(skeleton = "hm", skeleton24h = "Hm")
}
